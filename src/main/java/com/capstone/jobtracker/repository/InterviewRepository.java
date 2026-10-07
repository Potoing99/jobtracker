package com.capstone.jobtracker.repository;

import com.capstone.jobtracker.model.Application;
import com.capstone.jobtracker.model.Interview;
import com.capstone.jobtracker.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {

    // 상세 화면 주입용: 해당 지원건 + 내 소유, 일정 오름차순
    List<Interview> findByApplicationAndOwnerOrderByScheduledAtAsc(Application application, User owner);

    // 리스트(페이징): 내 소유 기준, 일정 오름차순
    Page<Interview> findByOwnerOrderByScheduledAtAsc(User owner, Pageable pageable);

    // 검색(회사/포지션/연락처/메모) + 페이징, 내 소유
    @Query("""
           SELECT i
             FROM Interview i
             JOIN i.application a
            WHERE i.owner = :owner
              AND (
                   LOWER(a.company)  LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(a.position) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(i.contact)  LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(i.memo)     LIKE LOWER(CONCAT('%', :q, '%'))
              )
           """)
    Page<Interview> searchByOwnerAndKeyword(@Param("owner") User owner,
                                            @Param("q") String q,
                                            Pageable pageable);

    // 단건(소유자 제한)
    Optional<Interview> findByIdAndOwner(Long id, User owner);

    List<Interview> findTop5ByOwnerAndScheduledAtGreaterThanEqualOrderByScheduledAtAsc(
            User owner,
            java.time.LocalDateTime now
    );
}

