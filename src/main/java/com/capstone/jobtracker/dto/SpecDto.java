package com.capstone.jobtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecDto {

    private Long id;

    @NotBlank(message = "스펙/자격증 이름을 입력해주세요.")
    @Size(max = 100, message = "스펙/자격증 이름은 100자 이내로 입력해주세요.")
    private String title;

    @Size(max = 100, message = "발행처/기관은 100자 이내로 입력해주세요.")
    private String issuer;          // 발행처/기관

    private LocalDate acquiredAt;   // 취득일

    @Size(max = 255, message = "태그는 255자 이내로 입력해주세요.")
    private String tags;            // 태그 문자열
}
