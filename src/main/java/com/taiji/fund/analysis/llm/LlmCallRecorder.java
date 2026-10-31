package com.taiji.fund.analysis.llm;

/**
 * 调用日志记录器。实现必须自行吞掉写入异常，不能阻断分析运行。
 */
public interface LlmCallRecorder {

    LlmCallRecorder NO_OP = entry -> {
    };

    void record(LlmCallLogEntry entry);
}
