package com.capstone.jobtracker.config; // 1) 패키지: 파일 경로와 동일해야 함

// 2) import: 필요한 클래스 로딩
import com.capstone.jobtracker.repository.UserRepository; // DB 접근용 JPA 리포지토리
import lombok.RequiredArgsConstructor;                    // 생성자 주입 자동화(롬복)
import org.springframework.security.core.userdetails.UserDetails; // 시큐리티가 요구하는 사용자 표준
import org.springframework.security.core.userdetails.UserDetailsService; // 핵심 인터페이스
import org.springframework.security.core.userdetails.UsernameNotFoundException; // 못 찾았을 때 예외
import org.springframework.stereotype.Service;            // 스프링 빈 등록(서비스 계층)

// 3) @Service: 스프링이 이 클래스를 “서비스 빈”으로 등록하게 함
//    @RequiredArgsConstructor: final 필드를 매개변수로 받는 생성자를 자동 생성 → 생성자 주입
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    // 4) 의존성 주입 대상: DB에서 사용자 찾을 때 사용
    private final UserRepository userRepository;

    /**
     * 5) 스프링 시큐리티가 로그인 시 호출하는 단 하나의 메서드.
     *    - 파라미터 username: 우리가 username으로 'email'을 사용할 것.
     *    - 반환값: UserDetails (우리의 CustomUserDetails 로 감싸서 반환)
     *    - 못 찾으면 UsernameNotFoundException 던져야 함(시큐리티 규약)
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var user = userRepository.findByEmail(email) // 6) 이메일로 DB 조회
                .orElseThrow(() ->
                        new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        // 7) 우리가 만든 어댑터로 감싸 반환 → 시큐리티가 비밀번호/권한 등을 여기서 읽어감
        return new CustomUserDetails(user);
    }
}
