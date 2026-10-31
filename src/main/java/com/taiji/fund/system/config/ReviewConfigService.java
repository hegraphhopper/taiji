package com.taiji.fund.system.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 评审相关配置读取：管理页可改，非法值回退默认。
 */
@Service
@RequiredArgsConstructor
public class ReviewConfigService {

    public static final String REQUIRED_PASS_COUNT_KEY = "review.requiredPassCount";
    public static final int DEFAULT_REQUIRED_PASS_COUNT = 2;

    private final SystemConfigMapper configMapper;

    public int requiredPassCount() {
        SystemConfig config = configMapper.selectById(REQUIRED_PASS_COUNT_KEY);
        if (config == null || config.getConfigValue() == null) {
            return DEFAULT_REQUIRED_PASS_COUNT;
        }
        try {
            int value = Integer.parseInt(config.getConfigValue().trim());
            return value >= 1 && value <= 10 ? value : DEFAULT_REQUIRED_PASS_COUNT;
        } catch (NumberFormatException exception) {
            return DEFAULT_REQUIRED_PASS_COUNT;
        }
    }

    public SystemConfig require(String key) {
        SystemConfig config = configMapper.selectOne(Wrappers.<SystemConfig>lambdaQuery()
                .eq(SystemConfig::getConfigKey, key));
        if (config == null) {
            throw new com.taiji.fund.common.exception.BizException(
                    com.taiji.fund.common.exception.ErrorCode.NOT_FOUND, "系统参数不存在");
        }
        return config;
    }
}
