package com.capstone.jobtracker.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import com.capstone.jobtracker.model.Resume; // Resume 엔티티 타입 참조
import jakarta.persistence.FetchType;        // 지연 로딩(필요 시 로딩) 전략 지정
import jakarta.persistence.ManyToOne;        // N:1 연관관계 매핑 애노테이션
import jakarta.persistence.JoinColumn;       // FK 컬럼명을 직접 지정


/** 지원서 엔티티 */
@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "application")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK: AUTO_INCREMENT
    private Long id;

    @Column(nullable = false, length = 100) // 회사명(필수)
    private String company;

    @Column(nullable = false, length = 100) // 포지션(필수)
    private String position;

    private LocalDate appliedAt;            // 지원일(선택)

    @Column(nullable = false)               // 마감일(필수)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)  // 상태(필수): APPLIED/INTERVIEW/OFFER/REJECTED 등
    private ApplicationStatus status;

    /** 제출 이력서(선택) — 없을 수도 있음 */
    @ManyToOne(fetch = FetchType.LAZY)      // N:1, 필요 시 로딩
    @JoinColumn(name = "resume_id")         // FK: application.resume_id → resumes.id
    private Resume resume;

    /** 소유자(로그인 사용자, 필수) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false) // FK: application.user_id → users.id
    private User owner;
}