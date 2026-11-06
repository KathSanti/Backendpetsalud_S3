package com.petsalud.backendpetsalud.security;


import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.petsalud.backendpetsalud.monitoreo.SuspiciousActivityService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import java.io.IOException;

@Component 
@AllArgsConstructor 
public class JwtFilter extends OncePerRequestFilter {

    //Bearer 
    private static final String PREFIJO = JwtService.TIPO_TOKEN + " ";

    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;
    private final SuspiciousActivityService monitoreo;

    @Override 
    protected void doFilterInternal(

        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain

    )throws ServletException, IOException{
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if(header != null && header.startsWith(PREFIJO) 
            && SecurityContextHolder.getContext().getAuthentication() == null){

        try{    
            String token = header.substring(PREFIJO.length());
            Claims claims = jwtService.validationToken(token);
            UserDetails user = usuarioDetailsService.loadUserByUsername(claims.getSubject());

            if (user.isEnabled() && user.isAccountNonLocked() && user.isAccountNonExpired()) {
                var authentication = new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
                monitoreo.registrarTokenInvalido(request.getRemoteAddr(), request.getRequestURI(), ex.getMessage());
                SecurityContextHolder.clearContext();
        }
       
    }
       filterChain.doFilter(request, response);
    
}
 
}
