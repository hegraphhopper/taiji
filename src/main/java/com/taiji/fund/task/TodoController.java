package com.taiji.fund.task;

import com.taiji.fund.common.api.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 待办中心：所有登录角色可用，数据范围由服务层控制。
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @GetMapping("/todo")
    public Result<TodoSummaryVO> todo() {
        List<TodoItemVO> items = todoService.list();
        return Result.ok(TodoSummaryVO.builder()
                .total(items.size())
                .counts(items.stream().collect(Collectors.groupingBy(
                        TodoItemVO::getAction, Collectors.counting())))
                .items(items)
                .build());
    }
}
