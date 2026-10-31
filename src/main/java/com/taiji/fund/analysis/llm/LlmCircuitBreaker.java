package com.taiji.fund.analysis.llm;

/**
 * 单次运行内熔断：连续失败达到阈值后停止调用，可由下一次运行创建新实例复位。
 */
public class LlmCircuitBreaker {

    public static final int DEFAULT_THRESHOLD = 3;

    private final int threshold;
    private int consecutiveFailures;

    public LlmCircuitBreaker() {
        this(DEFAULT_THRESHOLD);
    }

    public LlmCircuitBreaker(int threshold) {
        this.threshold = Math.max(1, threshold);
    }

    public synchronized boolean isOpen() {
        return consecutiveFailures >= threshold;
    }

    public synchronized void recordSuccess() {
        consecutiveFailures = 0;
    }

    public synchronized void recordFailure() {
        consecutiveFailures++;
    }

    public synchronized void reset() {
        consecutiveFailures = 0;
    }

    public synchronized int failures() {
        return consecutiveFailures;
    }

    public int threshold() {
        return threshold;
    }
}
