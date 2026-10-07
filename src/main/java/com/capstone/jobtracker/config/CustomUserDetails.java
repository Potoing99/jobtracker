package com.capstone.jobtracker.config;

import com.capstone.jobtracker.model.User; // 우리 프로젝트의 User 엔티티
import org.springframework.security.core.GrantedAuthority; // "권한"을 나타내는 인터페이스
import org.springframework.security.core.authority.SimpleGrantedAuthority; // 문자열 한 개로 권한 표현
import org.springframework.security.core.userdetails.UserDetails; // 시큐리티가 요구하는 사용자 표준 인터페이스

import java.util.Collection; // 여러 개(집합)를 담을 수 있는 타입(인터페이스)
import java.util.List;       // List 구현체(불변 리스트 생성에 List.of 사용)


public class CustomUserDetails implements UserDetails {
    private final User user;


    public CustomUserDetails(User user) {
        this.user = user; // this.user(필드) = user(매개변수)
    }

    // ★ 헤더에서 [[${#authentication.principal.name}]] 로 쓸 이름
    public String getName() {
        return user.getName();
    }

    // 6) @Override: 부모(인터페이스/부모클래스) 메서드를 "재정의"한다는 뜻
    // 반환타입: Collection<? extends GrantedAuthority>
    //  - Collection : 여러 권한을 담는 "그릇"의 인터페이스
    //  - ? extends GrantedAuthority : GrantedAuthority(권한) 또는 그 자식 타입들을 담을 수 있다는 제네릭 문법
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 7) 권한 이름 규칙: 반드시 "ROLE_" 접두어가 붙어야 함
        //    예) STUDENT -> ROLE_STUDENT, ADMIN -> ROLE_ADMIN
        String roleName = "ROLE_" + user.getRole().name();
        // 8) SimpleGrantedAuthority: 문자열 하나로 권한 표현하는 가장 간단한 구현체
        //    List.of(...) : 자바 9+의 불변 리스트 생성(한 번 만든 뒤 수정 불가 -> 안전)
        return List.of(new SimpleGrantedAuthority(roleName));
    }

    // 9) username(로그인 식별자)으로 "email"을 사용하기로 했으니 그대로 반환
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    // 10) 패스워드 반환 (주의: 이미 암호화(BCrypt)된 해시 문자열이 들어있어야 함)
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    // 11) 계정 상태 플래그들: 지금은 전부 true(정상)로 처리
    // 필요하면 추후 User 엔티티에 필드를 추가해 실제 상태를 반영 가능
    @Override public boolean isAccountNonExpired() { return true; }     // 계정 만료 X
    @Override public boolean isAccountNonLocked() { return true; }      // 계정 잠금 X
    @Override public boolean isCredentialsNonExpired() { return true; } // 자격만료 X
    @Override public boolean isEnabled() { return true; }               // 활성화 O

    // 12) 우리 쪽 코드(컨트롤러/서비스)에서 원본 User가 필요할 수 있으니 getter 제공(선택)
    public User getUser() {
        return this.user;
    }
}
