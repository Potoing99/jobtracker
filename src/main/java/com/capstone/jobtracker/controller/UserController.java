package com.capstone.jobtracker.controller;

import com.capstone.jobtracker.dto.UserRegistrationDto;
import com.capstone.jobtracker.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 회원가입 폼 GET
    @GetMapping("/register/form")
    public String showRegisterForm(Model model) {
        model.addAttribute("userForm", new UserRegistrationDto());
        return "register"; // templates/register.html
    }

    // 회원가입 처리 POST
    @PostMapping("/register/form")
    public String submitRegisterForm(
            @Valid @ModelAttribute("userForm") UserRegistrationDto form,
            BindingResult bindingResult,
            Model model
    ) {
        // 1. 기본 검증 실패 → 다시 폼
        if (bindingResult.hasErrors()) {
            return "register";
        }

        // 2. 비밀번호 불일치 체크
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "passwordMismatch", "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
            return "register";
        }

        // 3. 이메일 중복 체크
        if (userService.existsByEmail(form.getEmail())) {
            bindingResult.rejectValue("email", "duplicate", "이미 등록된 이메일입니다.");
            return "register";
        }

        // 4. 회원가입 처리
        userService.register(form);

        // 5. 성공 후 로그인 페이지로 이동
        return "redirect:/login?registered";
    }
}
