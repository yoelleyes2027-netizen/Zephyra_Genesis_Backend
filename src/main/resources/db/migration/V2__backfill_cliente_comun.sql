INSERT INTO cliente_comun (id)
SELECT c.id
FROM cliente c
LEFT JOIN empresa e ON e.id = c.id
LEFT JOIN cliente_comun cc ON cc.id = c.id
WHERE e.id IS NULL
  AND cc.id IS NULL;
