CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE consultorio (
    id uuid PRIMARY KEY,
    nombre_visible text NOT NULL CHECK (btrim(nombre_visible) <> ''),
    zona_horaria text NOT NULL CHECK (btrim(zona_horaria) <> ''),
    unico boolean NOT NULL DEFAULT true UNIQUE CHECK (unico)
);

CREATE TABLE odontologo (
    id uuid PRIMARY KEY,
    consultorio_id uuid NOT NULL UNIQUE REFERENCES consultorio (id),
    nombre_visible text NOT NULL CHECK (btrim(nombre_visible) <> '')
);

CREATE TABLE agenda (
    id uuid PRIMARY KEY,
    odontologo_id uuid NOT NULL UNIQUE REFERENCES odontologo (id)
);

CREATE TABLE motivo_consulta (
    id uuid PRIMARY KEY,
    agenda_id uuid NOT NULL REFERENCES agenda (id),
    nombre text NOT NULL CHECK (btrim(nombre) <> ''),
    duracion_minutos integer NOT NULL CHECK (duracion_minutos > 0),
    UNIQUE (id, agenda_id)
);

CREATE INDEX motivo_consulta_agenda_idx ON motivo_consulta (agenda_id);

CREATE TABLE franja_semanal (
    id uuid PRIMARY KEY,
    agenda_id uuid NOT NULL REFERENCES agenda (id),
    dia_semana integer NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),
    hora_inicio time NOT NULL,
    hora_fin time NOT NULL,
    tipo text NOT NULL CHECK (tipo IN ('ATENCION', 'PAUSA')),
    CHECK (hora_inicio < hora_fin AND hora_fin < time '24:00')
);

CREATE INDEX franja_semanal_agenda_dia_inicio_idx ON franja_semanal (agenda_id, dia_semana, hora_inicio);

CREATE TABLE bloqueo_agenda (
    id uuid PRIMARY KEY,
    agenda_id uuid NOT NULL REFERENCES agenda (id),
    inicio timestamptz NOT NULL,
    fin timestamptz NOT NULL,
    descripcion text,
    CHECK (isfinite(inicio) AND isfinite(fin) AND inicio < fin)
);

CREATE INDEX bloqueo_agenda_agenda_inicio_idx ON bloqueo_agenda (agenda_id, inicio);

CREATE TABLE paciente (
    id uuid PRIMARY KEY,
    consultorio_id uuid NOT NULL REFERENCES consultorio (id),
    nombre text NOT NULL CHECK (btrim(nombre) <> ''),
    apellido text NOT NULL CHECK (btrim(apellido) <> ''),
    email text NOT NULL CHECK (btrim(email) <> ''),
    telefono text NOT NULL CHECK (btrim(telefono) <> ''),
    dni text NOT NULL CHECK (btrim(dni) <> '')
);

CREATE INDEX paciente_consultorio_idx ON paciente (consultorio_id);

CREATE TABLE turno (
    id uuid PRIMARY KEY,
    agenda_id uuid NOT NULL REFERENCES agenda (id),
    paciente_id uuid NOT NULL REFERENCES paciente (id),
    motivo_id uuid NOT NULL,
    inicio timestamptz NOT NULL,
    fin timestamptz NOT NULL,
    duracion_reservada_minutos integer NOT NULL CHECK (duracion_reservada_minutos > 0),
    estado text NOT NULL CHECK (estado IN ('CONFIRMADO', 'CANCELADO')),
    revision integer NOT NULL DEFAULT 0 CHECK (revision >= 0),
    creado_en timestamptz NOT NULL CHECK (isfinite(creado_en)),
    cancelado_en timestamptz CHECK (isfinite(cancelado_en)),
    FOREIGN KEY (motivo_id, agenda_id) REFERENCES motivo_consulta (id, agenda_id),
    CHECK (isfinite(inicio) AND isfinite(fin) AND inicio < fin),
    CHECK (fin - inicio = make_interval(mins => duracion_reservada_minutos)),
    CHECK ((estado = 'CANCELADO') = (cancelado_en IS NOT NULL)),
    CONSTRAINT turno_confirmado_sin_solapamiento EXCLUDE USING gist (
        agenda_id WITH =, tstzrange(inicio, fin, '[)') WITH &&
    ) WHERE (estado = 'CONFIRMADO')
);

CREATE INDEX turno_agenda_inicio_idx ON turno (agenda_id, inicio);
CREATE INDEX turno_paciente_idx ON turno (paciente_id);
CREATE INDEX turno_motivo_agenda_idx ON turno (motivo_id, agenda_id);
