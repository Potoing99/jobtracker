# JobTracker — 취업 지원 종합 관리

취업 준비 과정에서 흩어지기 쉬운 **지원 현황, 이력서, 자기소개서, 면접, 스펙**을 한 곳에서 관리하는 웹 서비스입니다.

## 주요 기능

- 회원가입 / 로그인 (Spring Security)
- 지원 현황 관리: 기업별 지원 상태(ApplicationStatus)와 D-Day 표시
- 이력서 파일 업로드 및 파일 검증
- 자기소개서 작성·관리
- 면접 기록 관리
- 스펙(자격증 등) 관리

## 기술 스택

| 구분 | 사용 기술 |
|---|---|
| Backend | Java 17, Spring Boot 3.5, Spring Data JPA, Spring Security |
| Frontend | Thymeleaf, HTML/CSS |
| Database | MySQL |
| Build | Gradle |

## 프로젝트 구조

```
src/main/java/com/capstone/jobtracker
├── config       # 보안 설정, 사용자 인증
├── controller   # 화면/요청 처리
├── dto          # 요청·응답 데이터
├── model        # JPA 엔티티
├── repository   # DB 접근
├── service      # 비즈니스 로직
└── util         # D-Day 계산, 파일 검증
src/main/resources/templates   # Thymeleaf 화면
```

## 실행 방법

1. MySQL에서 데이터베이스를 만듭니다.
   ```sql
   CREATE DATABASE jobtracker DEFAULT CHARACTER SET utf8mb4;
   ```
2. 설정 파일을 복사하고 DB 비밀번호를 입력합니다.
   ```
   src/main/resources/application-example.yml  →  application.yml
   ```
3. 실행합니다.
   ```bash
   ./gradlew bootRun
   ```
4. 브라우저에서 http://localhost:8090 으로 접속합니다.

## 진행 중 / 예정

- Spring AI + Ollama(로컬 LLM)를 활용한 AI 챗봇 기능 추가 (웹서비스실무 수업과 함께 진행 중)

## 스크린샷

> 여기에 주요 화면 캡처 2~3장을 넣으세요. (로그인 화면, 지원 현황 목록, 상세 화면)
