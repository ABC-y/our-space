package com.belongus.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateSpaceRequest(
        @NotBlank(message = "请给这个空间起个名字") String name,
        @NotNull(message = "请选择关系开始日期") LocalDate relationshipStartedOn
) {
}
