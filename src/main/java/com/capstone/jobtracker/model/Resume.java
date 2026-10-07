// 간단 주석만: Resume 엔티티 보강 버전 (Lombok + 기본값 처리 + 인덱스/제약)
package com.capstone.jobtracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@Entity
@Table(
        name = "resumes",
        indexes = {
                @Index(name = "idx_resumes_owner", columnList = "owner_id"),
                @Index(name = "idx_resumes_uploadedAt", columnList = "uploadedAt")
        },
        uniqueConstraints = {
                // 동일 사용자가 같은 title을 중복 생성하지 못하게 하고 싶다면 주석 해제
                // @UniqueConstraint(name = "uk_resumes_owner_title", columnNames = {"owner_id", "title"})
        }
)
public class Resume {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사람이 보는 제목(버전명) — 예: "국문_백엔드_2025_v3"
    @Column(nullable = false, length = 120)
    private String title;

    // 서버 저장 파일명 (UUID 등). 업로드 후 불변 처리
    @Column(nullable = false, length = 200, unique = true, updatable = false)
    private String storedFilename;

    // 원본 업로드 파일명 (사용자에게 보여줌)
    @Column(nullable = false, length = 200)
    private String originalFilename;

    // MIME 타입 (e.g. application/pdf, application/vnd.openxmlformats-officedocument.wordprocessingml.document)
    @Column(nullable = false, length = 120)
    private String contentType;

    // 바이트 크기
    @Column(nullable = false)
    private long size;

    // 업로드 시각. 최초 저장 시 자동 세팅, 이후 불변
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    // 소유자(필수) — 개인 스코프
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    // 업로드 시각이 비어 있으면 저장 직전에 자동 세팅
    @PrePersist
    void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }
}
