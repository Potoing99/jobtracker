package com.capstone.jobtracker.repository;

import com.capstone.jobtracker.model.CoverLetter;
import com.capstone.jobtracker.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoverLetterRepository extends JpaRepository<CoverLetter, Long> {

    // 검색어 없음: owner 기준으로만 조회, 정렬은 Pageable.sort 에서 처리
    Page<CoverLetter> findByOwner(User owner, Pageable pageable);

    // 검색어 있음: 제목 OR 내용에 keyword 포함, 정렬은 Pageable.sort 에서 처리
    Page<CoverLetter>
    findByOwnerAndTitleContainingIgnoreCaseOrOwnerAndContentContainingIgnoreCase(
            User owner1, String keyword1,
            User owner2, String keyword2,
            Pageable pageable
    );

    // 내 자소서 한 건 조회 (소유자 검증)
    Optional<CoverLetter> findByIdAndOwner(Long id, User owner);

    // 로그인한 사용자 기준 자소서 개수
    long countByOwner(User owner);
}
