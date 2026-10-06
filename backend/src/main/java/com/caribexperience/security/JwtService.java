package com.caribexperience.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Encapsula toda la logica de generacion y validacion de JWT (libreria
 * jjwt). Los claims incluyen el id y el rol del usuario para que el filtro
 * pueda reconstruir el principal sin volver a golpear la base de datos en
 * cada request (salvo para lo estrictamente necesario).
 */
@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.expiration-ms}") long expiracionMs) {
        // HMAC-SHA necesita una clave de al menos 256 bits; el secreto de
        // application.yml se codifica a bytes UTF-8 directamente.
        this.clave = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiracionMs = expiracionMs;
    }

    public String generarToken(UsuarioPrincipal principal) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expiracionMs);

        return Jwts.builder()
                .subject(principal.getEmail())
                .claims(Map.of(
                        "id", principal.getId(),
                        "rol", principal.getRol(),
                        "nombre", principal.getNombre()
                ))
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(clave)
                .compact();
    }

    public String extraerEmail(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    public boolean esTokenValido(String token, UsuarioPrincipal principal) {
        String email = extraerEmail(token);
        return email.equals(principal.getEmail()) && !haExpirado(token);
    }

    private boolean haExpirado(String token) {
        return extraerClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extraerClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}
