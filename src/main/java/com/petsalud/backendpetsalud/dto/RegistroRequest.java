package com.petsalud.backendpetsalud.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroRequest(

    // Solo letras (incluye tildes y ñ), espacios, apóstrofo y guion: bloquea etiquetas como <script>
    @NotBlank @Size(max = 100)
    @Pattern(regexp = "^[\\p{L} '-]+$", message = "solo puede contener letras, espacios, apóstrofo o guion")
    String nombre,

    @NotBlank @Size(max = 100)
    @Pattern(regexp = "^[\\p{L} '-]+$", message = "solo puede contener letras, espacios, apóstrofo o guion")
    String apellido,

    @NotBlank @Email @Size(max = 254)
    String email,

    @Pattern(regexp = "^(\\+?56)?9\\d{8}$", message = "debe ser un celular chileno, por ejemplo +56912345678")
    String telefono,

    // BCrypt solo considera los primeros 72 bytes de la contraseña
    @NotBlank @Size(min = 8, max = 72)
    String password,

    // La dirección admite números y signos, pero no los caracteres < > usados en HTML
    @NotBlank @Size(max = 200)
    @Pattern(regexp = "^[^<>]*$", message = "no puede contener los caracteres < o >")
    String direccion
) {
}
