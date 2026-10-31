package com.taiji.fund.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.common.audit.Audit;
import com.taiji.fund.common.enums.ProjectStatus;
import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.domain.ProjectReview;
import com.taiji.fund.project.dto.ProjectReviewRequest;
import com.taiji.fund.project.dto.ProjectReviewVO;
import com.taiji.fund.project.mapper.ProjectMapper;
import com.taiji.fund.project.mapper.ProjectReviewMapper;
import com.taiji.fund.security.LoginUser;
import com.taiji.fund.security.SecurityUtils;
import com.taiji.fund.system.user.User;
import com.taiji.fund.system.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 专家评审：REVIEWING 项目可评，每轮每位专家一次，记录不可修改。
 */
@Service
@RequiredArgsConstructor
public class ProjectReviewService {

    private static final Set<String> RESULTS = new HashSet<>(Arrays.asList("PASS", "REJECT", "REVISE"));

    private final ProjectMapper projectMapper;
    private final ProjectReviewMapper reviewMapper;
    private final ProjectAccessService accessService;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public List<ProjectReviewVO> list(Long projectId) {
        Project project = load(projectId);
        accessService.assertCanView(project);
        Map<Long, String> names = reviewerNames(projectId);
        return reviewMapper.selectList(Wrappers.<ProjectReview>lambdaQuery()
                        .eq(ProjectReview::getProjectId, projectId)
                        .orderByDesc(ProjectReview::getRound)
                        .orderByAsc(ProjectReview::getId))
                .stream()
                .map(review -> {
                    ProjectReviewVO vo = ProjectReviewVO.from(review);
                    vo.setReviewerName(names.get(review.getReviewerId()));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Audit(action = "PROJECT_REVIEW_SUBMIT", resourceType = "PROJECT")
    @Transactional
    public ProjectReviewVO submit(Long projectId, ProjectReviewRequest request) {
        Project project = load(projectId);
        assertReviewer();
        accessService.assertCanView(project);
        if (!ProjectStatus.REVIEWING.getCode().equals(project.getStatus())) {
            throw new BizException(ErrorCode.PROJECT_ILLEGAL_TRANSITION, "仅评审中项目可提交评审");
        }
        int round = currentRound(project);
        LoginUser user = accessService.currentUser();
        Long existing = reviewMapper.selectCount(Wrappers.<ProjectReview>lambdaQuery()
                .eq(ProjectReview::getProjectId, projectId)
                .eq(ProjectReview::getReviewerId, user.getId())
                .eq(ProjectReview::getRound, round));
        if (existing != null && existing > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "本轮已提交评审，不可重复提交");
        }
        if (request == null || request.getScore() == null
                || request.getScore().doubleValue() < 0 || request.getScore().doubleValue() > 100) {
            throw new BizException(ErrorCode.BAD_REQUEST, "评分必须在 0-100 之间");
        }
        if (!StringUtils.hasText(request.getOpinion())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "评审意见不能为空");
        }
        String result = request.getResult() == null
                ? "" : request.getResult().trim().toUpperCase();
        if (!RESULTS.contains(result)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "评审结论必须为 PASS/REJECT/REVISE");
        }
        ProjectReview review = new ProjectReview();
        review.setProjectId(projectId);
        review.setReviewerId(user.getId());
        review.setRound(round);
        review.setScore(request.getScore());
        review.setOpinion(request.getOpinion().trim());
        review.setResult(result);
        review.setCreatedAt(LocalDateTime.now());
        reviewMapper.insert(review);
        ProjectReviewVO vo = ProjectReviewVO.from(review);
        User reviewer = userMapper.selectById(user.getId());
        vo.setReviewerName(reviewer == null ? user.getUsername() : reviewer.getRealName());
        return vo;
    }

    @Transactional(readOnly = true)
    public int requiredPassCount(Project project) {
        return passCount(project.getId());
    }

    /**
     * 项目全部轮次中 PASS 的不同专家数：同一专家多轮 PASS 只计 1 人。
     */
    @Transactional(readOnly = true)
    public int passCount(Long projectId) {
        if (projectId == null) {
            return 0;
        }
        List<ProjectReview> reviews = reviewMapper.selectList(Wrappers.<ProjectReview>lambdaQuery()
                .eq(ProjectReview::getProjectId, projectId)
                .eq(ProjectReview::getResult, "PASS"));
        return (int) reviews.stream().map(ProjectReview::getReviewerId).distinct().count();
    }

    public int currentRound(Project project) {
        Integer round = project.getReviewRound();
        return round == null || round < 1 ? 1 : round;
    }

    public void ensureRound(Project project) {
        if (project.getReviewRound() == null || project.getReviewRound() < 1) {
            project.setReviewRound(1);
        }
    }

    private Project load(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException(ErrorCode.PROJECT_NOT_FOUND);
        }
        return project;
    }

    private void assertReviewer() {
        LoginUser user = SecurityUtils.currentUser();
        if (!user.getRoles().contains(RoleCode.EXPERT.name())
                && !user.getRoles().contains(RoleCode.ADMIN.name())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅评审专家可提交评审");
        }
    }

    private Map<Long, String> reviewerNames(Long projectId) {
        List<ProjectReview> reviews = reviewMapper.selectList(Wrappers.<ProjectReview>lambdaQuery()
                .eq(ProjectReview::getProjectId, projectId));
        List<Long> ids = reviews.stream().map(ProjectReview::getReviewerId)
                .filter(id -> id != null).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        return userMapper.selectList(Wrappers.<User>lambdaQuery().in(User::getId, ids))
                .stream().collect(Collectors.toMap(User::getId,
                        user -> user.getRealName() == null ? user.getUsername() : user.getRealName(),
                        (left, right) -> left));
    }
}
