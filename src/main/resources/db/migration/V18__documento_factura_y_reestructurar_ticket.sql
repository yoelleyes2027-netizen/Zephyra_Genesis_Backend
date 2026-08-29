CREATE TABLE documento (
    id BIGSERIAL PRIMARY KEY,
    monto_total REAL NOT NULL,
    tipo_moneda VARCHAR(3) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL
);

INSERT INTO documento (id, monto_total, tipo_moneda, fecha_creacion)
SELECT
    t.id,
    COALESCE(t.monto_total, 0),
    COALESCE(t.tipo_moneda, 'UYU'),
    COALESCE(t.fecha_creacion, CURRENT_TIMESTAMP)
FROM ticket t;

SELECT setval(
    pg_get_serial_sequence('documento', 'id'),
    GREATEST(COALESCE((SELECT MAX(id) FROM documento), 0) + 1, 1),
    false
);

ALTER TABLE ticket
    ADD CONSTRAINT fk_ticket_documento
    FOREIGN KEY (id) REFERENCES documento(id);

CREATE SEQUENCE ticket_nro_ticket_seq;
ALTER TABLE ticket ADD COLUMN nro_ticket INTEGER;
UPDATE ticket SET nro_ticket = nextval('ticket_nro_ticket_seq') WHERE nro_ticket IS NULL;
ALTER TABLE ticket ALTER COLUMN nro_ticket SET NOT NULL;
ALTER TABLE ticket ALTER COLUMN nro_ticket SET DEFAULT nextval('ticket_nro_ticket_seq');
ALTER TABLE ticket ADD CONSTRAINT uk_ticket_nro_ticket UNIQUE (nro_ticket);
SELECT setval(
    'ticket_nro_ticket_seq',
    GREATEST(COALESCE((SELECT MAX(nro_ticket) FROM ticket), 0) + 1, 1),
    false
);

ALTER TABLE ticket DROP COLUMN fecha_creacion;
ALTER TABLE ticket DROP COLUMN monto_total;
ALTER TABLE ticket DROP COLUMN tipo_moneda;

CREATE SEQUENCE factura_nro_factura_seq;
CREATE TABLE factura (
    id BIGINT PRIMARY KEY REFERENCES documento(id),
    fecha_emision TIMESTAMP NOT NULL,
    remito BOOLEAN NOT NULL DEFAULT FALSE,
    nro_factura INTEGER NOT NULL DEFAULT nextval('factura_nro_factura_seq') UNIQUE,
    nro_serie VARCHAR(255) NOT NULL UNIQUE,
    remito_realizado BOOLEAN NOT NULL DEFAULT FALSE,
    proveedor_id BIGINT NOT NULL REFERENCES proveedor(id),
    usuario_id BIGINT NOT NULL REFERENCES usuario(id)
);

CREATE TABLE detalle_factura (
    factura_id BIGINT NOT NULL REFERENCES factura(id),
    producto_id BIGINT NOT NULL REFERENCES producto(id),
    cantidad INTEGER NOT NULL,
    precio_compra REAL NOT NULL,
    PRIMARY KEY (factura_id, producto_id)
);
