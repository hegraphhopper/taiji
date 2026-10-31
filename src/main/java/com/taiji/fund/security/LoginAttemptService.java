package com.taiji.fund.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.system.user.User;
import com.taiji.fund.system.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 登录失败计数与锁定状态持久化。
 *
 * <p>登录失败时上层会抛出 {@link com.taiji.fund.common.exception.BizException}，若与登录逻辑共用同一个
 * 事务会被回滚，导致失败计数与锁定永远不生效。因此这里使用 {@link Propagation#REQUIRES_NEW}
 * 独立提交，保证审计/计数证据不被业务异常回滚。</p>
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    static final int MAX_FAIL_COUNT = 5;
    static final long LOCK_MINUTES = 10L;

    private final UserMapper userMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(User user, LocalDateTime now) {
        int previous = user.getFailCount() == null ? 0 : user.getFailCount();
        LocalDateTime lockedUntil = user.getLockedUntil();
        if (lockedUntil != null && !lockedUntil.isAfter(now)) {
            // 锁定期已过，重新开始计数，避免解锁后一次输错就再次锁定
            previous = 0;
            lockedUntil = null;
        }
        int failCount = previous + 1;
        if (failCount >= MAX_FAIL_COUNT) {
            lockedUntil = now.plusMinutes(LOCK_MINUTES);
        }
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, user.getId())
                .set(User::getFailCount, failCount)
                .set(User::getLockedUntil, lockedUntil)
                .set(User::getUpdatedAt, now));
    }

    /**
     * 登录成功时清零失败计数。随登录事务一起提交或回滚，保证“成功才清零”。
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void clearFailure(Long userId, LocalDateTime now) {
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, userId)
                .set(User::getFailCount, 0)
                .set(User::getLockedUntil, null)
                .set(User::getLastLoginAt, now)
                .set(User::getUpdatedAt, now));
    }
}
