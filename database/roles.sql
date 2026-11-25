BEGIN;

CREATE TABLE IF NOT EXISTS public.rol (
    codigo VARCHAR(50) PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);

INSERT INTO public.rol (codigo, nombre) VALUES
    ('VOLUNTARIO', 'Voluntario'),
    ('ORGANIZACION', 'Organización'),
    ('ADMIN', 'Administrador')
ON CONFLICT (codigo) DO NOTHING;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.usuario'::regclass AND conname = 'fk_usuario_rol'
    ) THEN
        ALTER TABLE public.usuario
            ADD CONSTRAINT fk_usuario_rol FOREIGN KEY (rol) REFERENCES public.rol (codigo);
    END IF;
END $$;

COMMIT;
