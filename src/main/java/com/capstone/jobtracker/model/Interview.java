// src/main/java/com/capstone/jobtracker/model/Interview.java
package com.capstone.jobtracker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Entity
@Table(name = "interviews")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Interview {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 면접 일정(날짜+시간) */
    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") // ★ datetime-local 바인딩/출력용 포맷
    private LocalDateTime scheduledAt;

    /** 유형(예: 1차/코테/실무/임원 등) */
    @NotBlank
    private String type;

    /** 진행 형태(대면/온라인/전화 등) */
    @NotBlank
    private String mode;

    /** 장소(대면) 또는 링크(온라인) */
    private String placeOrLink;

    /** 담당자/연락처 */
    private String contact;

    /** 메모 */
    @Column(length = 2000)
    private String memo;

    /** 어떤 지원건의 면접인지 (필수) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id")
    private Application application;

    /** 소유자(내 데이터만 보이도록) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;
}
