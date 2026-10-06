-- V2: Datos semilla de las tablas catalogo

INSERT INTO roles (nombre) VALUES
    ('GUIA'),
    ('VIAJERO'),
    ('ADMIN');

INSERT INTO ciudades (nombre, departamento) VALUES
    ('Cartagena',   'Bolivar'),
    ('Santa Marta', 'Magdalena'),
    ('Barranquilla','Atlantico'),
    ('Riohacha',    'La Guajira'),
    ('Monteria',    'Cordoba'),
    ('Sincelejo',   'Sucre'),
    ('Valledupar',  'Cesar');

INSERT INTO categorias (nombre, descripcion) VALUES
    ('Aventura',     'Actividades de adrenalina y exploracion al aire libre'),
    ('Cultural',     'Recorridos historicos, patrimoniales y de tradicion local'),
    ('Gastronomica', 'Experiencias centradas en comida y bebida tipica'),
    ('Ecoturismo',   'Actividades en contacto con la naturaleza y ecosistemas'),
    ('Nautica',      'Actividades en el mar, playas, rios o cienagas'),
    ('Nocturna',     'Vida nocturna, musica y entretenimiento');

INSERT INTO estados_experiencia (nombre) VALUES
    ('ACTIVA'),
    ('PAUSADA'),
    ('FINALIZADA'),
    ('CANCELADA');

INSERT INTO estados_reserva (nombre) VALUES
    ('PENDIENTE'),
    ('CONFIRMADA'),
    ('CANCELADA');
