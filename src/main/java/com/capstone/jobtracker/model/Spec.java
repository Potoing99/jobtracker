package com.capstone.jobtracker.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "specs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Spec {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;           // 자격증/스펙 이름

    @Column(length = 100)
    private String issuer;          // 발행처/기관 (예: 한국산업인력공단)

    private LocalDate acquiredAt;   // 취득일

    @Column(length = 255)
    private String tags;            // 태그 문자열

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;             // 소유자(현재 로그인)
}
