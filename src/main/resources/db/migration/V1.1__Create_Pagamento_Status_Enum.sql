CREATE TYPE pagamento_status AS ENUM (
    'PENDENTE',
    'PAGO',
    'CANCELADO',
    'ESTORNADO'
    );

-- Remove o DEFAULT antigo (VARCHAR)
ALTER TABLE pagamento
    ALTER COLUMN status DROP DEFAULT;

-- Converte a coluna VARCHAR para o ENUM
ALTER TABLE pagamento
    ALTER COLUMN status TYPE pagamento_status
        USING status::pagamento_status;

-- Cria novamente o DEFAULT, agora como ENUM
ALTER TABLE pagamento
    ALTER COLUMN status SET DEFAULT 'PENDENTE'::pagamento_status;

