DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'ticket' AND column_name = 'fechacreacion'
    ) THEN
        UPDATE ticket
        SET fecha_creacion = COALESCE(fecha_creacion, fechacreacion)
        WHERE fecha_creacion IS NULL;
        ALTER TABLE ticket DROP COLUMN IF EXISTS fechacreacion;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'ticket' AND column_name = 'formadepago'
    ) THEN
        UPDATE ticket
        SET forma_de_pago = COALESCE(forma_de_pago, formadepago)
        WHERE forma_de_pago IS NULL;
        ALTER TABLE ticket DROP COLUMN IF EXISTS formadepago;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'ticket' AND column_name = 'montototal'
    ) THEN
        UPDATE ticket
        SET monto_total = COALESCE(monto_total, montototal)
        WHERE monto_total IS NULL;
        ALTER TABLE ticket DROP COLUMN IF EXISTS montototal;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'detalle_ticket' AND column_name = 'preciounitario'
    ) THEN
        UPDATE detalle_ticket
        SET precio_unitario = COALESCE(precio_unitario, preciounitario)
        WHERE precio_unitario IS NULL;
        ALTER TABLE detalle_ticket DROP COLUMN IF EXISTS preciounitario;
    END IF;
END $$;