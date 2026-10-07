package com.capstone.jobtracker.dto;

import com.capstone.jobtracker.model.ApplicationStatus;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 지원현황 등록/수정용 DTO
 * - 화면 바인딩 + 필드 검증 담당
 * - 엔티티와 1:1은 아니어도 되지만, 현재는 동일 필드 구성
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ApplicationDto {

    @NotBlank(message = "회사명은 필수입니다.")
    @Size(max = 100, message = "회사명은 최대 100자까지 가능합니다.")
    private String company;

    @NotBlank(message = "직무는 필수입니다.")
    @Size(max = 100, message = "직무는 최대 100자까지 가능합니다.")
    private String position;

    // 선택값: 오늘 또는 과거만 허용(미입력 허용)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @PastOrPresent(message = "지원일은 오늘 또는 과거여야 합니다.")
    private LocalDate appliedAt;

    // 필수값: 과거일 수도 있음(Overdue 표시 필요하므로 @Future는 사용하지 않음)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @NotNull(message = "마감일은 필수입니다.")
    private LocalDate dueDate;

    @NotNull(message = "상태를 선택하세요.")
    private ApplicationStatus status;

}
