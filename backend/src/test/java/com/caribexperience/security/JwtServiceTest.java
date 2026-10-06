package com.caribexperience.security;

import com.caribexperience.domain.Rol;
import com.caribexperience.domain.Usuario;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias de JwtService: no requieren Spring context ni base de
 * datos, solo verifican la logica pura de generacion/validacion de tokens.
 */
class JwtServiceTest {

    private static final String SECRETO_TEST = "un-secreto-de-prueba-de-al-menos-256-bits-1234567890";

    private JwtService jwtService;
    private UsuarioPrincipal principal;

    @BeforeEach
    void setUp() {
        Rol rolGuia = Rol.builder().id(1L).nombre("GUIA").build();
        Usuario usuario = Usuario.builder()
                .id(1L).nombre("Carlos").apellido("Perez").email("guia1@test.co")
                .passwordHash("hash").rol(rolGuia).activo(true).build();
        principal = new UsuarioPrincipal(usuario);
    }

    @Test
    void generarToken_deberiaProducirUnTokenQueContieneElEmailComoSubject() {
        jwtService = new JwtService(SECRETO_TEST, 86400000L);

        String token = jwtService.generarToken(principal);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extraerEmail(token)).isEqualTo("guia1@test.co");
    }

    @Test
    void esTokenValido_deberiaRetornarTrue_cuandoElTokenCorrespondeAlPrincipalYNoHaExpirado() {
        jwtService = new JwtService(SECRETO_TEST, 86400000L);

        String token = jwtService.generarToken(principal);

        assertThat(jwtService.esTokenValido(token, principal)).isTrue();
    }

    @Test
    void esTokenValido_deberiaRetornarFalse_cuandoElTokenPerteneceAOtroUsuario() {
        jwtService = new JwtService(SECRETO_TEST, 86400000L);

        Rol rolViajero = Rol.builder().id(2L).nombre("VIAJERO").build();
        Usuario otroUsuario = Usuario.builder()
                .id(2L).nombre("Ana").apellido("Gomez").email("viajero1@test.co")
                .passwordHash("hash").rol(rolViajero).activo(true).build();
        UsuarioPrincipal otroPrincipal = new UsuarioPrincipal(otroUsuario);

        String token = jwtService.generarToken(principal); // token de guia1@test.co

        assertThat(jwtService.esTokenValido(token, otroPrincipal)).isFalse();
    }

    @Test
    void extraerEmail_deberiaLanzarExcepcion_cuandoElTokenYaExpiro() throws InterruptedException {
        // Expiracion casi inmediata para forzar el caso de token vencido.
        jwtService = new JwtService(SECRETO_TEST, 1L);

        String token = jwtService.generarToken(principal);
        Thread.sleep(20);

        assertThatThrownBy(() -> jwtService.extraerEmail(token))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
