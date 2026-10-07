package com.capstone.jobtracker.controller;

import com.capstone.jobtracker.dto.SpecDto;
import com.capstone.jobtracker.model.User;
import com.capstone.jobtracker.service.SpecService;
import com.capstone.jobtracker.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/specs")
@RequiredArgsConstructor
public class SpecController {

    private final SpecService specService;
    private final UserService userService;

    // 스펙 목록 + 검색 + 정렬
    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String keyword,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       @RequestParam(name = "sort", defaultValue = "latest") String sort,
                       Principal principal,
                       Model model) {

        User owner = getCurrentUser(principal);

        int size = 50; // 자격증 개수 많지 않으니 넉넉하게 고정

        Page<SpecDto> specPage = specService.searchPage(owner, keyword, page, size, sort);

        model.addAttribute("page", specPage);
        model.addAttribute("q", keyword);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);

        return "specs/list";
    }

    // 신규 등록 폼
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("spec")) {
            model.addAttribute("spec", new SpecDto());
        }
        return "specs/form";
    }

    // 수정 폼
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id,
                               Principal principal,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        User owner = getCurrentUser(principal);

        try {
            SpecDto dto = specService.getSpec(owner, id);
            model.addAttribute("spec", dto);
            return "specs/form";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/specs";
        }
    }

    // 등록/수정 처리 (id == null 이면 등록, 아니면 수정)
    @PostMapping
    public String save(@Valid @ModelAttribute("spec") SpecDto specDto,
                       BindingResult bindingResult,
                       Principal principal,
                       RedirectAttributes redirectAttributes) {

        User owner = getCurrentUser(principal);

        // 입력 검증 에러 있으면 다시 폼으로
        if (bindingResult.hasErrors()) {
            return "specs/form";
        }

        try {
            if (specDto.getId() == null) {
                specService.create(owner, specDto);
                redirectAttributes.addFlashAttribute("message", "스펙이 등록되었습니다.");
            } else {
                specService.update(owner, specDto);
                redirectAttributes.addFlashAttribute("message", "스펙이 수정되었습니다.");
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/specs";
    }

    // 삭제
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         Principal principal,
                         RedirectAttributes redirectAttributes) {

        User owner = getCurrentUser(principal);

        try {
            specService.delete(owner, id);
            redirectAttributes.addFlashAttribute("message", "스펙이 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/specs";
    }

    // 현재 로그인한 유저 조회 (owner 패턴 공통 헬퍼)
    private User getCurrentUser(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("로그인 정보가 없습니다.");
        }
        String email = principal.getName(); // SecurityConfig에서 username = email 로 사용 중이라고 가정
        return userService.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("사용자 정보를 찾을 수 없습니다."));
    }
}
