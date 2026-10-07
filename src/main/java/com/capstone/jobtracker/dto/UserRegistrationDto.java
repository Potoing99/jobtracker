package com.capstone.jobtracker.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserRegistrationDto {

    @NotBlank(message = "이메일은 필수 항목입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "이름은 필수 항목입니다.")
    @Size(min = 2, max = 30, message = "이름은 2~30자 사이여야 합니다.")
    private String name;

    @NotBlank(message = "비밀번호는 필수 항목입니다.")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수 항목입니다.")
    private String confirmPassword;
}
