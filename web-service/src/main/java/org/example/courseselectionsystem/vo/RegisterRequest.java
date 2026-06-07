package org.example.courseselectionsystem.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 用户注册请求DTO
 */
@Data
@ApiModel(value = "RegisterRequest", description = "用户注册请求参数")
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    @ApiModelProperty(value = "用户名", required = true, example = "newuser")
    public String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6-50个字符之间")
    @ApiModelProperty(value = "密码", required = true, example = "password123")
    public String password;

    @NotBlank(message = "确认密码不能为空")
    @ApiModelProperty(value = "确认密码", required = true, example = "password123")
    public String confirmPassword;

    @NotBlank(message = "真实姓名不能为空")
    @Size(min = 1, max = 50, message = "真实姓名长度必须在1-50个字符之间")
    @ApiModelProperty(value = "真实姓名", required = true, example = "张三")
    public String realName;

    @NotBlank(message = "工号/学号不能为空")
    @Size(min = 1, max = 20, message = "工号/学号长度必须在1-20个字符之间")
    @ApiModelProperty(value = "工号/学号", required = true, example = "20230001")
    public String userCode;

    @Email(message = "邮箱格式不正确")
    @ApiModelProperty(value = "邮箱", example = "user@example.com")
    public String email;

    @Size(max = 20, message = "手机号长度不能超过20个字符")
    @ApiModelProperty(value = "手机号", example = "13800138000")
    public String phone;

    @ApiModelProperty(value = "用户类型：1-学生，2-教师", required = true, example = "1")
    public Integer userType;

    @ApiModelProperty(value = "所属学院ID", required = true, example = "1")
    public Long departmentId;

    @ApiModelProperty(value = "所属专业ID", required = true, example = "1")
    public Long majorId;

    @Size(max = 50, message = "班级名称长度不能超过50个字符")
    @ApiModelProperty(value = "班级", example = "计科1班")
    public String className;

    public String role;
}
