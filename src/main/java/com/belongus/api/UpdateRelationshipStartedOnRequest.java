package com.belongus.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateRelationshipStartedOnRequest(
        @Size(max = 80, message = "空间名称不能超过 80 个字符")
        String name,
        @NotNull(message = "请选择关系开始日期") LocalDate relationshipStartedOn
) {
}
