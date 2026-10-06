package com.caribexperience.service;

import com.caribexperience.domain.Rol;
import com.caribexperience.domain.Usuario;
import com.caribexperience.exception.EmailYaRegistradoException;
import com.caribexperience.exception.RecursoNoEncontradoException;
import com.caribexperience.repository.RolRepository;
import com.caribexperience.repository.UsuarioRepository;
import com.caribexperience.service.impl.UsuarioServiceImpl;
import com.caribexperience.web.dto.usuario.UsuarioRegistroRequest;
import com.caribexperience.web.dto.usuario.UsuarioResponse;
import com.caribexperience.web.mapper.UsuarioMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del servicio de usuarios, con todas las dependencias
 * mockeadas (Mockito). No requieren base de datos: prueban unicamente la
 * logica de negocio de UsuarioServiceImpl de forma aislada y rapida.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UsuarioMapper usuarioMapper;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private Rol rolGuia;

    @BeforeEach
    void setUp() {
        rolGuia = Rol.builder().id(1L).nombre("GUIA").build();
    }

    @Test
    void registrar_deberiaCrearUsuarioConPasswordHasheado_cuandoEmailNoExiste() {
        UsuarioRegistroRequest request = new UsuarioRegistroRequest(
                "Carlos", "Perez", "guia1@test.co", "password123", "GUIA");

        when(usuarioRepository.existsByEmail("guia1@test.co")).thenReturn(false);
        when(rolRepository.findByNombre("GUIA")).thenReturn(Optional.of(rolGuia));
        when(passwordEncoder.encode("password123")).thenReturn("hash-bcrypt-simulado");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(usuarioMapper.toResponse(any(Usuario.class))).thenReturn(
                new UsuarioResponse(1L, "Carlos", "Perez", "guia1@test.co", "GUIA", true, null));

        UsuarioResponse response = usuarioService.registrar(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("guia1@test.co");
        assertThat(response.rol()).isEqualTo("GUIA");

        // Verifica que NUNCA se guarda la contrasena en texto plano.
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hash-bcrypt-simulado");
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("password123");
    }

    @Test
    void registrar_deberiaLanzarEmailYaRegistrado_cuandoElEmailYaExiste() {
        UsuarioRegistroRequest request = new UsuarioRegistroRequest(
                "Carlos", "Perez", "duplicado@test.co", "password123", "GUIA");

        when(usuarioRepository.existsByEmail("duplicado@test.co")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.registrar(request))
                .isInstanceOf(EmailYaRegistradoException.class)
                .hasMessageContaining("duplicado@test.co");

        // No debe intentar guardar nada si el email ya existe.
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrar_deberiaLanzarRecursoNoEncontrado_cuandoElRolNoExisteEnCatalogo() {
        UsuarioRegistroRequest request = new UsuarioRegistroRequest(
                "Carlos", "Perez", "nuevo@test.co", "password123", "ADMIN");

        when(usuarioRepository.existsByEmail("nuevo@test.co")).thenReturn(false);
        when(rolRepository.findByNombre("ADMIN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.registrar(request))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void obtenerPorId_deberiaLanzarRecursoNoEncontrado_cuandoElUsuarioNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.obtenerPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");
    }
}
