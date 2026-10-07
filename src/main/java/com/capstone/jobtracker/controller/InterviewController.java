package com.capstone.jobtracker.controller;

import com.capstone.jobtracker.model.Application;
import com.capstone.jobtracker.model.Interview;
import com.capstone.jobtracker.model.User;
import com.capstone.jobtracker.service.ApplicationService;
import com.capstone.jobtracker.service.InterviewService;
import com.capstone.jobtracker.service.UserService;
import com.capstone.jobtracker.util.DDay;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/interviews")
public class InterviewController {

    private final InterviewService service;
    private final ApplicationService applicationService;
    private final UserService userService;

    // 로그인 사용자
    private User me(Principal principal) {
        return userService.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("로그인 정보가 유효하지 않습니다."));
    }

    // 목록: KPI + due 필터 + 검색(q) + (신규) 페이징
    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "due", required = false) String due,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "10") int size,
                       Principal principal,
                       Model model) {

        User owner = me(principal);

        // 전체 면접 목록 (임박순 등 정렬은 service.list() 안에서 처리되어 있다고 가정)
        List<Interview> all = service.list(owner);

        // 오늘 기준 KPI 계산
        LocalDate today = LocalDate.now();
        long within3 = all.stream().filter(i -> i.getScheduledAt() != null)
                .map(i -> i.getScheduledAt().toLocalDate())
                .filter(d -> !d.isBefore(today) && !d.isAfter(today.plusDays(3))).count();
        long within7 = all.stream().filter(i -> i.getScheduledAt() != null)
                .map(i -> i.getScheduledAt().toLocalDate())
                .filter(d -> !d.isBefore(today) && !d.isAfter(today.plusDays(7))).count();
        long within14 = all.stream().filter(i -> i.getScheduledAt() != null)
                .map(i -> i.getScheduledAt().toLocalDate())
                .filter(d -> !d.isBefore(today) && !d.isAfter(today.plusDays(14))).count();
        long overdue = all.stream().filter(i -> i.getScheduledAt() != null)
                .map(i -> i.getScheduledAt().toLocalDate())
                .filter(d -> d.isBefore(today)).count();

        model.addAttribute("within3", within3);
        model.addAttribute("within7", within7);
        model.addAttribute("within14", within14);
        model.addAttribute("overdue", overdue);

        // 선택된 필터/검색 값 유지용
        model.addAttribute("due", due);
        model.addAttribute("q", q);

        // 1) due 필터 적용 (overdue | 3 | 7 | 14)
        List<Interview> filtered = new ArrayList<>(all);
        if (due != null && !due.isBlank()) {
            switch (due) {
                case "overdue":
                    filtered = filtered.stream()
                            .filter(i -> i.getScheduledAt() != null &&
                                    i.getScheduledAt().toLocalDate().isBefore(today))
                            .collect(Collectors.toList());
                    break;
                case "3":
                    filtered = filtered.stream()
                            .filter(i -> i.getScheduledAt() != null)
                            .filter(i -> {
                                LocalDate d = i.getScheduledAt().toLocalDate();
                                return !d.isBefore(today) && !d.isAfter(today.plusDays(3));
                            }).collect(Collectors.toList());
                    break;
                case "7":
                    filtered = filtered.stream()
                            .filter(i -> i.getScheduledAt() != null)
                            .filter(i -> {
                                LocalDate d = i.getScheduledAt().toLocalDate();
                                return !d.isBefore(today) && !d.isAfter(today.plusDays(7));
                            }).collect(Collectors.toList());
                    break;
                case "14":
                    filtered = filtered.stream()
                            .filter(i -> i.getScheduledAt() != null)
                            .filter(i -> {
                                LocalDate d = i.getScheduledAt().toLocalDate();
                                return !d.isBefore(today) && !d.isAfter(today.plusDays(14));
                            }).collect(Collectors.toList());
                    break;
                default:
                    // 전체
            }
        }

        // 2) 검색 필터: 회사/직무/연락처/메모에 q 포함 (대소문자 무시)
        if (q != null && !q.isBlank()) {
            String keyword = q.trim().toLowerCase();
            filtered = filtered.stream().filter(i -> {
                String company  = Optional.ofNullable(i.getApplication()).map(Application::getCompany).orElse("");
                String position = Optional.ofNullable(i.getApplication()).map(Application::getPosition).orElse("");
                String contact  = Optional.ofNullable(i.getContact()).orElse("");
                String memo     = Optional.ofNullable(i.getMemo()).orElse("");
                return company.toLowerCase().contains(keyword)
                        || position.toLowerCase().contains(keyword)
                        || contact.toLowerCase().contains(keyword)
                        || memo.toLowerCase().contains(keyword);
            }).collect(Collectors.toList());
        }

        // ===== 여기부터 페이징 처리 =====
        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        int total = filtered.size();
        int fromIndex = page * size;
        if (fromIndex > total) {
            // 범위 넘어가면 첫 페이지로 보정
            page = 0;
            fromIndex = 0;
        }
        int toIndex = Math.min(fromIndex + size, total);

        List<Interview> pageContent = filtered.subList(fromIndex, toIndex);
        Pageable pageable = PageRequest.of(page, size);
        Page<Interview> pageResult = new PageImpl<>(pageContent, pageable, total);

        // D-Day 라벨은 현재 페이지에 보이는 것만 계산해도 됨
        Map<Long, String> ddayMap = new HashMap<>();
        pageContent.forEach(i -> {
            LocalDate date = (i.getScheduledAt() == null ? null : i.getScheduledAt().toLocalDate());
            ddayMap.put(i.getId(), DDay.label(date));
        });

        model.addAttribute("dday", ddayMap);
        model.addAttribute("list", pageContent);  // 테이블에서 th:each="i : ${list}"
        model.addAttribute("page", pageResult);   // 배너/페이저에서 사용
        model.addAttribute("size", size);         // 페이저 링크에서 사용

        return "interviews/list";
    }

    // 등록 폼
    @GetMapping("/new")
    public String createForm(@RequestParam(value = "applicationId", required = false) Long applicationId,
                             Principal principal, Model model) {
        User owner = me(principal);
        Interview form = new Interview();
        form.setScheduledAt(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));

        model.addAttribute("applications", applicationService.search(owner, null));
        model.addAttribute("selectedApplicationId", applicationId);
        model.addAttribute("form", form);
        model.addAttribute("isEdit", false);
        return "interviews/form";
    }

    // 등록 처리
    @PostMapping
    public String create(@Valid @ModelAttribute("form") Interview form,
                         BindingResult binding,
                         @RequestParam("applicationId") Long applicationId,
                         Principal principal, Model model) {
        User owner = me(principal);

        if (form.getScheduledAt() == null) binding.rejectValue("scheduledAt", "required", "면접 일시를 입력하세요.");
        if (form.getType() == null || form.getType().isBlank()) binding.rejectValue("type", "required", "면접 유형을 입력하세요.");
        if (form.getMode() == null || form.getMode().isBlank()) binding.rejectValue("mode", "required", "진행 형태를 입력하세요.");

        if (binding.hasErrors()) {
            model.addAttribute("applications", applicationService.search(owner, null));
            model.addAttribute("isEdit", false);
            return "interviews/form";
        }

        Application app = applicationService.getOwned(applicationId, owner);
        Interview saved = service.create(form, owner, app);
        return "redirect:/interviews/" + saved.getId();
    }

    // 상세
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Principal principal, Model model, RedirectAttributes ra) {
        try {
            Interview i = service.getOwned(id, me(principal));
            model.addAttribute("itv", i);
            model.addAttribute("dday", DDay.label(i.getScheduledAt() == null ? null : i.getScheduledAt().toLocalDate()));
            return "interviews/detail";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", "해당 면접이 없거나 접근 권한이 없습니다.");
            return "redirect:/interviews";
        }
    }

    // 수정 폼
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Principal principal, Model model, RedirectAttributes ra) {
        try {
            User owner = me(principal);
            Interview i = service.getOwned(id, owner);
            model.addAttribute("form", i);
            model.addAttribute("isEdit", true);
            model.addAttribute("id", id);
            model.addAttribute("applications", applicationService.search(owner, null));
            model.addAttribute("selectedApplicationId", i.getApplication().getId());
            return "interviews/form";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", "해당 면접이 없거나 접근 권한이 없습니다.");
            return "redirect:/interviews";
        }
    }

    // 수정 처리
    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute("form") Interview form,
                       BindingResult binding,
                       @RequestParam("applicationId") Long applicationId,
                       Principal principal, Model model, RedirectAttributes ra) {
        User owner = me(principal);

        if (form.getScheduledAt() == null) binding.rejectValue("scheduledAt", "required", "면접 일시를 입력하세요.");
        if (form.getType() == null || form.getType().isBlank()) binding.rejectValue("type", "required", "면접 유형을 입력하세요.");
        if (form.getMode() == null || form.getMode().isBlank()) binding.rejectValue("mode", "required", "진행 형태를 입력하세요.");

        if (binding.hasErrors()) {
            model.addAttribute("applications", applicationService.search(owner, null));
            model.addAttribute("isEdit", true);
            model.addAttribute("id", id);
            return "interviews/form";
        }

        try {
            Application app = applicationService.getOwned(applicationId, owner);
            service.update(id, form, owner, app);
            return "redirect:/interviews/" + id;
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", "해당 면접이 없거나 접근 권한이 없습니다.");
            return "redirect:/interviews";
        }
    }

    // 삭제
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            service.delete(id, me(principal));
            ra.addFlashAttribute("msg", "삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", "해당 면접이 없거나 접근 권한이 없습니다.");
        }
        return "redirect:/interviews";
    }
}
