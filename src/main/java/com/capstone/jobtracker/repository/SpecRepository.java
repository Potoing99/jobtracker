package com.capstone.jobtracker.repository;

import com.capstone.jobtracker.model.Spec;
import com.capstone.jobtracker.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// Spec 엔티티용 JPA 리포지토리
public interface SpecRepository extends JpaRepository<Spec, Long> {

    // 로그인한 사용자(owner)의 스펙 목록 페이징 조회
    Page<Spec> findByOwner(User owner, Pageable pageable);

    // 로그인한 사용자(owner)의 스펙 중에서 제목/태그에 keyword가 포함된 것만 검색
    @Query("select s from Spec s " +
            "where s.owner = :owner " +
            "and (" +
            "   lower(s.title) like lower(concat('%', :keyword, '%')) " +
            "   or lower(s.tags) like lower(concat('%', :keyword, '%')) " +
            "   or lower(s.issuer) like lower(concat('%', :keyword, '%'))" +
            ")")
    Page<Spec> searchByOwnerAndKeyword(@Param("owner") User owner,
                                       @Param("keyword") String keyword,
                                       Pageable pageable);

    // 상세보기/수정/삭제 시에도 owner 검증을 위해 사용
    Optional<Spec> findByIdAndOwner(Long id, User owner);

    List<Spec> findByOwnerOrderByAcquiredAtDesc(User owner);

    // 로그인한 사용자 기준 스펙 개수
    long countByOwner(User owner);
}
