package com.caribexperience.security;

import com.caribexperience.domain.Usuario;
import com.caribexperience.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Puente entre Spring Security y el repositorio de usuarios. Spring Security
 * llama a este servicio (por email, que actua como "username") durante la
 * autenticacion y para reconstruir el principal en cada request autenticado.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    // @Transactional es obligatorio aqui: Usuario.rol es @ManyToOne(LAZY) y
    // open-in-view esta deshabilitado (buena practica). Sin una transaccion
    // abierta, usuario.getRol().getNombre() (dentro del constructor de
    // UsuarioPrincipal) lanza LazyInitializationException porque la sesion
    // de Hibernate ya se cerro al salir del repositorio.
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No existe un usuario con el email: " + email));
        return new UsuarioPrincipal(usuario);
    }
}
