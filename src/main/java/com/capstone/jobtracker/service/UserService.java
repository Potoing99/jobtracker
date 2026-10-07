package com.capstone.jobtracker.service;

import com.capstone.jobtracker.dto.UserRegistrationDto; // 회원가입용 DTO
import com.capstone.jobtracker.model.User;              // 사용자 엔티티
import com.capstone.jobtracker.repository.UserRepository; // JPA 리포지토리
import lombok.RequiredArgsConstructor;                  // 생성자 자동 주입
import org.springframework.security.crypto.password.PasswordEncoder; // 비밀번호 해시
import org.springframework.stereotype.Service;           // 서비스 빈

import java.util.Optional;                              // Optional 반환

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;        // DB 접근
    private final PasswordEncoder passwordEncoder;      // 비밀번호 해시

    // 이메일 중복 여부 확인
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // 회원가입 처리 (DTO → 엔티티 변환 후 저장)
    public void register(UserRegistrationDto dto) {
        User user = User.builder()
                .email(dto.getEmail())
                .name(dto.getName())
                .password(passwordEncoder.encode(dto.getPassword())) // 비밀번호 해시
                .role(User.Role.STUDENT)                             // 기본 권한
                .build();
        userRepository.save(user);
    }

    // ✅ Optional<User>로 반환 (컨트롤러에서 orElseThrow 사용 가능)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // (선택) 필요 시 바로 예외 던지는 버전도 제공해도 됨
    public User getByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일 사용자를 찾을 수 없습니다: " + email));
    }
}
