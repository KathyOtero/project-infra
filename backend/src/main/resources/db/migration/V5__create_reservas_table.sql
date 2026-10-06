-- V5: Tabla reservas

CREATE TABLE reservas (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiencia_id     BIGINT   NOT NULL,
    viajero_id         BIGINT   NOT NULL,
    estado_id          BIGINT   NOT NULL,
    cantidad_personas  INT      NOT NULL,
    fecha_reserva      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_reservas_experiencia FOREIGN KEY (experiencia_id) REFERENCES experiencias (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_reservas_viajero FOREIGN KEY (viajero_id) REFERENCES usuarios (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_reservas_estado FOREIGN KEY (estado_id) REFERENCES estados_reserva (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_reservas_cantidad_personas CHECK (cantidad_personas > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_reservas_viajero_id ON reservas (viajero_id);
CREATE INDEX idx_reservas_experiencia_id ON reservas (experiencia_id);
CREATE INDEX idx_reservas_estado_id ON reservas (estado_id);
