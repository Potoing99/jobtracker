package com.capstone.jobtracker.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoverLetterDto {

    private Long id; // 수정 시 사용(신규 작성일 때는 null)

    @NotBlank(message = "제목을 입력하세요.")
    @Size(max = 200, message = "제목은 200자 이내로 입력하세요.")
    private String title; // 제목

    @NotBlank(message = "내용을 입력하세요.")
    private String content; // 본문

    @NotNull(message = "글자수 제한을 선택하세요.")
    private Integer limitChars; // 500/800/1000/1500 중 하나
}