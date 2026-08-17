ALTER TABLE caja_global ADD COLUMN IF NOT EXISTS transferencia_calculada REAL;
ALTER TABLE caja_global ALTER COLUMN transferencia_calculada DROP NOT NULL;
ALTER TABLE caja_global ALTER COLUMN transferencia_calculada DROP DEFAULT;
