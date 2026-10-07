package com.capstone.jobtracker.service;

import com.capstone.jobtracker.dto.SpecDto;
import com.capstone.jobtracker.model.Spec;
import com.capstone.jobtracker.model.User;
import com.capstone.jobtracker.repository.SpecRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecService {

    private final SpecRepository specRepository;

    /**
     * 스펙 목록 + 검색 + 정렬 (페이지 단위)
     *
     * @param owner   현재 로그인 사용자
     * @param keyword 검색어 (제목/발행처/태그)
     * @param page    페이지 번호 (0부터 시작)
     * @param size    페이지 크기
     * @param sortKey 정렬 기준 (latest / oldest / title)
     */
    public Page<SpecDto> searchPage(User owner,
                                    String keyword,
                                    int page,
                                    int size,
                                    String sortKey) {

        // 정렬 기준 선택
        Sort sort;
        switch (sortKey) {
            case "oldest":
                // 취득일 오래된 순
                sort = Sort.by(Sort.Direction.ASC, "acquiredAt", "id");
                break;
            case "title":
                // 제목 가나다 순
                sort = Sort.by(Sort.Direction.ASC, "title", "id");
                break;
            default: // "latest"
                // 취득일 최신 순
                sort = Sort.by(Sort.Direction.DESC, "acquiredAt", "id");
                break;
        }

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Spec> specPage;
        if (keyword == null || keyword.isBlank()) {
            // 검색어 없으면 전체 목록
            specPage = specRepository.findByOwner(owner, pageable);
        } else {
            // 검색어 있으면 제목/태그/발행처 검색
            specPage = specRepository.searchByOwnerAndKeyword(owner, keyword.trim(), pageable);
        }

        // 엔티티 페이지 -> DTO 페이지로 변환
        return specPage.map(this::toDto);
    }

    // 단건 조회 (상세/수정용) - owner 검증 포함
    public SpecDto getSpec(User owner, Long id) {
        Spec spec = specRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new IllegalArgumentException("스펙을 찾을 수 없거나 권한이 없습니다."));
        return toDto(spec);
    }

    // 등록
    public SpecDto create(User owner, SpecDto dto) {
        Spec spec = toEntityForCreate(dto, owner);
        Spec saved = specRepository.save(spec);
        return toDto(saved);
    }

    // 수정
    public SpecDto update(User owner, SpecDto dto) {
        if (dto.getId() == null) {
            throw new IllegalArgumentException("수정하려면 id가 필요합니다.");
        }

        Spec spec = specRepository.findByIdAndOwner(dto.getId(), owner)
                .orElseThrow(() -> new IllegalArgumentException("스펙을 찾을 수 없거나 권한이 없습니다."));

        updateEntityFromDto(spec, dto);
        Spec saved = specRepository.save(spec);
        return toDto(saved);
    }

    // 삭제
    public void delete(User owner, Long id) {
        Spec spec = specRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new IllegalArgumentException("스펙을 찾을 수 없거나 권한이 없습니다."));
        specRepository.delete(spec);
    }
    public List<Spec> getTopSpecsForOwner(User owner) {
        return specRepository.findByOwnerOrderByAcquiredAtDesc(owner);
    }
    // ===== 엔티티 <-> DTO 변환 메서드들 =====

    private SpecDto toDto(Spec spec) {
        if (spec == null) {
            return null;
        }
        return SpecDto.builder()
                .id(spec.getId())
                .title(spec.getTitle())
                .issuer(spec.getIssuer())
                .acquiredAt(spec.getAcquiredAt())
                .tags(spec.getTags())
                .build();
    }

    // 신규 등록용 엔티티 생성
    private Spec toEntityForCreate(SpecDto dto, User owner) {
        return Spec.builder()
                .title(dto.getTitle())
                .issuer(normalize(dto.getIssuer()))
                .acquiredAt(dto.getAcquiredAt())
                .tags(normalize(dto.getTags()))
                .owner(owner)
                .build();
    }

    // 수정 시 기존 엔티티에 DTO 값 반영
    private void updateEntityFromDto(Spec spec, SpecDto dto) {
        spec.setTitle(dto.getTitle());
        spec.setIssuer(normalize(dto.getIssuer()));
        spec.setAcquiredAt(dto.getAcquiredAt());
        spec.setTags(normalize(dto.getTags()));
    }

    // 공백 정리 (null 허용)
    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public long countByOwner(User owner) {
        return specRepository.countByOwner(owner);
    }
}
