-- V6: Indices adicionales para patrones de consulta frecuentes

-- Acelera el calculo de cupo ocupado por experiencia (solo reservas activas)
CREATE INDEX idx_reservas_experiencia_estado ON reservas (experiencia_id, estado_id);

-- Acelera el filtro de busqueda publica por categoria
CREATE INDEX idx_experiencias_categoria_id ON experiencias (categoria_id);
