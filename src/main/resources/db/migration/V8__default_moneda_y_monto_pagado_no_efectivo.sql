ALTER TABLE ticket ALTER COLUMN tipo_moneda SET DEFAULT 'UYU';

UPDATE ticket
SET tipo_moneda = 'UYU'
WHERE tipo_moneda IS NULL;

UPDATE ticket
SET monto_pagado = monto_total
WHERE forma_de_pago IN ('TARJETA', 'TRANSFERENCIA')
  AND monto_pagado IS NULL;