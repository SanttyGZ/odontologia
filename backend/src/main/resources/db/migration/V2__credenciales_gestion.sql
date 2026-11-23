CREATE TABLE credencial_gestion (
    id uuid PRIMARY KEY,
    turno_id uuid NOT NULL REFERENCES turno(id),
    verificador bytea NOT NULL UNIQUE CHECK (octet_length(verificador) = 32),
    material_cifrado bytea NOT NULL CHECK (octet_length(material_cifrado) = 48),
    nonce bytea NOT NULL CHECK (octet_length(nonce) = 12),
    version_clave text NOT NULL CHECK (btrim(version_clave) <> ''),
    emitida_en timestamptz NOT NULL,
    revocada_en timestamptz,
    CHECK (revocada_en IS NULL OR revocada_en >= emitida_en)
);

CREATE UNIQUE INDEX credencial_gestion_activa_por_turno
    ON credencial_gestion (turno_id) WHERE revocada_en IS NULL;

CREATE TABLE recuperacion_enlace (
    id uuid PRIMARY KEY,
    orden bigint GENERATED ALWAYS AS IDENTITY UNIQUE,
    turno_id uuid NOT NULL REFERENCES turno(id),
    odontologo text NOT NULL CHECK (btrim(odontologo) <> ''),
    metodo text NOT NULL CHECK (metodo IN ('DOCUMENTO_PRESENCIAL', 'LLAMADA_A_NUMERO_VERIFICADO')),
    motivo text NOT NULL CHECK (length(btrim(motivo)) BETWEEN 1 AND 120),
    destinatario text NOT NULL CHECK (btrim(destinatario) <> ''),
    realizada_en timestamptz NOT NULL
);

CREATE INDEX recuperacion_enlace_turno_idx ON recuperacion_enlace (turno_id, orden DESC);
