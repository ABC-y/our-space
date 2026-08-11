package com.belongus.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "请输入昵称")
        @Size(max = 40, message = "昵称不能超过 40 个字符")
        String displayName,
        @NotBlank(message = "请输入账号")
        @Pattern(regexp = "^[A-Za-z0-9_]{4,24}$", message = "账号需为 4-24 位字母、数字或下划线")
        String username,
        @NotBlank(message = "请输入密码")
        @Size(min = 8, max = 72, message = "密码需为 8-72 位")
        String password
) {
}
