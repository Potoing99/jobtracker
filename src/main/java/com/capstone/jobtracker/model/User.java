package com.capstone.jobtracker.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor   // 모든 필드 받는 생성자 자동 생성
@Builder              // 빌더 패턴 지원
@ToString(exclude = "password") // 비밀번호는 보안 때문에 출력 제외
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    public enum Role {
        STUDENT, ADMIN
    }
}
