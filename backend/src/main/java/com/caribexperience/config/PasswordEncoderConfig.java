package com.caribexperience.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Se separa el PasswordEncoder de la configuracion de seguridad HTTP
 * (Etapa 5) porque el servicio de usuarios (Etapa 3) ya lo necesita para
 * hashear contrasenas al registrar, sin acoplarse todavia a Spring Security.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
