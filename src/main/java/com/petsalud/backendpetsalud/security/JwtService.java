package com.petsalud.backendpetsalud.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service 
public class JwtService {

    public static final String TIPO_TOKEN = "Bearer";

    private final SecretKey secretKey;
    private final String issuer;
    private final long expirationMinutes;


    public JwtService(

        @Value ("${jwt.secret-key}") String base64Secret,
        @Value ("${jwt.issuer}") String issuer,
        @Value ("${jwt.expiration-minutes}") long expirationMinutes

        
    ) {
        byte[] decode = Base64.getDecoder().decode(base64Secret);
        if(decode.length < 32) {
            throw new IllegalArgumentException("La clave secreta debe tener al menos 256 bits (32 bytes)");
        }
        this.secretKey = Keys.hmacShaKeyFor(decode);
        this.issuer = issuer;
        this.expirationMinutes = expirationMinutes;
    }


    public String generateToken(UserDetails user) {
        
        List<String> role = user .getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .toList();
        Instant now = Instant.now();
        return Jwts.builder()
              .issuer(issuer)
              .subject(user.getUsername())
              .claim("roles", role)
              .issuedAt(Date.from(now))
              .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
              .signWith(secretKey, Jwts.SIG.HS256)
              .compact();    
    }

    public Claims validationToken (String token){
        Claims claims = Jwts.parser()
               .verifyWith(secretKey)
               .requireIssuer(issuer)
               .build()
               .parseSignedClaims(token)
               .getPayload();
        if (claims.getSubject() == null || claims.getSubject().isBlank()){
            throw new IllegalArgumentException("El token no contiene un subject válido");
        }return claims;
    }

    public long getExpirationMinutes(){
        return expirationMinutes;

    }


    
}
