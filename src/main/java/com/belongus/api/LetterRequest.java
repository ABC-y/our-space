package com.belongus.api;

import jakarta.validation.constraints.NotBlank;

public record LetterRequest(
        @NotBlank(message = "内容不能为空") String content
) {
}
