-- V3: Tabla usuarios

CREATE TABLE usuarios (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(100)  NOT NULL,
    apellido      VARCHAR(100)  NOT NULL,
    email         VARCHAR(150)  NOT NULL,
    password_hash VARCHAR(255)  NOT NULL,
    rol_id        BIGINT        NOT NULL,
    activo        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_usuarios_email UNIQUE (email),
    CONSTRAINT fk_usuarios_rol FOREIGN KEY (rol_id) REFERENCES roles (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_usuarios_rol_id ON usuarios (rol_id);
