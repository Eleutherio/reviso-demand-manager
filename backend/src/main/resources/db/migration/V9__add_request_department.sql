-- Adiciona disciplina/departamento responsável pela execução da demanda
-- Mantém compatibilidade usando IF NOT EXISTS

ALTER TABLE requests
    ADD COLUMN IF NOT EXISTS department VARCHAR(30);

-- Backfill para ambientes já existentes
UPDATE requests
SET department = 'DESIGN'
WHERE department IS NULL OR department = '';

ALTER TABLE requests
    ALTER COLUMN department SET NOT NULL;

-- Índice para listagens multi-tenant por disciplina (quando aplicável)
CREATE INDEX IF NOT EXISTS idx_requests_company_department_created_at_desc
    ON requests (company_id, department, created_at DESC);
