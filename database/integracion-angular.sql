BEGIN;
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE campania ADD COLUMN IF NOT EXISTS categoria VARCHAR(10) NOT NULL DEFAULT 'Social';
ALTER TABLE campania ADD COLUMN IF NOT EXISTS estado VARCHAR(10) NOT NULL DEFAULT 'ACTIVA';
ALTER TABLE reporte_impacto ADD COLUMN IF NOT EXISTS materiales_kg NUMERIC NOT NULL DEFAULT 0;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'reporte_impacto'
                 AND column_name = 'horas_voluntariado' AND data_type = 'integer') THEN
        ALTER TABLE reporte_impacto ALTER COLUMN horas_voluntariado TYPE NUMERIC;
    END IF;
    IF EXISTS (SELECT 1 FROM inscripcion GROUP BY campania_id, voluntario_id HAVING count(*) > 1)
       OR EXISTS (SELECT 1 FROM reporte_impacto GROUP BY campania_id HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'Hay inscripciones o reportes duplicados. Resuélvelos antes de migrar.';
    END IF;
END $$;
UPDATE inscripcion SET estado = 'INSCRITO' WHERE estado IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_inscripcion_campania_voluntario
    ON inscripcion (campania_id, voluntario_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_reporte_impacto_campania
    ON reporte_impacto (campania_id);
COMMIT;
