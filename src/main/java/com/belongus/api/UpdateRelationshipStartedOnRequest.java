package com.belongus.api;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateRelationshipStartedOnRequest(
        @NotNull(message = "请选择关系开始日期") LocalDate relationshipStartedOn
) {
}
