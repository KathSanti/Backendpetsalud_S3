package com.petsalud.backendpetsalud.dto;

import com.petsalud.backendpetsalud.entity.Usuario;

public record UsuarioResponse (
    Long id,
    String nombre,
    String apellido,
    String email,
    String telefono,
    String direccion,
    String rol

) {

    public static UsuarioResponse desde (Usuario usuario){
        return new UsuarioResponse(
            usuario.getId(),
            usuario.getNombre(),
            usuario.getApellido(),
            usuario.getEmail(),
            usuario.getTelefono(),
            usuario.getDireccion(),
            usuario.getRol().name()
        );
    }

    
    
}
