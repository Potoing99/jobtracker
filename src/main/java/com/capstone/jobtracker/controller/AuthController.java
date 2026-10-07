package com.capstone.jobtracker.controller;

import com.capstone.jobtracker.model.User;                 // 로그인한 사용자 엔티티
import com.capstone.jobtracker.service.*;
import lombok.RequiredArgsConstructor;                     // 생성자 자동 주입
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


import java.security.Principal;                            // 현재 로그인한 사용자 정보
import java.util.Collections;                              // 빈 리스트 만들 때 사용

@Controller
@RequiredArgsConstructor
public class AuthController {

    // 서비스 주입
    private final UserService userService;
    private final ApplicationService applicationService;
    private final InterviewService interviewService;
    private final ResumeService resumeService;
    private final SpecService specService;
    private final CoverLetterService coverLetterService;

    // 로그인 페이지
    @GetMapping("/login")     // GET /login → login.html
    public String loginPage() {
        return "login";
    }

    // 홈 페이지 (/home)
    @GetMapping("/home")      // 로그인 성공 후 이동
    public String homePage(Principal principal, Model model) {

        if (principal != null) { // 로그인 되어 있을 때만 집계
            String email = principal.getName();                 // 현재 로그인한 사용자의 이메일
            User owner = userService.getByEmailOrThrow(email);  // 이메일로 User 엔티티 조회

            // 1) Overdue = 이미 마감 지난 공고
            long overdueCount = applicationService.countOverdueApplications(owner);

            // 2) D-3 이내 = 오늘 ~ 3일 뒤까지
            long d3Count = applicationService.countApplicationsDueInRange(owner, 0, 3);

            // 3) D-7 이내 = 4일 ~ 7일 뒤
            long d7Count = applicationService.countApplicationsDueInRange(owner, 4, 7);

            // 4) D-14 이내 = 8일 ~ 14일 뒤
            long d14Count = applicationService.countApplicationsDueInRange(owner, 8, 14);

            // 뷰에 전달 (D-Day 요약)
            model.addAttribute("overdueCount", overdueCount);
            model.addAttribute("d3Count", d3Count);
            model.addAttribute("d7Count", d7Count);
            model.addAttribute("d14Count", d14Count);

            // ✅ 홈 화면용 임박 지원 TOP 5 리스트 (로그인 된 경우에만)
            model.addAttribute(
                    "upcomingApplications",
                    applicationService.findTop5UpcomingApplications(owner)
            );

            // ✅ 다가오는 면접 TOP 5
            model.addAttribute(
                    "upcomingInterviews",
                    interviewService.findTop5UpcomingInterviews(owner)
            );

            // ✅ 이력서 / 스펙 / 자소서 개수
            long resumeCount = resumeService.countByOwner(owner);
            long specCount = specService.countByOwner(owner);
            long coverLetterCount = coverLetterService.countByOwner(owner);

            model.addAttribute("resumeCount", resumeCount);
            model.addAttribute("specCount", specCount);
            model.addAttribute("coverLetterCount", coverLetterCount);


        } else {
            // 비로그인 상태에서 /home 접근하는 경우를 대비한 기본값
            model.addAttribute("overdueCount", 0L);
            model.addAttribute("d3Count", 0L);
            model.addAttribute("d7Count", 0L);
            model.addAttribute("d14Count", 0L);

            // 로그인 안 돼 있으면 빈 리스트
            model.addAttribute("upcomingApplications", Collections.emptyList());
            model.addAttribute("upcomingInterviews", Collections.emptyList());
        }

        return "home"; // templates/home.html 렌더링
    }
}
