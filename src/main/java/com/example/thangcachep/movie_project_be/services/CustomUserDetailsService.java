//package com.example.thangcachep.movie_project_be.services;
//
//import com.example.thangcachep.movie_project_be.config.CustomUserDetails;
//import com.example.thangcachep.movie_project_be.entities.UserEntity;
//import com.example.thangcachep.movie_project_be.repositories.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class CustomUserDetailsService implements UserDetailsService {
//
//    private final UserRepository userRepository;
//
//    @Override
//    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
//        UserEntity user = userRepository.findByPhoneNumber(phone)
//                .orElseThrow(() -> new UsernameNotFoundException("user not found"));
//        return new CustomUserDetails(
//                user.getUserId(),
//                user.getPhoneNumber(),
//                user.getPassword(),
//                user.getAuthorities()
//        );
//    }
//}
