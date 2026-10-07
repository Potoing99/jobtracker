package com.capstone.jobtracker.model;

/**
 * 지원 진행 상태 (뷰에서 드롭다운으로 선택)
 */
public enum ApplicationStatus {
    APPLIED,   // 지원 완료
    INTERVIEW, // 면접 예정/진행
    OFFER,     // 합격/오퍼
    REJECTED   // 불합격
}
