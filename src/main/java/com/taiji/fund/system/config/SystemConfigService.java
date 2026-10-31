package com.taiji.fund.system.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.common.audit.Audit;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统参数：查询与更新，更新写审计。
 */
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigMapper configMapper;

    @Transactional(readOnly = true)
    public List<SystemConfigVO> list() {
        return configMapper.selectList(Wrappers.<SystemConfig>lambdaQuery()
                        .orderByAsc(SystemConfig::getConfigKey))
                .stream().map(SystemConfigService::toVO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SystemConfigVO get(String key) {
        SystemConfig config = configMapper.selectById(key);
        if (config == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "系统参数不存在");
        }
        return toVO(config);
    }

    @Audit(action = "SYSTEM_CONFIG_UPDATE", resourceType = "SYSTEM_CONFIG")
    @Transactional
    public SystemConfigVO update(String key, SystemConfigUpdateRequest request) {
        SystemConfig config = configMapper.selectById(key);
        if (config == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "系统参数不存在");
        }
        String value = request == null ? null : request.getValue();
        if (value == null || value.trim().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "参数值不能为空");
        }
        validate(key, value.trim());
        config.setConfigValue(value.trim());
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            config.setDescription(request.getDescription().trim());
        }
        config.setUpdatedBy(SecurityUtils.currentUserId());
        config.setUpdatedAt(LocalDateTime.now());
        configMapper.updateById(config);
        return toVO(config);
    }

    private void validate(String key, String value) {
        if (ReviewConfigService.REQUIRED_PASS_COUNT_KEY.equals(key)) {
            try {
                int count = Integer.parseInt(value);
                if (count < 1 || count > 10) {
                    throw new BizException(ErrorCode.BAD_REQUEST, "评审人数必须为 1-10");
                }
            } catch (NumberFormatException exception) {
                throw new BizException(ErrorCode.BAD_REQUEST, "评审人数必须为整数");
            }
            return;
        }
        throw new BizException(ErrorCode.BAD_REQUEST, "不支持的参数：" + key);
    }

    private static SystemConfigVO toVO(SystemConfig config) {
        return SystemConfigVO.builder()
                .key(config.getConfigKey())
                .value(config.getConfigValue())
                .description(config.getDescription())
                .updatedBy(config.getUpdatedBy())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
