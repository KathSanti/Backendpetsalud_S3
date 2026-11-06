package com.petsalud.backendpetsalud.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Pruebas unitarias de JwtService: generación, validación y rechazo de tokens manipulados.
 * No levantan Spring ni base de datos.
 */
class JwtServiceTest {

    private static final String EMISOR = "backendpetsalud";

    private byte[] claveBytes;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        claveBytes = claveAleatoria(32);
        jwtService = new JwtService(Base64.getEncoder().encodeToString(claveBytes), EMISOR, 30);
    }

    @Test
    @DisplayName("Genera un token con el email como subject, el emisor y el rol con prefijo ROLE_")
    void generaTokenValido() {
        String token = jwtService.generateToken(cliente());

        Claims claims = jwtService.validationToken(token);

        assertThat(claims.getSubject()).isEqualTo("cliente@petsalud.test");
        assertThat(claims.getIssuer()).isEqualTo(EMISOR);
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        assertThat(roles).containsExactly("ROLE_CLIENTE");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    @DisplayName("Rechaza un token cuyo contenido fue modificado (por ejemplo, para cambiar el rol)")
    void rechazaTokenAlterado() {
        String[] partes = jwtService.generateToken(cliente()).split("\\.");
        String contenidoFalso = "{\"iss\":\"" + EMISOR + "\",\"sub\":\"cliente@petsalud.test\","
                + "\"roles\":[\"ROLE_GERENTE\"],\"exp\":4102444800}";
        String payloadFalso = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(contenidoFalso.getBytes(StandardCharsets.UTF_8));
        String tokenAlterado = partes[0] + "." + payloadFalso + "." + partes[2];

        assertThatThrownBy(() -> jwtService.validationToken(tokenAlterado))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Rechaza un token firmado con otra clave secreta")
    void rechazaTokenDeOtraClave() {
        JwtService otroServidor = new JwtService(
                Base64.getEncoder().encodeToString(claveAleatoria(32)), EMISOR, 30);
        String tokenAjeno = otroServidor.generateToken(cliente());

        assertThatThrownBy(() -> jwtService.validationToken(tokenAjeno))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Rechaza un token expirado")
    void rechazaTokenExpirado() {
        SecretKey clave = Keys.hmacShaKeyFor(claveBytes);
        Instant haceDosHoras = Instant.now().minus(2, ChronoUnit.HOURS);
        String tokenExpirado = Jwts.builder()
                .issuer(EMISOR)
                .subject("cliente@petsalud.test")
                .issuedAt(Date.from(haceDosHoras))
                .expiration(Date.from(haceDosHoras.plus(30, ChronoUnit.MINUTES)))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.validationToken(tokenExpirado))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Rechaza un token emitido por otro sistema aunque use la misma clave")
    void rechazaEmisorIncorrecto() {
        JwtService otroEmisor = new JwtService(Base64.getEncoder().encodeToString(claveBytes), "otro-sistema", 30);
        String token = otroEmisor.generateToken(cliente());

        assertThatThrownBy(() -> jwtService.validationToken(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Rechaza texto que no es un JWT")
    void rechazaTokenMalFormado() {
        assertThatThrownBy(() -> jwtService.validationToken("esto.no.es-un-jwt"))
                .isInstanceOfAny(JwtException.class, IllegalArgumentException.class);
    }

    @Test
    @DisplayName("No arranca con una clave secreta menor a 32 bytes")
    void rechazaClaveDebil() {
        String claveCorta = Base64.getEncoder().encodeToString(claveAleatoria(16));

        assertThatThrownBy(() -> new JwtService(claveCorta, EMISOR, 30))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static UserDetails cliente() {
        return User.withUsername("cliente@petsalud.test").password("no-importa").roles("CLIENTE").build();
    }

    private static byte[] claveAleatoria(int bytes) {
        byte[] clave = new byte[bytes];
        new SecureRandom().nextBytes(clave);
        return clave;
    }
}
