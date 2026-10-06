-- V1: Tablas catalogo (roles, ciudades, categorias, estados)
-- Estas tablas existen para evitar "magic strings"/ENUM embebidos en el codigo
-- y permitir agregar valores nuevos sin migrar el esquema.

CREATE TABLE roles (
    id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(20) NOT NULL,
    CONSTRAINT uk_roles_nombre UNIQUE (nombre)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE ciudades (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL,
    departamento VARCHAR(100) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE categorias (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE estados_experiencia (
    id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(20) NOT NULL,
    CONSTRAINT uk_estados_experiencia_nombre UNIQUE (nombre)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE estados_reserva (
    id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(20) NOT NULL,
    CONSTRAINT uk_estados_reserva_nombre UNIQUE (nombre)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
