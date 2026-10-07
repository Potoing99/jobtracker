package com.capstone.jobtracker.service;

import com.capstone.jobtracker.dto.ApplicationDto;
import com.capstone.jobtracker.model.Application;
import com.capstone.jobtracker.model.Resume;
import com.capstone.jobtracker.model.User;
import com.capstone.jobtracker.repository.ApplicationRepository;
import com.capstone.jobtracker.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository; // CRUD
    private final ResumeRepository resumeRepository;           // owner 스코프 검증

    /** 검색/목록 (List 반환) - KPI 계산 등에 사용(기존 유지) */
    @Transactional(readOnly = true)
    public List<Application> search(User owner, String q) {
        if (q == null || q.trim().isEmpty()) {
            List<Application> all = applicationRepository.findByOwner(owner);
            all.sort(Comparator.comparing(Application::getDueDate,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            return all;
        }
        String keyword = q.trim();
        List<Application> filtered = applicationRepository.searchByOwnerAndKeyword(owner, keyword);
        filtered.sort(Comparator.comparing(Application::getDueDate,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return filtered;
    }

    /** 검색/목록 (Page 반환) - 테이블 페이징 전용 */
    @Transactional(readOnly = true)
    public Page<Application> searchPage(User owner, String q, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1)); // ← 정렬 제거

        if (q == null || q.trim().isEmpty()) {
            return applicationRepository.pageByOwnerOrderByDueDateAscNullLast(owner, pageable);
        }
        String keyword = q.trim();
        return applicationRepository.searchPageByOwnerAndKeywordOrderByDueDateAscNullLast(owner, keyword, pageable);
    }

    /** 단건 소유자 스코프로 조회 */
    @Transactional(readOnly = true)
    public Application getOwned(Long id, User owner) {
        return applicationRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new IllegalArgumentException("항목을 찾을 수 없습니다."));
    }

    /** 등록 (선택 이력서 연결) */
    @Transactional
    public Application create(ApplicationDto form, User owner, Long resumeId) {
        Application a = Application.builder()
                .company(form.getCompany())
                .position(form.getPosition())
                .appliedAt(form.getAppliedAt())
                .dueDate(form.getDueDate())
                .status(form.getStatus())
                .owner(owner)
                .build();

        if (resumeId != null) {
            Resume r = resumeRepository.findByIdAndOwner(resumeId, owner)
                    .orElseThrow(() -> new IllegalArgumentException("내 이력서가 아니거나 존재하지 않습니다."));
            a.setResume(r);
        } else {
            a.setResume(null);
        }

        return applicationRepository.save(a);
    }

    /** 수정 (날짜는 입력된 값만 덮어쓰기 = 미입력시 기존 유지) */
    @Transactional
    public void update(Long id, ApplicationDto form, User owner, Long resumeId) {
        Application a = getOwned(id, owner);

        a.setCompany(form.getCompany());
        a.setPosition(form.getPosition());
        a.setStatus(form.getStatus());

        if (form.getAppliedAt() != null) {
            a.setAppliedAt(form.getAppliedAt()); // 입력 있을 때만 변경
        }
        if (form.getDueDate() != null) {
            a.setDueDate(form.getDueDate());     // 입력 있을 때만 변경
        }

        if (resumeId != null) {
            Resume r = resumeRepository.findByIdAndOwner(resumeId, owner)
                    .orElseThrow(() -> new IllegalArgumentException("내 이력서가 아니거나 존재하지 않습니다."));
            a.setResume(r);
        } else {
            a.setResume(null); // "연결 안 함" 선택 시 해제
        }
    }

    /** 삭제 (내 것만) */
    @Transactional
    public void delete(Long id, User owner) {
        Application a = getOwned(id, owner);
        applicationRepository.delete(a);
    }

    /**
     * Overdue(이미 마감 지난 공고) 개수 반환
     */
    public long countOverdueApplications(User owner) {
        LocalDate today = LocalDate.now(); // 오늘 날짜
        return applicationRepository.countByOwnerAndDueDateLessThan(owner, today);
    }

    /**
     * D-Day 범위별(예: 0~3일, 4~7일, 8~14일) 마감 예정 공고 개수 반환
     *
     * @param owner    로그인한 사용자
     * @param fromDays 오늘로부터 몇 일 뒤부터 시작할지 (포함)
     * @param toDays   오늘로부터 몇 일 뒤까지 볼지 (포함)
     */
    public long countApplicationsDueInRange(User owner, int fromDays, int toDays) {
        LocalDate today = LocalDate.now();

        LocalDate start = today.plusDays(fromDays);
        LocalDate end = today.plusDays(toDays);

        return applicationRepository.countByOwnerAndDueDateBetween(owner, start, end);
    }

    /**
     * 홈 화면용: 오늘 기준으로, 앞으로 마감 예정인 지원 중
     * 마감일이 가장 가까운 5개만 가져온다.
     */
    public List<Application> findTop5UpcomingApplications(User owner) {
        LocalDate today = LocalDate.now();
        return applicationRepository
                .findTop5ByOwnerAndDueDateGreaterThanEqualOrderByDueDateAsc(owner, today);
    }
}