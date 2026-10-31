package com.taiji.fund.project.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectQuery {

    private String keyword;

    private Long groupId;

    private String fundType;

    private Long techDirectionId;

    private List<String> status = new ArrayList<>();

    private BigDecimal budgetMin;

    private BigDecimal budgetMax;

    private BigDecimal duplicateScoreMin;

    private BigDecimal duplicateScoreMax;

    private LocalDate createdFrom;

    private LocalDate createdTo;

    private long page = 1;

    private long size = 10;

    private String sortBy = "updatedAt";

    private String sortOrder = "desc";
}
