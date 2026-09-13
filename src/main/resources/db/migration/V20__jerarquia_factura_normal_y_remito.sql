CREATE TABLE IF NOT EXISTS factura_normal (
    id BIGINT PRIMARY KEY REFERENCES factura(id)
);

INSERT INTO factura_normal (id)
SELECT f.id
FROM factura f
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS remito (
    id BIGINT PRIMARY KEY REFERENCES factura(id),
    factura_origen_id BIGINT NOT NULL REFERENCES factura_normal(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_remito_factura_origen_id ON remito(factura_origen_id);

ALTER TABLE factura DROP COLUMN IF EXISTS remito;
ALTER TABLE factura DROP COLUMN IF EXISTS remito_realizado;
