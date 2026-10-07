// 간단 주석만
package com.capstone.jobtracker.controller;

import com.capstone.jobtracker.model.Resume;
import com.capstone.jobtracker.model.User;
import com.capstone.jobtracker.service.ResumeService;
import com.capstone.jobtracker.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/resumes")
public class ResumeController {

    private final ResumeService resumeService;
    private final UserService userService;

    private User me(Principal principal) {
        return userService.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("로그인 정보가 유효하지 않습니다."));
    }

    // 목록(검색/정렬/페이징)
    @GetMapping
    public String list(@RequestParam(value = "q",    required = false) String q,
                       @RequestParam(value = "sort", required = false, defaultValue = "dateDesc") String sort,
                       @RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
                       Principal principal, Model model) {
        Page<Resume> result = resumeService.searchPage(me(principal), q, sort, page, size);
        model.addAttribute("page", result);
        model.addAttribute("q", q);
        model.addAttribute("sort", sort);
        model.addAttribute("size", size);
        return "resumes/list";
    }

    // 업로드 폼
    @GetMapping("/new")
    public String uploadForm() {
        return "resumes/form";
    }

    // 업로드 처리(POST /resumes)
    @PostMapping
    public String upload(@RequestParam("title") String title,
                         @RequestParam("file") MultipartFile file,
                         Principal principal,
                         RedirectAttributes ra) {
        User owner = me(principal);
        resumeService.upload(owner, title, file);
        ra.addFlashAttribute("msg", "업로드 완료");
        return "redirect:/resumes";
    }

    // 다운로드
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, Principal principal) {
        User owner = me(principal);
        Resume r = resumeService.getOwned(id, owner);
        Resource resource = resumeService.loadAsResource(r);

        String encoded = URLEncoder.encode(
                (r.getOriginalFilename() == null ? "resume" : r.getOriginalFilename()),
                StandardCharsets.UTF_8
        ).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .header(HttpHeaders.CONTENT_TYPE, r.getContentType() == null ? "application/octet-stream" : r.getContentType())
                .body(resource);
    }

    // 삭제
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        User owner = me(principal);
        resumeService.deleteOwned(id, owner);
        ra.addFlashAttribute("msg", "삭제되었습니다.");
        return "redirect:/resumes";
    }
}
