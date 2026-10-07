package com.capstone.jobtracker.repository;

import com.capstone.jobtracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일로 사용자 한 명 찾기 (없으면 빈 Optional)
    Optional<User> findByEmail(String email);

    // 이메일 중복 여부 빠르게 확인
    boolean existsByEmail(String email);

}
