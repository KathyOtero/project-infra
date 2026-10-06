package com.caribexperience.web.controller;

import com.caribexperience.exception.GlobalExceptionHandler;
import com.caribexperience.security.CustomUserDetailsService;
import com.caribexperience.security.JwtAuthenticationFilter;
import com.caribexperience.security.JwtService;
import com.caribexperience.security.RestAccessDeniedHandler;
import com.caribexperience.security.RestAuthenticationEntryPoint;
import com.caribexperience.security.SecurityConfig;
import com.caribexperience.service.interfaces.ExperienciaService;
import com.caribexperience.web.dto.experiencia.ExperienciaRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integracion de la capa web (Etapa 6). A diferencia de los tests
 * unitarios de servicio (que mockean el repositorio), esta prueba carga el
 * contexto real de Spring MVC + Spring Security (SecurityConfig, filtro
 * JWT, manejadores de error) para validar que las REGLAS DE RUTA
 * (publico/protegido) definidas en SecurityConfig realmente se aplican,
 * algo que un test unitario puro con Mockito no puede verificar.
 *
 * El servicio de negocio (ExperienciaService) se mockea porque lo que se
 * prueba aqui es el comportamiento de la capa web/seguridad, no la logica
 * de negocio (ya cubierta en ExperienciaServiceImplTest).
 */
@WebMvcTest(controllers = ExperienciaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class ExperienciaControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExperienciaService experienciaService;

    // Dependencias transitivas de SecurityConfig que deben existir en el
    // contexto de este test slice (no se auto-configuran en @WebMvcTest).
    // NOTA: JwtAuthenticationFilter, RestAuthenticationEntryPoint y
    // RestAccessDeniedHandler se dejan como beans REALES (no @MockBean):
    // mockearlos anularia su comportamiento (un mock de un manejador de
    // errores no escribe el status HTTP), impidiendo verificar el 401/403
    // real. Solo se mockean sus dependencias externas (JwtService, etc.).
    @MockBean
    private CustomUserDetailsService customUserDetailsService;
    @MockBean
    private PasswordEncoder passwordEncoder;
    @MockBean
    private JwtService jwtService;

    private String requestJson() {
        return """
                {
                  "titulo": "Tour Cartagena",
                  "descripcion": "Recorrido por la ciudad amurallada",
                  "precio": 50.00,
                  "cupoMax": 3,
                  "fecha": "%s",
                  "hora": "10:00:00",
                  "ciudadId": 1,
                  "categoriaId": 1
                }
                """.formatted(LocalDate.now().plusDays(5));
    }

    @Test
    void get_listadoDeExperiencias_deberiaSerAccesibleSinAutenticacion() throws Exception {
        // GET /api/experiencias es publico (ver SecurityConfig): no requiere token.
        when(experienciaService.buscar(any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of()));

        mockMvc.perform(get("/api/experiencias"))
                .andExpect(status().isOk());
    }

    @Test
    void post_crearExperiencia_deberiaResponder401_cuandoNoHayTokenDeAutenticacion() throws Exception {
        mockMvc.perform(post("/api/experiencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "GUIA")
    void post_crearExperiencia_deberiaResponder400_cuandoElBodyEsInvalido() throws Exception {
        // Autenticado, pero titulo vacio -> falla @Valid antes de llegar al servicio.
        String bodyInvalido = """
                {
                  "titulo": "",
                  "descripcion": "desc",
                  "precio": 50.00,
                  "cupoMax": 3,
                  "fecha": "%s",
                  "hora": "10:00:00",
                  "ciudadId": 1,
                  "categoriaId": 1
                }
                """.formatted(LocalDate.now().plusDays(5));

        mockMvc.perform(post("/api/experiencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyInvalido))
                .andExpect(status().isBadRequest());
    }
}
