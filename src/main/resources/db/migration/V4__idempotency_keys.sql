CREATE TABLE idempotency_keys (
    idempotency_key      VARCHAR(255) NOT NULL,
    workflow_instance_id UUID         NOT NULL,
    created_at           TIMESTAMP    NOT NULL,
    CONSTRAINT pk_idempotency_keys PRIMARY KEY (idempotency_key),
    CONSTRAINT fk_idempotency_keys_instance FOREIGN KEY (workflow_instance_id) REFERENCES workflow_instances (id)
);
