package org.example.courseselectionsystem.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 用户登录请求DTO
 */
@Data
@ApiModel(value = "LoginRequest", description = "用户登录请求参数")
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    @ApiModelProperty(value = "用户名", required = true, example = "admin")
    public String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6-50个字符之间")
    @ApiModelProperty(value = "密码", required = true, example = "password123")
    public String password;

    @ApiModelProperty(value = "验证码ID（由 /api/v1/auth/captcha 下发，存入 Redis）", example = "1f4f7f3a-....")
    public String captchaId;

    @ApiModelProperty(value = "用户输入的验证码", example = "A3k9")
    public String captchaCode;
}
