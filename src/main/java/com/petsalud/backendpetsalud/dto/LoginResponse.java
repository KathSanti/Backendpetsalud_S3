package com.petsalud.backendpetsalud.dto;

import com.petsalud.backendpetsalud.security.JwtService;

public record LoginResponse(

    String token,
    String type,
    String role,
    long expirationTime

) {
   //
    public LoginResponse(String token, String rol, long expiraEn) {
        this(token, JwtService.TIPO_TOKEN, rol, expiraEn);
    }
    
}
