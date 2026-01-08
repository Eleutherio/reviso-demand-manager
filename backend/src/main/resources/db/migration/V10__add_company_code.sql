-- V10 - Add company_code with business-friendly format
ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS company_code VARCHAR(32);

WITH normalized AS (
    SELECT
        id,
        upper(regexp_replace(coalesce(name, ''), '[^A-Z0-9]', '', 'g')) AS name_norm,
        upper(regexp_replace(coalesce(segment, ''), '[^A-Z0-9]', '', 'g')) AS segment_norm,
        company_type
    FROM companies
),
base_codes AS (
    SELECT
        id,
        substring(name_norm || repeat('X', 4), 1, 4) AS name_part,
        CASE WHEN company_type = 'CLIENT' THEN 'CL' ELSE 'AG' END AS type_part,
        substring(segment_norm || repeat('X', 3), 1, 3) AS segment_part
    FROM normalized
),
composed AS (
    SELECT
        id,
        name_part || '-' || type_part || '-' || segment_part AS base_code
    FROM base_codes
),
ranked AS (
    SELECT
        id,
        base_code,
        row_number() OVER (PARTITION BY base_code ORDER BY id) AS rn
    FROM composed
)
UPDATE companies c
SET company_code = CASE
    WHEN r.rn = 1 THEN r.base_code
    ELSE r.base_code || '-' || lpad((r.rn - 1)::text, 2, '0')
END
FROM ranked r
WHERE c.id = r.id
  AND c.company_code IS NULL;

ALTER TABLE companies
    ALTER COLUMN company_code SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_companies_company_code_unique
    ON companies (company_code);
