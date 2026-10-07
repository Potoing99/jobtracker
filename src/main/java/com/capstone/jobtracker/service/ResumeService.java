// 간단 주석만
package com.capstone.jobtracker.service;

import com.capstone.jobtracker.model.Resume;
import com.capstone.jobtracker.model.User;
import com.capstone.jobtracker.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;

    private static final Path ROOT = Paths.get("uploads/resumes");

    // 드롭다운용
    public List<Resume> list(User owner) {
        return resumeRepository.findByOwnerOrderByUploadedAtDesc(owner);
    }

    // 목록/검색/정렬/페이징
    public Page<Resume> searchPage(User owner, String q, String sort, Integer page, Integer size) {
        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size <= 0) ? 10 : size;
        Pageable pageable = PageRequest.of(p, s, resolveSort(sort));
        if (q == null || q.trim().isEmpty()) {
            return resumeRepository.findByOwner(owner, pageable);
        }
        return resumeRepository.searchByOwnerAndKeyword(owner, q.trim(), pageable);
    }

    // 업로드
    public Resume upload(User owner, String title, MultipartFile file) {
        try {
            Files.createDirectories(ROOT);
            String original = sanitize(file.getOriginalFilename());
            String ext = (original != null && original.contains(".")) ? original.substring(original.lastIndexOf('.')+1) : "";
            String stored = UUID.randomUUID() + (ext.isBlank() ? "" : ("." + ext));
            Path target = ROOT.resolve(stored).normalize();

            // 간단 검증
            if (file.isEmpty()) throw new IllegalArgumentException("빈 파일입니다.");
            if (!target.getParent().equals(ROOT.toAbsolutePath().normalize())) {
                throw new SecurityException("잘못된 경로");
            }

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            Resume r = new Resume();
            r.setTitle(title);
            r.setOriginalFilename(original);
            r.setStoredFilename(stored);
            r.setContentType(file.getContentType());
            r.setSize(file.getSize());
            r.setUploadedAt(LocalDateTime.now());
            r.setOwner(owner);

            return resumeRepository.save(r);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패", e);
        }
    }

    // 단건(소유자)
    public Resume getOwned(Long id, User owner) {
        return resumeRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new IllegalArgumentException("데이터를 찾을 수 없거나 권한이 없습니다."));
    }

    // 리소스로 로딩
    public Resource loadAsResource(Resume r) {
        Path path = ROOT.resolve(r.getStoredFilename()).normalize();
        Resource res = new FileSystemResource(path.toFile());
        if (!res.exists()) throw new IllegalArgumentException("파일이 존재하지 않습니다.");
        return res;
    }

    // 삭제(파일+DB)
    public void deleteOwned(Long id, User owner) {
        Resume r = getOwned(id, owner);
        Path path = ROOT.resolve(r.getStoredFilename()).normalize();
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {}
        resumeRepository.delete(r);
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) return Sort.by(Sort.Order.desc("uploadedAt"));
        return switch (sort) {
            case "dateAsc"   -> Sort.by(Sort.Order.asc("uploadedAt"));
            case "titleAsc"  -> Sort.by(Sort.Order.asc("title"));
            case "titleDesc" -> Sort.by(Sort.Order.desc("title"));
            case "sizeAsc"   -> Sort.by(Sort.Order.asc("size"));
            case "sizeDesc"  -> Sort.by(Sort.Order.desc("size"));
            default          -> Sort.by(Sort.Order.desc("uploadedAt"));
        };
    }

    private String sanitize(String name) {
        if (name == null) return null;
        return name.replace("\\", "_").replace("/", "_");
    }

    public long countByOwner(User owner) {
        return resumeRepository.countByOwner(owner);
    }
}
