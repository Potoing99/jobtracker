package com.capstone.jobtracker.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cover_letters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoverLetter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                 // PK

    @Column(nullable = false, length = 200)
    private String title;            // 자소서 제목

    @Lob
    @Column(nullable = false)
    private String content;          // 자소서 본문

    @Column(name = "limit_chars", nullable = false)
    private Integer limitChars;      // 글자수 제한 (500/800/1000/1500)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성 시각

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 마지막 수정 시각

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;              // 작성자(로그인 사용자)

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}