DROP TABLE IF EXISTS detalle_ticket;

CREATE TABLE detalle_ticket (
    ticket_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    precio_unitario REAL NOT NULL,
    CONSTRAINT detalle_ticket_pkey PRIMARY KEY (ticket_id, producto_id),
    CONSTRAINT detalle_ticket_ticket_fk FOREIGN KEY (ticket_id) REFERENCES ticket (id) ON DELETE CASCADE,
    CONSTRAINT detalle_ticket_producto_fk FOREIGN KEY (producto_id) REFERENCES producto (id)
);