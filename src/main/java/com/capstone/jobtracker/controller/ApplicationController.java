package com.capstone.jobtracker.controller;

import com.capstone.jobtracker.dto.ApplicationDto;
import com.capstone.jobtracker.model.Application;
import com.capstone.jobtracker.model.ApplicationStatus;
import com.capstone.jobtracker.model.User;
import com.capstone.jobtracker.service.ApplicationService;
import com.capstone.jobtracker.service.ResumeService;
import com.capstone.jobtracker.service.UserService;
import com.capstone.jobtracker.service.InterviewService; // ★ 유지
import com.capstone.jobtracker.util.DDay; // D-Day 유틸
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page; // ★ 추가
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.security.Principal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationService service;
    private final UserService userService;
    private final ResumeService resumeService;
    private final InterviewService interviewService; // ★ 주입

    /** 현재 로그인 사용자(User) 조회 */
    private User me(Principal principal) {
        return userService.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("로그인 정보가 유효하지 않습니다."));
    }

    /** 목록 + 검색 + 배너/DDay (내 것만) — 페이징 + due 필터 */
    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "due", required = false) String due,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "10") int size,
                       Principal principal,
                       Model model) {

        User owner = me(principal);

        // 1) KPI/DDay 계산용: 검색(q)까지 적용된 전체 목록 (마감일 오름차순, null 마지막)
        var fullList = service.search(owner, q);

        model.addAttribute("q", q);
        model.addAttribute("due", due);

        // KPI 집계 (오늘 기준, 전체 목록 기준)
        LocalDate today = LocalDate.now();
        long within3 = fullList.stream().filter(a -> a.getDueDate() != null)
                .filter(a -> !a.getDueDate().isBefore(today) && !a.getDueDate().isAfter(today.plusDays(3))).count();
        long within7 = fullList.stream().filter(a -> a.getDueDate() != null)
                .filter(a -> !a.getDueDate().isBefore(today) && !a.getDueDate().isAfter(today.plusDays(7))).count();
        long within14 = fullList.stream().filter(a -> a.getDueDate() != null)
                .filter(a -> !a.getDueDate().isBefore(today) && !a.getDueDate().isAfter(today.plusDays(14))).count();
        long overdue = fullList.stream().filter(a -> a.getDueDate() != null && a.getDueDate().isBefore(today)).count();

        model.addAttribute("within3", within3);
        model.addAttribute("within7", within7);
        model.addAttribute("within14", within14);
        model.addAttribute("overdue", overdue);

        // 2) 테이블 표시용: due 필터 적용한 목록 만들기
        var filtered = new java.util.ArrayList<Application>(fullList);

        if (due != null && !due.isBlank()) {
            switch (due) {
                case "overdue":
                    filtered.removeIf(a ->
                            a.getDueDate() == null || !a.getDueDate().isBefore(today));
                    break;
                case "3":
                    filtered.removeIf(a -> {
                        if (a.getDueDate() == null) return true;
                        LocalDate d = a.getDueDate();
                        return d.isBefore(today) || d.isAfter(today.plusDays(3));
                    });
                    break;
                case "7":
                    filtered.removeIf(a -> {
                        if (a.getDueDate() == null) return true;
                        LocalDate d = a.getDueDate();
                        return d.isBefore(today) || d.isAfter(today.plusDays(7));
                    });
                    break;
                case "14":
                    filtered.removeIf(a -> {
                        if (a.getDueDate() == null) return true;
                        LocalDate d = a.getDueDate();
                        return d.isBefore(today) || d.isAfter(today.plusDays(14));
                    });
                    break;
                default:
                    // 필터 없음
            }
        }

        // 3) filtered 기준으로 수동 페이징
        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        int total = filtered.size();
        int fromIndex = page * size;
        if (fromIndex > total) {
            page = 0;
            fromIndex = 0;
        }
        int toIndex = Math.min(fromIndex + size, total);

        var pageContent = filtered.subList(fromIndex, toIndex);
        Pageable pageable = PageRequest.of(page, size);
        Page<Application> pageData = new PageImpl<>(pageContent, pageable, total);

        // 4) 페이지에 표시되는 항목들에 대한 D-Day 맵
        Map<Long, String> ddayMapForPage = new HashMap<>();
        pageContent.forEach(a -> ddayMapForPage.put(a.getId(), DDay.label(a.getDueDate())));

        model.addAttribute("page", pageData);          // 페이저, 상단 "총 N개"
        model.addAttribute("list", pageContent);       // 테이블 th:each="a : ${list}"
        model.addAttribute("dday", ddayMapForPage);    // D-Day 표시
        model.addAttribute("size", size);

        return "applications/list";
    }

    /** 등록 폼 */
    @GetMapping("/new")
    public String createForm(Model model, Principal principal) {
        model.addAttribute("form", new ApplicationDto());
        model.addAttribute("statuses", ApplicationStatus.values());
        model.addAttribute("resumes", resumeService.list(me(principal)));
        model.addAttribute("isEdit", false);
        return "applications/form";
    }

    /** 등록 처리 */
    @PostMapping
    public String create(@Valid @ModelAttribute("form") ApplicationDto form,
                         BindingResult binding,
                         @RequestParam(value = "resumeId", required = false) Long resumeId,
                         Principal principal, Model model) {

        if (form.getAppliedAt() != null && form.getDueDate() != null
                && form.getDueDate().isBefore(form.getAppliedAt())) {
            binding.rejectValue("dueDate", "invalidDate", "마감일은 지원일보다 빠를 수 없습니다.");
        }
        if (binding.hasErrors()) {
            model.addAttribute("statuses", ApplicationStatus.values());
            model.addAttribute("resumes", resumeService.list(me(principal)));
            model.addAttribute("isEdit", false);
            return "applications/form";
        }

        Application saved = service.create(form, me(principal), resumeId);
        return "redirect:/applications/" + saved.getId();
    }

    /** 상세 (내 것만) — 연결된 이력서 + 면접 목록 주입 */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Principal principal, Model model, RedirectAttributes ra) {
        try {
            User owner = me(principal);
            Application app = service.getOwned(id, owner);

            model.addAttribute("app", app);
            model.addAttribute("dday", DDay.label(app.getDueDate())); // 지원건 D-Day
            model.addAttribute("resume", app.getResume());            // 연결된 이력서

            // 이 지원건에 연결된 면접 목록(일정 오름차순)
            var interviews = interviewService.listByApplication(app, owner);
            model.addAttribute("interviews", interviews);

            return "applications/detail";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", "해당 항목이 없거나 접근 권한이 없습니다.");
            return "redirect:/applications";
        }
    }

    /** 수정 폼 */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Principal principal, Model model, RedirectAttributes ra) {
        try {
            var app = service.getOwned(id, me(principal));
            var form = ApplicationDto.builder()
                    .company(app.getCompany())
                    .position(app.getPosition())
                    .appliedAt(app.getAppliedAt())
                    .dueDate(app.getDueDate())
                    .status(app.getStatus())
                    .build();

            model.addAttribute("form", form);
            model.addAttribute("id", id);
            model.addAttribute("statuses", ApplicationStatus.values());
            model.addAttribute("resumes", resumeService.list(me(principal)));
            model.addAttribute("selectedResumeId", (app.getResume() == null ? null : app.getResume().getId()));
            model.addAttribute("isEdit", true);
            return "applications/form";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", "해당 항목이 없거나 접근 권한이 없습니다.");
            return "redirect:/applications";
        }
    }

    /** 수정 처리 */
    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute("form") ApplicationDto form,
                       BindingResult binding,
                       @RequestParam(value = "resumeId", required = false) Long resumeId,
                       Principal principal, Model model, RedirectAttributes ra) {

        if (form.getAppliedAt() != null && form.getDueDate() != null
                && form.getDueDate().isBefore(form.getAppliedAt())) {
            binding.rejectValue("dueDate", "invalidDate", "마감일은 지원일보다 빠를 수 없습니다.");
        }
        if (binding.hasErrors()) {
            model.addAttribute("id", id);
            model.addAttribute("statuses", ApplicationStatus.values());
            model.addAttribute("resumes", resumeService.list(me(principal)));
            model.addAttribute("isEdit", true);
            return "applications/form";
        }

        try {
            service.update(id, form, me(principal), resumeId);
            return "redirect:/applications/" + id;
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", "해당 항목이 없거나 접근 권한이 없습니다.");
            return "redirect:/applications";
        }
    }

    /** 삭제 */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            service.delete(id, me(principal));
            ra.addFlashAttribute("msg", "삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", "해당 항목이 없거나 접근 권한이 없습니다.");
        }
        return "redirect:/applications";
    }
}