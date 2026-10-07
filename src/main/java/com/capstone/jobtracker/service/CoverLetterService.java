package com.capstone.jobtracker.service;

import com.capstone.jobtracker.dto.CoverLetterDto;
import com.capstone.jobtracker.model.CoverLetter;
import com.capstone.jobtracker.model.User;
import com.capstone.jobtracker.repository.CoverLetterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본: 읽기 전용 트랜잭션
public class CoverLetterService {

    private final CoverLetterRepository coverLetterRepository;

    // 자소서 목록 (검색 + 정렬 + 페이징)
    public Page<CoverLetter> getPage(User owner,
                                     String keyword,
                                     String sort,
                                     int page,
                                     int size) {

        // 정렬 기준 결정
        Sort sortSpec;
        if ("oldest".equalsIgnoreCase(sort)) {
            // 오래된 순
            sortSpec = Sort.by(Sort.Direction.ASC, "updatedAt");
        } else if ("title".equalsIgnoreCase(sort)) {
            // 제목순
            sortSpec = Sort.by(Sort.Direction.ASC, "title");
        } else {
            // 기본: 최신 수정순
            sortSpec = Sort.by(Sort.Direction.DESC, "updatedAt");
        }

        Pageable pageable = PageRequest.of(page, size, sortSpec);

        // 검색어 없으면 owner만 조건
        if (keyword == null || keyword.trim().isEmpty()) {
            return coverLetterRepository.findByOwner(owner, pageable);
        }

        // 검색어 있으면 제목 OR 내용 검색
        String q = keyword.trim();
        return coverLetterRepository
                .findByOwnerAndTitleContainingIgnoreCaseOrOwnerAndContentContainingIgnoreCase(
                        owner, q,
                        owner, q,
                        pageable
                );
    }

    // 단일 자소서 조회 (소유자 검증)
    public CoverLetter getOneForOwner(User owner, Long id) {
        return coverLetterRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new IllegalArgumentException("자소서를 찾을 수 없거나 권한이 없습니다."));
    }

    // 자소서 신규 생성
    @Transactional
    public CoverLetter create(User owner, CoverLetterDto dto) {
        validateLimitChars(dto.getLimitChars());

        CoverLetter letter = CoverLetter.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .limitChars(dto.getLimitChars())
                .owner(owner)
                .build();

        return coverLetterRepository.save(letter);
    }

    // 자소서 수정
    @Transactional
    public CoverLetter update(User owner, Long id, CoverLetterDto dto) {
        validateLimitChars(dto.getLimitChars());

        CoverLetter letter = getOneForOwner(owner, id);

        letter.setTitle(dto.getTitle());
        letter.setContent(dto.getContent());
        letter.setLimitChars(dto.getLimitChars());
        // 영속 상태라서 트랜잭션 끝날 때 자동 UPDATE

        return letter;
    }

    // 자소서 삭제
    @Transactional
    public void delete(User owner, Long id) {
        CoverLetter letter = getOneForOwner(owner, id);
        coverLetterRepository.delete(letter);
    }

    // 글자수 제한 값 검증 (500 / 800 / 1000 / 1500 만 허용)
    private void validateLimitChars(Integer limitChars) {
        if (limitChars == null) {
            throw new IllegalArgumentException("글자수 제한을 선택해야 합니다.");
        }

        if (!(limitChars == 500 || limitChars == 800
                || limitChars == 1000 || limitChars == 1500)) {
            throw new IllegalArgumentException("지원하지 않는 글자수 제한 값입니다.");
        }
    }

    public long countByOwner(User owner) {
        return coverLetterRepository.countByOwner(owner);
    }
}
