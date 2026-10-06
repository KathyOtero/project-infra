package com.caribexperience.web.controller;

import com.caribexperience.security.JwtService;
import com.caribexperience.security.UsuarioPrincipal;
import com.caribexperience.web.dto.auth.LoginRequest;
import com.caribexperience.web.dto.auth.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unico endpoint publico ademas del registro de usuarios. Delega la
 * verificacion de credenciales al AuthenticationManager de Spring Security
 * (que internamente usa CustomUserDetailsService + PasswordEncoder), y si
 * son validas emite un JWT firmado.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // Si las credenciales son invalidas, AuthenticationManager lanza
        // BadCredentialsException (subclase de AuthenticationException), que
        // se traduce a 401 en GlobalExceptionHandler.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        UsuarioPrincipal principal = (UsuarioPrincipal) authentication.getPrincipal();
        String token = jwtService.generarToken(principal);

        return ResponseEntity.ok(LoginResponse.de(
                token, principal.getId(), principal.getNombre(), principal.getEmail(), principal.getRol()));
    }
}
