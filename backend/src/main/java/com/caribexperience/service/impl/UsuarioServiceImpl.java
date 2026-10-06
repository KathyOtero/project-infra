package com.caribexperience.service.impl;

import com.caribexperience.domain.Rol;
import com.caribexperience.domain.Usuario;
import com.caribexperience.exception.EmailYaRegistradoException;
import com.caribexperience.exception.RecursoNoEncontradoException;
import com.caribexperience.repository.RolRepository;
import com.caribexperience.repository.UsuarioRepository;
import com.caribexperience.service.interfaces.UsuarioService;
import com.caribexperience.web.dto.usuario.UsuarioRegistroRequest;
import com.caribexperience.web.dto.usuario.UsuarioResponse;
import com.caribexperience.web.mapper.UsuarioMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

    @Override
    @Transactional
    public UsuarioResponse registrar(UsuarioRegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new EmailYaRegistradoException(request.email());
        }

        Rol rol = rolRepository.findByNombre(request.rol())
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol no encontrado: " + request.rol()));

        Usuario usuario = Usuario.builder()
                .nombre(request.nombre())
                .apellido(request.apellido())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .rol(rol)
                .activo(true)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        return usuarioMapper.toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id: " + id));
        return usuarioMapper.toResponse(usuario);
    }
}
