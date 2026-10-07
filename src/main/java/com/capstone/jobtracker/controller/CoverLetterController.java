package com.capstone.jobtracker.controller;

import com.capstone.jobtracker.dto.CoverLetterDto;
import com.capstone.jobtracker.model.CoverLetter;
import com.capstone.jobtracker.model.User;
import com.capstone.jobtracker.service.CoverLetterService;
import com.capstone.jobtracker.service.SpecService;
import com.capstone.jobtracker.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/coverletters")
@RequiredArgsConstructor
public class CoverLetterController {

    private final CoverLetterService coverLetterService; // 자소서 서비스
    private final UserService userService;               // 현재 로그인 유저 조회
    private final SpecService specService;               // 스펙(자격증) 읽기 전용

    // 글자수 제한 프리셋
    private static final Integer[] LIMIT_PRESETS = {500, 800, 1000, 1500};

    // 현재 로그인한 User 엔티티 가져오기
    private User getCurrentUser(Principal principal) {
        String email = principal.getName();
        return userService.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("로그인 정보를 찾을 수 없습니다."));
    }

    // 자소서 목록: 검색 + 정렬 + 페이징
    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String keyword,
                       @RequestParam(value = "sort", defaultValue = "latest") String sort,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "10") int size,
                       Principal principal,
                       Model model) {

        User me = getCurrentUser(principal);
        Page<CoverLetter> coverLetterPage =
                coverLetterService.getPage(me, keyword, sort, page, size);

        model.addAttribute("page", coverLetterPage);
        model.addAttribute("q", keyword == null ? "" : keyword);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);

        return "coverletters/list";
    }

    // 자소서 작성 폼
    @GetMapping("/new")
    public String showCreateForm(Principal principal, Model model) {
        User me = getCurrentUser(principal);

        CoverLetterDto dto = new CoverLetterDto();
        dto.setLimitChars(1000); // 기본 1000자

        model.addAttribute("coverLetter", dto);
        model.addAttribute("limits", LIMIT_PRESETS);
        model.addAttribute("isEdit", false);
        model.addAttribute("specs", specService.getTopSpecsForOwner(me)); // 우측 스펙 패널

        return "coverletters/form";
    }

    // 자소서 신규 등록
    @PostMapping
    public String create(@Valid @ModelAttribute("coverLetter") CoverLetterDto dto,
                         BindingResult bindingResult,
                         Principal principal,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        User me = getCurrentUser(principal);

        // ✅ 서버에서도 "내용 글자수 <= limitChars" 검증
        validateContentLength(dto, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("limits", LIMIT_PRESETS);
            model.addAttribute("isEdit", false);
            model.addAttribute("specs", specService.getTopSpecsForOwner(me));
            return "coverletters/form";
        }

        coverLetterService.create(me, dto);

        redirectAttributes.addFlashAttribute("message", "자소서가 등록되었습니다.");
        return "redirect:/coverletters";
    }

    // 자소서 수정 폼
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id,
                               Principal principal,
                               Model model) {

        User me = getCurrentUser(principal);
        CoverLetter letter = coverLetterService.getOneForOwner(me, id);

        CoverLetterDto dto = CoverLetterDto.builder()
                .id(letter.getId())
                .title(letter.getTitle())
                .content(letter.getContent())
                .limitChars(letter.getLimitChars())
                .build();

        model.addAttribute("coverLetter", dto);
        model.addAttribute("limits", LIMIT_PRESETS);
        model.addAttribute("isEdit", true);
        model.addAttribute("specs", specService.getTopSpecsForOwner(me));

        return "coverletters/form";
    }

    // 자소서 수정 처리
    @PostMapping("/{id}/edit")
    public String update(@PathVariable("id") Long id,
                         @Valid @ModelAttribute("coverLetter") CoverLetterDto dto,
                         BindingResult bindingResult,
                         Principal principal,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        User me = getCurrentUser(principal);

        // ✅ 서버에서도 글자수 초과 검증
        validateContentLength(dto, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("limits", LIMIT_PRESETS);
            model.addAttribute("isEdit", true);
            model.addAttribute("specs", specService.getTopSpecsForOwner(me));
            return "coverletters/form";
        }

        coverLetterService.update(me, id, dto);

        redirectAttributes.addFlashAttribute("message", "자소서가 수정되었습니다.");
        return "redirect:/coverletters";
    }

    // 자소서 삭제
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id,
                         Principal principal,
                         RedirectAttributes redirectAttributes) {

        User me = getCurrentUser(principal);
        coverLetterService.delete(me, id);

        redirectAttributes.addFlashAttribute("message", "자소서가 삭제되었습니다.");
        return "redirect:/coverletters";
    }

    // ===== 내부 유효성 검증 메서드 =====

    // 내용 길이가 글자수 제한을 넘는지 서버에서 한 번 더 체크
    private void validateContentLength(CoverLetterDto dto, BindingResult bindingResult) {
        String content = dto.getContent();
        Integer limit = dto.getLimitChars();

        if (content == null || limit == null) {
            return; // 나머지는 JSR-303(@NotBlank/@NotNull)에게 맡김
        }

        if (content.length() > limit) {
            bindingResult.rejectValue(
                    "content",
                    "content.tooLong",
                    "내용 글자 수가 설정한 글자수 제한을 초과했습니다."
            );
        }
    }
}
