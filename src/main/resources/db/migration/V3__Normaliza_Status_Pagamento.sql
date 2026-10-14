ALTER TABLE pagamento
    ALTER COLUMN status DROP DEFAULT;

ALTER TABLE pagamento
    ALTER COLUMN status TYPE VARCHAR(30)
        USING status::text;

ALTER TABLE pagamento
    ALTER COLUMN status SET DEFAULT 'PENDENTE';

DROP TYPE pagamento_status;
