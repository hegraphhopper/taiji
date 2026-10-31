package com.taiji.fund.task;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class TodoSummaryVO {

    private long total;

    private Map<String, Long> counts;

    private List<TodoItemVO> items;
}
