-- Solo lectura; ejecutar en SQL Editor de Supabase con un usuario autorizado.
SELECT table_name, column_name, data_type, numeric_precision, numeric_scale,
       is_nullable, is_identity, column_default
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN ('documento', 'cotizacion', 'detalle_cotizacion', 'comprobante', 'usuario', 'auditoria', 'producto')
ORDER BY table_name, ordinal_position;

SELECT conrelid::regclass AS tabla, conname, pg_get_constraintdef(oid) AS definicion
FROM pg_constraint
WHERE conrelid IN ('public.documento'::regclass, 'public.cotizacion'::regclass,
                  'public.comprobante'::regclass, 'public.auditoria'::regclass)
ORDER BY tabla, conname;

SELECT version, description, success FROM public.flyway_schema_history ORDER BY installed_rank;

-- Cotizaciones sin registro automatico o con origen/monto/estado desajustados.
SELECT c.id, c.numero, c.total, c.estado, d.id AS documento_id,
       d.origen, d.origen_id, d.monto, d.estado AS documento_estado
FROM public.cotizacion c
LEFT JOIN public.documento d ON d.tipo = 'COTIZACION' AND d.numero = c.numero
WHERE c.numero IS NOT NULL AND (d.id IS NULL OR d.origen <> 'COTIZACION'
       OR d.origen_id IS DISTINCT FROM c.id::text OR d.monto IS DISTINCT FROM c.total
       OR d.estado IS DISTINCT FROM c.estado);

-- Boletas sin documento o con registro desajustado.
SELECT c.id, c.serie, c.correlativo, c.estado, d.id AS documento_id,
       d.origen, d.origen_id, d.monto, d.estado AS documento_estado
FROM public.comprobante c
LEFT JOIN public.documento d ON d.tipo = 'BOLETA'
       AND d.numero = c.serie || '-' || c.correlativo::text
WHERE c.tipo_doc = '03' AND (d.id IS NULL OR d.origen <> 'COMPROBANTE'
       OR d.origen_id IS DISTINCT FROM c.id::text OR d.monto IS DISTINCT FROM c.mto_imp_venta
       OR d.estado IS DISTINCT FROM c.estado);

-- Trazabilidad reciente: ajustar filtro por entidad_id para la prueba realizada.
SELECT id, origen, origen_id, usuario_id, estado, monto, creado_en, actualizado_en
FROM public.documento ORDER BY id DESC LIMIT 20;
SELECT id, accion, entidad, entidad_id, usuario_id, fecha
FROM public.auditoria ORDER BY id DESC LIMIT 30;
