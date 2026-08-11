ALTER TABLE caja_diaria ADD COLUMN IF NOT EXISTS fecha_inicio DATE;
ALTER TABLE caja_diaria ADD COLUMN IF NOT EXISTS diferencia_pos REAL NOT NULL DEFAULT 0;
ALTER TABLE caja_diaria ADD COLUMN IF NOT EXISTS diferencia_efectivo REAL NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS caja_global (
    id BIGSERIAL PRIMARY KEY,
    total_ingresos REAL NOT NULL DEFAULT 0,
    total_egresos REAL NOT NULL DEFAULT 0,
    fecha_inicio DATE,
    fecha_cierre DATE,
    diferencia REAL NOT NULL DEFAULT 0,
    diferencia_pos REAL NOT NULL DEFAULT 0,
    diferencia_efectivo REAL NOT NULL DEFAULT 0,
    pos_calculado REAL NOT NULL DEFAULT 0,
    pos_declarado REAL NOT NULL DEFAULT 0,
    efectivo_calculado INTEGER NOT NULL DEFAULT 0,
    efectivo_declarado INTEGER NOT NULL DEFAULT 0
);

ALTER TABLE caja_diaria ADD COLUMN IF NOT EXISTS caja_global_id BIGINT;
ALTER TABLE caja_diaria DROP CONSTRAINT IF EXISTS fk_caja_diaria_caja_global;
ALTER TABLE caja_diaria ADD CONSTRAINT fk_caja_diaria_caja_global
    FOREIGN KEY (caja_global_id) REFERENCES caja_global(id);