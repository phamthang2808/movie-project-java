//package com.example.thangcachep.movie_project_be.config;
//
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//
//import java.util.Collection;
//
//public class CustomUserDetails implements UserDetails {
//    private Long id;
//    private String phone;
//    private String password;
//    private Collection<? extends GrantedAuthority> authorities;
//
//    public CustomUserDetails(Long id, String phone, String password,
//                             Collection<? extends GrantedAuthority> authorities) {
//        this.id = id;
//        this.phone = phone;
//        this.password = password;
//        this.authorities = authorities;
//    }
//
//    public Long getId() {
//        return id;
//    }
//
//    @Override
//    public Collection<? extends GrantedAuthority> getAuthorities() {
//        return authorities;
//    }
//
//    @Override
//    public String getPassword() {
//        return password;
//    }
//
//    @Override
//    public String getUsername() {
//        return phone;
//    }
//
//    @Override
//    public boolean isAccountNonExpired() { return true; }
//
//    @Override
//    public boolean isAccountNonLocked() { return true; }
//
//    @Override
//    public boolean isCredentialsNonExpired() { return true; }
//
//    @Override
//    public boolean isEnabled() { return true; }
//}
