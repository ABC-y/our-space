package com.belongus.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MemoryRequest(
        @NotBlank(message = "标题不能为空") String title,
        @NotBlank(message = "故事不能为空") String content,
        String imageUrl,
        @NotNull(message = "日期不能为空") LocalDate occurredOn
) {
}
