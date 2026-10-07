package com.capstone.jobtracker.repository;

import com.capstone.jobtracker.model.Application;
import com.capstone.jobtracker.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    /** 전체(내 것) - List (KPI 계산용 기존 유지) */
    List<Application> findByOwner(User owner);

    /** 전체(내 것) - Page (NULL-LAST 정렬 명시) */
    @Query(
            value = """
                select a
                from Application a
                where a.owner = :owner
                order by case when a.dueDate is null then 1 else 0 end,
                         a.dueDate asc
                """,
            countQuery = """
                     select count(a)
                     from Application a
                     where a.owner = :owner
                     """
    )
    Page<Application> pageByOwnerOrderByDueDateAscNullLast(@Param("owner") User owner, Pageable pageable);

    /** 검색(내 것) - List (기존 유지) */
    @Query("""
           select a
           from Application a
           where a.owner = :owner
             and ( lower(a.company)  like lower(concat('%', :q, '%'))
                or lower(a.position) like lower(concat('%', :q, '%')) )
           """)
    List<Application> searchByOwnerAndKeyword(@Param("owner") User owner, @Param("q") String q);

    /** 검색(내 것) - Page (NULL-LAST 정렬 명시) */
    @Query(
            value = """
                select a
                from Application a
                where a.owner = :owner
                  and ( lower(a.company)  like lower(concat('%', :q, '%'))
                     or lower(a.position) like lower(concat('%', :q, '%')) )
                order by case when a.dueDate is null then 1 else 0 end,
                         a.dueDate asc
                """,
            countQuery = """
                     select count(a)
                     from Application a
                     where a.owner = :owner
                       and ( lower(a.company)  like lower(concat('%', :q, '%'))
                          or lower(a.position) like lower(concat('%', :q, '%')) )
                     """
    )
    Page<Application> searchPageByOwnerAndKeywordOrderByDueDateAscNullLast(
            @Param("owner") User owner,
            @Param("q") String q,
            Pageable pageable
    );

    /** 단건(내 것) */
    Optional<Application> findByIdAndOwner(Long id, User owner);

    // 마감일이 오늘(LocalDate today) 보다 이전인 공고 개수 = Overdue
    long countByOwnerAndDueDateLessThan(User owner, LocalDate today);

    // 마감일이 start ~ end 사이인 공고 개수 (D-범위용 공통 메서드)
    long countByOwnerAndDueDateBetween(User owner, LocalDate start, LocalDate end);

    // 홈 화면용: 오늘 이후 마감 예정인 지원 중, 마감일 빠른 순 TOP 5
    List<Application> findTop5ByOwnerAndDueDateGreaterThanEqualOrderByDueDateAsc(
            User owner,
            LocalDate today
    );
}