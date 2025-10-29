package com.example.thangcachep.movie_project_be.filters;

import com.example.thangcachep.movie_project_be.components.JwtTokenUtils;
import com.example.thangcachep.movie_project_be.entities.UserEntity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {
    @Value("${api.prefix}")
    private String apiPrefix;

    private final UserDetailsService userDetailsService;
    private final JwtTokenUtils jwtTokenUtil;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String uri = request.getRequestURI();       // robust hơn servletPath
        final String method = request.getMethod();

        try {
            // 1) Bỏ qua các path public / preflight / error
            if (isBypass(uri, method)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 2) Nếu KHÔNG có header Bearer -> cho qua (để permitAll/authorize quyết định)
            final String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            // 3) Có token -> validate
            final String token = authHeader.substring(7);
            final String phoneNumber = jwtTokenUtil.extractPhoneNumber(token);

            if (phoneNumber != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                log.info("Auth header = {}", authHeader);
                log.info("SecurityContext = {}", SecurityContextHolder.getContext().getAuthentication());

                UserEntity userDetails = (UserEntity) userDetailsService.loadUserByUsername(phoneNumber);
                if (jwtTokenUtil.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    // Có Bearer nhưng token invalid -> 401
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                    return;
                }
            }

            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            // Đừng gửi 401 cho mọi lỗi bắt được; dọn context và cho chain xử lý /error
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
        }
    }

    private boolean isBypass(String uri, String method) {
        String base = ensureLeadingSlash(apiPrefix); // đảm bảo bắt đầu bằng '/'

        // Public endpoints
        if ("POST".equals(method) && uri.startsWith(base + "/users/register")) return true;
        if ("POST".equals(method) && uri.startsWith(base + "/users/login"))    return true;
//        if ("GET".equals(method)  && uri.startsWith(base + "/rooms") )         return true;
//        if ("POST".equals(method)  && uri.startsWith(base + "/rooms") )         return true;  // role staff, admin phai k dc pass de lay token xac thuc dc role
        if ("GET".equals(method)  && uri.startsWith("/uploads"))               return true; // ảnh public
        // Health, docs… (tuỳ bạn bật)
         if ("GET".equals(method) && uri.startsWith(base + "/healthcheck")) return true;
        if ("GET".equals(method) && uri.startsWith(base + "/test")) return true;
        //vo danh la qua dc anonymous
        // CORS preflight & error forward
        if ("OPTIONS".equals(method)) return true;
        if ("/error".equals(uri))     return true;

        return false;
    }

    private String ensureLeadingSlash(String p) {
        if (p == null || p.isEmpty()) return "";
        return p.startsWith("/") ? p : ("/" + p);
    }
}







//@Component
//@RequiredArgsConstructor
//
//public class JwtTokenFilter extends OncePerRequestFilter{
//    @Value("${api.prefix}")
//    private String apiPrefix;
//    private final UserDetailsService userDetailsService;
//    private final JwtTokenUtils jwtTokenUtil;
//    @Override
//    protected void doFilterInternal(@NonNull  HttpServletRequest request,
//                                    @NonNull HttpServletResponse response,
//                                    @NonNull FilterChain filterChain)
//            throws ServletException, IOException {
//        try {
//            if(isBypassToken(request)) {
//                filterChain.doFilter(request, response); //enable bypass
//                return;
//            }
//            final String authHeader = request.getHeader("Authorization");
//            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
//                return;
//            }
//            final String token = authHeader.substring(7);
//            final String phoneNumber = jwtTokenUtil.extractPhoneNumber(token);
//            if (phoneNumber != null
//                    && SecurityContextHolder.getContext().getAuthentication() == null) {
//                UserEntity userDetails = (UserEntity) userDetailsService.loadUserByUsername(phoneNumber);
//                if(jwtTokenUtil.validateToken(token, userDetails)) {
//                    UsernamePasswordAuthenticationToken authenticationToken =
//                            new UsernamePasswordAuthenticationToken(
//                                    userDetails,
//                                    null,
//                                    userDetails.getAuthorities()
//                            );
//                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
//                }
//            }
//            filterChain.doFilter(request, response); //enable bypass
//        }catch (Exception e) {
//            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
//        }
//
//    }
//    private boolean isBypassToken(@NonNull HttpServletRequest request) {
//        final List<Pair<String, String>> bypassTokens = Arrays.asList(
////                Pair.of(String.format("%s/roles", apiPrefix), "GET"),
////                Pair.of(String.format("%s/healthcheck/health", apiPrefix), "GET"),
////                Pair.of(String.format("%s/roles", apiPrefix), "GET"),
////                Pair.of(String.format("%s/products", apiPrefix), "GET"),
////                Pair.of(String.format("%s/categories", apiPrefix), "GET"),
//                Pair.of(String.format("%s/users/register", apiPrefix), "POST"),
//                Pair.of(String.format("%s/users/login", apiPrefix), "POST")
//        );
//
//        String requestPath = request.getServletPath();
//        String requestMethod = request.getMethod();
//
////        if (requestPath.startsWith(String.format("/%s/orders", apiPrefix))
////                && requestMethod.equals("GET")) {
////            // Check if the requestPath matches the desired pattern
////            if (requestPath.matches(String.format("/%s/orders/\\d+", apiPrefix))) {
////                return true;
////            }
////            // If the requestPath is just "%s/orders", return true
////            if (requestPath.equals(String.format("/%s/orders", apiPrefix))) {
////                return true;
////            }
////        }
//        for (Pair<String, String> bypassToken : bypassTokens) {
//            if (requestPath.contains(bypassToken.getFirst())
//                    && requestMethod.equals(bypassToken.getSecond())) {
//                return true;
//            }
//        }
//
//        return false;
//    }
//}
