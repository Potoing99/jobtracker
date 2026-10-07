package com.capstone.jobtracker.service;

import com.capstone.jobtracker.model.Application;
import com.capstone.jobtracker.model.Interview;
import com.capstone.jobtracker.model.User;
import com.capstone.jobtracker.repository.InterviewRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class InterviewService {

    private final InterviewRepository interviewRepository;

    public InterviewService(InterviewRepository interviewRepository) {
        this.interviewRepository = interviewRepository;
    }

    // 컨트롤러 list()용: 임박순(날짜 오름차순) 전체 조회
    public List<Interview> list(User owner) {
        return interviewRepository
                .findByOwnerOrderByScheduledAtAsc(owner, Pageable.unpaged())
                .getContent();
    }

    // 상세 화면용
    public List<Interview> listByApplication(Application app, User owner) {
        return interviewRepository.findByApplicationAndOwnerOrderByScheduledAtAsc(app, owner);
    }

    // (참고) 페이징/검색 통합 — 이번 최소변경에서는 사용하지 않음
    public Page<Interview> searchPage(User owner, String q, String sort, Integer page, Integer size) {
        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size <= 0) ? 10 : size;
        Pageable pageable = PageRequest.of(p, s, resolveSort(sort));

        if (q == null || q.trim().isEmpty()) {
            return interviewRepository.findByOwnerOrderByScheduledAtAsc(owner, pageable);
        } else {
            return interviewRepository.searchByOwnerAndKeyword(owner, q.trim(), pageable);
        }
    }

    public Interview getOwned(Long id, User owner) {
        return interviewRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new IllegalArgumentException("데이터를 찾을 수 없거나 권한이 없습니다."));
    }

    public Interview create(Interview interview, User owner, Application app) {
        interview.setOwner(owner);
        interview.setApplication(app);
        return interviewRepository.save(interview);
    }

    public Interview update(Long id, Interview patch, User owner, Application app) {
        Interview cur = getOwned(id, owner);
        if (patch.getScheduledAt() != null) cur.setScheduledAt(patch.getScheduledAt());
        if (patch.getType() != null)        cur.setType(patch.getType());
        if (patch.getMode() != null)        cur.setMode(patch.getMode());
        if (patch.getPlaceOrLink() != null) cur.setPlaceOrLink(patch.getPlaceOrLink());
        if (patch.getContact() != null)     cur.setContact(patch.getContact());
        if (patch.getMemo() != null)        cur.setMemo(patch.getMemo());
        if (app != null)                    cur.setApplication(app);
        return interviewRepository.save(cur);
    }

    public void delete(Long id, User owner) {
        Interview cur = getOwned(id, owner);
        interviewRepository.delete(cur);
    }

    private Sort resolveSort(String sort) {
        Sort.Order def = Sort.Order.asc("scheduledAt").nullsLast();
        if (sort == null || sort.isBlank()) return Sort.by(def);

        String[] parts = sort.split(",", 2);
        String prop = parts[0].trim();
        String dirStr = (parts.length > 1) ? parts[1].trim().toLowerCase() : "asc";
        boolean asc = !"desc".equals(dirStr);

        if (!Objects.equals(prop, "scheduledAt") &&
                !Objects.equals(prop, "type") &&
                !Objects.equals(prop, "mode")) {
            return Sort.by(def);
        }
        return Sort.by((asc ? Sort.Order.asc(prop) : Sort.Order.desc(prop)).nullsLast());
    }

    /**
     * 홈 화면용: 지금 시각 이후에 잡혀있는 면접 중
     * 가장 가까운 일정 5개만 가져온다.
     */
    public List<Interview> findTop5UpcomingInterviews(User owner) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return interviewRepository
                .findTop5ByOwnerAndScheduledAtGreaterThanEqualOrderByScheduledAtAsc(owner, now);
    }
}