-- V4: Tabla experiencias

CREATE TABLE experiencias (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    guia_id          BIGINT         NOT NULL,
    ciudad_id        BIGINT         NOT NULL,
    categoria_id     BIGINT         NOT NULL,
    estado_id        BIGINT         NOT NULL,
    titulo           VARCHAR(150)   NOT NULL,
    descripcion      TEXT           NOT NULL,
    precio           DECIMAL(10, 2) NOT NULL,
    cupo_max         INT            NOT NULL,
    cupo_disponible  INT            NOT NULL,
    fecha            DATE           NOT NULL,
    hora             TIME           NOT NULL,
    foto_url         VARCHAR(500)   NULL,
    created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME       NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_experiencias_guia FOREIGN KEY (guia_id) REFERENCES usuarios (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_experiencias_ciudad FOREIGN KEY (ciudad_id) REFERENCES ciudades (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_experiencias_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_experiencias_estado FOREIGN KEY (estado_id) REFERENCES estados_experiencia (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_experiencias_precio CHECK (precio >= 0),
    CONSTRAINT chk_experiencias_cupo_max CHECK (cupo_max > 0),
    CONSTRAINT chk_experiencias_cupo_disponible CHECK (cupo_disponible >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_experiencias_guia_id ON experiencias (guia_id);
CREATE INDEX idx_experiencias_ciudad_fecha ON experiencias (ciudad_id, fecha);
CREATE INDEX idx_experiencias_estado_id ON experiencias (estado_id);
