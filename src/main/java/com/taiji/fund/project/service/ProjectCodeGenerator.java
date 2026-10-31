package com.taiji.fund.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectCodeGenerator {

    private static final String PREFIX = "TJJH-";
    private static final int MAX_ATTEMPTS = 20;

    private final ProjectMapper projectMapper;

    public String nextCode() {
        return nextCode(Year.now().getValue(), 0);
    }

    private String nextCode(int year, int offset) {
        String prefix = PREFIX + year + "-";
        List<Project> latest = projectMapper.selectList(Wrappers.<Project>lambdaQuery()
                .likeRight(Project::getCode, prefix)
                .orderByDesc(Project::getCode)
                .last("LIMIT 1"));
        int sequence = 1;
        if (!latest.isEmpty()) {
            String code = latest.get(0).getCode();
            int dash = code.lastIndexOf('-');
            if (dash >= 0 && dash + 1 < code.length()) {
                try {
                    sequence = Integer.parseInt(code.substring(dash + 1)) + 1;
                } catch (NumberFormatException ignored) {
                    sequence = 1;
                }
            }
        }
        sequence += offset;
        return prefix + String.format("%04d", sequence);
    }

    public boolean exists(String code) {
        return projectMapper.selectCount(Wrappers.<Project>lambdaQuery().eq(Project::getCode, code)) > 0;
    }

    public String nextAvailableCode(int offset) {
        return nextCode(Year.now().getValue(), offset);
    }

    public static int maxAttempts() {
        return MAX_ATTEMPTS;
    }
}
