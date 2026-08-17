ALTER TABLE caja_diaria ADD COLUMN IF NOT EXISTS diferencia_dolares REAL NOT NULL DEFAULT 0;
ALTER TABLE caja_diaria ADD COLUMN IF NOT EXISTS dolares_calculados REAL NOT NULL DEFAULT 0;
ALTER TABLE caja_diaria ADD COLUMN IF NOT EXISTS dolares_declarados REAL NOT NULL DEFAULT 0;

ALTER TABLE caja_global ADD COLUMN IF NOT EXISTS diferencia_dolares REAL;
ALTER TABLE caja_global ADD COLUMN IF NOT EXISTS dolares_calculados REAL;
ALTER TABLE caja_global ADD COLUMN IF NOT EXISTS dolares_declarados REAL;

ALTER TABLE caja_global ALTER COLUMN diferencia_dolares DROP NOT NULL;
ALTER TABLE caja_global ALTER COLUMN dolares_calculados DROP NOT NULL;
ALTER TABLE caja_global ALTER COLUMN dolares_declarados DROP NOT NULL;

ALTER TABLE caja_global ALTER COLUMN diferencia_dolares DROP DEFAULT;
ALTER TABLE caja_global ALTER COLUMN dolares_calculados DROP DEFAULT;
ALTER TABLE caja_global ALTER COLUMN dolares_declarados DROP DEFAULT;
