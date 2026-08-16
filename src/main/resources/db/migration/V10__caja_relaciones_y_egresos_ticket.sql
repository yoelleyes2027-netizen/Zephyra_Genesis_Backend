ALTER TABLE ticket ADD COLUMN IF NOT EXISTS egresos_descripcion VARCHAR(255);
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS egreso BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE caja_diaria ADD COLUMN IF NOT EXISTS transferencia_calculada REAL;
ALTER TABLE caja_diaria ADD COLUMN IF NOT EXISTS usuario_id BIGINT;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'usuario' AND column_name = 'caja_diaria_id'
    ) THEN
        UPDATE caja_diaria cd
        SET usuario_id = u.id
        FROM usuario u
        WHERE u.caja_diaria_id = cd.id
          AND cd.usuario_id IS NULL;
    END IF;
END $$;

ALTER TABLE caja_diaria DROP CONSTRAINT IF EXISTS fk_caja_diaria_usuario;
ALTER TABLE caja_diaria ADD CONSTRAINT fk_caja_diaria_usuario
    FOREIGN KEY (usuario_id) REFERENCES usuario(id);

DROP INDEX IF EXISTS ux_caja_diaria_usuario_id;
CREATE UNIQUE INDEX IF NOT EXISTS ux_caja_diaria_usuario_id
    ON caja_diaria(usuario_id)
    WHERE usuario_id IS NOT NULL;

UPDATE caja_global
SET fecha_inicio = CURRENT_DATE
WHERE fecha_inicio IS NULL;

ALTER TABLE caja_global ALTER COLUMN fecha_inicio SET NOT NULL;
ALTER TABLE caja_global ALTER COLUMN total_ingresos DROP NOT NULL;
ALTER TABLE caja_global ALTER COLUMN total_egresos DROP NOT NULL;
ALTER TABLE caja_global ALTER COLUMN diferencia DROP NOT NULL;
ALTER TABLE caja_global ALTER COLUMN diferencia_pos DROP NOT NULL;
ALTER TABLE caja_global ALTER COLUMN diferencia_efectivo DROP NOT NULL;
ALTER TABLE caja_global ALTER COLUMN pos_calculado DROP NOT NULL;
ALTER TABLE caja_global ALTER COLUMN pos_declarado DROP NOT NULL;
ALTER TABLE caja_global ALTER COLUMN efectivo_calculado DROP NOT NULL;
ALTER TABLE caja_global ALTER COLUMN efectivo_declarado DROP NOT NULL;