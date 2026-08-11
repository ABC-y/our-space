package com.belongus.api;

import jakarta.validation.constraints.NotBlank;

public record JoinSpaceRequest(
        @NotBlank(message = "请输入邀请码") String inviteCode
) {
}
