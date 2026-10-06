package com.caribexperience.service.impl;

import com.caribexperience.common.EstadoExperienciaNombre;
import com.caribexperience.common.RolNombre;
import com.caribexperience.domain.*;
import com.caribexperience.exception.OperacionNoAutorizadaException;
import com.caribexperience.exception.RecursoNoEncontradoException;
import com.caribexperience.repository.*;
import com.caribexperience.service.interfaces.ExperienciaService;
import com.caribexperience.web.dto.experiencia.ExperienciaFiltro;
import com.caribexperience.web.dto.experiencia.ExperienciaRequest;
import com.caribexperience.web.dto.experiencia.ExperienciaResponse;
import com.caribexperience.web.mapper.ExperienciaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExperienciaServiceImpl implements ExperienciaService {

    private final ExperienciaRepository experienciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CiudadRepository ciudadRepository;
    private final CategoriaRepository categoriaRepository;
    private final EstadoExperienciaRepository estadoExperienciaRepository;
    private final ExperienciaMapper experienciaMapper;

    @Override
    @Transactional
    @PreAuthorize("hasRole('GUIA')")
    public ExperienciaResponse crear(Long guiaId, ExperienciaRequest request) {
        Usuario guia = obtenerGuiaValidado(guiaId);
        Ciudad ciudad = obtenerCiudad(request.ciudadId());
        Categoria categoria = obtenerCategoria(request.categoriaId());
        EstadoExperiencia activa = obtenerEstado(EstadoExperienciaNombre.ACTIVA);

        Experiencia experiencia = Experiencia.builder()
                .guia(guia)
                .ciudad(ciudad)
                .categoria(categoria)
                .estado(activa)
                .titulo(request.titulo())
                .descripcion(request.descripcion())
                .precio(request.precio())
                .cupoMax(request.cupoMax())
                // Al crear, el cupo disponible siempre inicia igual al cupo maximo.
                .cupoDisponible(request.cupoMax())
                .fecha(request.fecha())
                .hora(request.hora())
                .fotoUrl(request.fotoUrl())
                .build();

        return experienciaMapper.toResponse(experienciaRepository.save(experiencia));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('GUIA')")
    public ExperienciaResponse actualizar(Long experienciaId, Long guiaId, ExperienciaRequest request) {
        Experiencia experiencia = obtenerExperiencia(experienciaId);
        validarPropiedad(experiencia, guiaId);

        Ciudad ciudad = obtenerCiudad(request.ciudadId());
        Categoria categoria = obtenerCategoria(request.categoriaId());

        // El cupo disponible se ajusta proporcionalmente si el guia cambia el
        // cupo maximo, preservando los cupos ya ocupados por reservas existentes.
        int cupoOcupado = experiencia.getCupoMax() - experiencia.getCupoDisponible();
        int nuevoCupoDisponible = Math.max(0, request.cupoMax() - cupoOcupado);

        experiencia.setCiudad(ciudad);
        experiencia.setCategoria(categoria);
        experiencia.setTitulo(request.titulo());
        experiencia.setDescripcion(request.descripcion());
        experiencia.setPrecio(request.precio());
        experiencia.setCupoMax(request.cupoMax());
        experiencia.setCupoDisponible(nuevoCupoDisponible);
        experiencia.setFecha(request.fecha());
        experiencia.setHora(request.hora());
        experiencia.setFotoUrl(request.fotoUrl());

        return experienciaMapper.toResponse(experienciaRepository.save(experiencia));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('GUIA')")
    public void cancelar(Long experienciaId, Long guiaId) {
        Experiencia experiencia = obtenerExperiencia(experienciaId);
        validarPropiedad(experiencia, guiaId);

        experiencia.setEstado(obtenerEstado(EstadoExperienciaNombre.CANCELADA));
        experienciaRepository.save(experiencia);
    }

    @Override
    @Transactional(readOnly = true)
    public ExperienciaResponse obtenerPorId(Long experienciaId) {
        return experienciaMapper.toResponse(obtenerExperiencia(experienciaId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExperienciaResponse> buscar(ExperienciaFiltro filtro, Pageable pageable) {
        return experienciaRepository.findAll(ExperienciaSpecifications.conFiltro(filtro), pageable)
                .map(experienciaMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExperienciaResponse> listarPorGuia(Long guiaId, Pageable pageable) {
        return experienciaRepository.findByGuiaId(guiaId, pageable).map(experienciaMapper::toResponse);
    }

    // ---- Helpers privados ----

    private Usuario obtenerGuiaValidado(Long guiaId) {
        Usuario guia = usuarioRepository.findById(guiaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id: " + guiaId));
        if (!RolNombre.GUIA.equals(guia.getRol().getNombre())) {
            throw new OperacionNoAutorizadaException("Solo un usuario con rol GUIA puede publicar experiencias");
        }
        return guia;
    }

    private void validarPropiedad(Experiencia experiencia, Long guiaId) {
        if (!experiencia.getGuia().getId().equals(guiaId)) {
            throw new OperacionNoAutorizadaException("No tienes permiso para modificar esta experiencia");
        }
    }

    private Experiencia obtenerExperiencia(Long id) {
        return experienciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Experiencia no encontrada con id: " + id));
    }

    private Ciudad obtenerCiudad(Long id) {
        return ciudadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ciudad no encontrada con id: " + id));
    }

    private Categoria obtenerCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoria no encontrada con id: " + id));
    }

    private EstadoExperiencia obtenerEstado(String nombre) {
        return estadoExperienciaRepository.findByNombre(nombre)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado de experiencia no encontrado: " + nombre));
    }
}
