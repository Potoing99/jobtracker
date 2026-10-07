package com.capstone.jobtracker.repository;

import com.capstone.jobtracker.model.Resume;
import com.capstone.jobtracker.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    // 페이징 목록: 내 소유
    Page<Resume> findByOwner(User owner, Pageable pageable);

    // 검색(제목/원본파일명): 내 소유 + 페이징
    @Query("""
           SELECT r
             FROM Resume r
            WHERE r.owner = :owner
              AND (
                   LOWER(r.title)            LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(r.originalFilename) LIKE LOWER(CONCAT('%', :q, '%'))
              )
           """)
    Page<Resume> searchByOwnerAndKeyword(@Param("owner") User owner,
                                         @Param("q") String q,
                                         Pageable pageable);

    // 단건 (소유자 제한)
    Optional<Resume> findByIdAndOwner(Long id, User owner);

    // 기존 드롭다운(업로드 최신순)
    List<Resume> findByOwnerOrderByUploadedAtDesc(User owner);

    // 로그인한 사용자 기준 이력서 개수
    long countByOwner(User owner);
}
