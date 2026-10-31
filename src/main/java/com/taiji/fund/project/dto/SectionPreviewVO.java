package com.taiji.fund.project.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SectionPreviewVO {

    private boolean parsed;

    private String message;

    private List<String> goals = new ArrayList<>();

    private List<String> contents = new ArrayList<>();

    private List<String> keyTechs = new ArrayList<>();

    private List<String> milestones = new ArrayList<>();

    private List<String> indicators = new ArrayList<>();
}
