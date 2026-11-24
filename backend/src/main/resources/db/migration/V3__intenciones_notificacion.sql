CREATE TABLE origen_notificacion (
    id uuid PRIMARY KEY,
    turno_id uuid NOT NULL REFERENCES turno(id),
    revision integer NOT NULL CHECK (revision >= 0),
    clase text NOT NULL CHECK (clase IN ('RESERVA', 'CAMBIO')),
    destinatario text NOT NULL CHECK (btrim(destinatario) <> ''),
    ocurrido_en timestamptz NOT NULL,
    nuevo_inicio timestamptz,
    credencial_id uuid REFERENCES credencial_gestion(id)
);

CREATE TABLE intencion_notificacion (
    id uuid PRIMARY KEY,
    evento_id uuid NOT NULL,
    turno_id uuid NOT NULL REFERENCES turno(id),
    revision integer NOT NULL CHECK (revision >= 0),
    tipo text NOT NULL CHECK (tipo IN ('CONFIRMACION', 'CAMBIO', 'RECORDATORIO')),
    destinatario text NOT NULL CHECK (btrim(destinatario) <> ''),
    credencial_id uuid REFERENCES credencial_gestion(id),
    programada_en timestamptz NOT NULL,
    vence_en timestamptz NOT NULL,
    despacho_id uuid NOT NULL,
    elegible boolean NOT NULL DEFAULT true,
    recuperacion boolean NOT NULL DEFAULT false,
    UNIQUE (evento_id, tipo),
    CHECK (vence_en > programada_en)
);

CREATE INDEX intencion_turno_idx ON intencion_notificacion (turno_id);
CREATE INDEX intencion_despacho_idx ON intencion_notificacion (despacho_id);

CREATE TABLE intento_notificacion (
    id uuid PRIMARY KEY,
    despacho_id uuid NOT NULL,
    turno_id uuid NOT NULL REFERENCES turno(id),
    revision integer NOT NULL CHECK (revision >= 0),
    destinatario text NOT NULL CHECK (btrim(destinatario) <> ''),
    credencial_id uuid REFERENCES credencial_gestion(id),
    autorizado_en timestamptz NOT NULL,
    inicio_turno timestamptz NOT NULL,
    fin_turno timestamptz NOT NULL,
    estado_turno text NOT NULL CHECK (estado_turno IN ('CONFIRMADO', 'CANCELADO')),
    tipos text[] NOT NULL CHECK (cardinality(tipos) > 0),
    recuperacion boolean NOT NULL,
    resultado text CHECK (resultado IN ('ACEPTADA_SMTP', 'TRANSITORIO', 'PERMANENTE', 'INCIERTO')),
    terminado_en timestamptz,
    CHECK ((resultado IS NULL) = (terminado_en IS NULL))
);

CREATE INDEX intento_despacho_idx ON intento_notificacion (despacho_id);
CREATE UNIQUE INDEX intento_en_curso_idx ON intento_notificacion (despacho_id) WHERE resultado IS NULL;
