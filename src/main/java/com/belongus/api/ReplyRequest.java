package com.belongus.api;

import jakarta.validation.constraints.NotBlank;

public record ReplyRequest(
        @NotBlank(message = "回复不能为空") String content
) {
}
