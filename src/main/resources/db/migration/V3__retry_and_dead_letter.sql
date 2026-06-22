ALTER TABLE step_executions ADD COLUMN next_retry_at TIMESTAMP;
ALTER TABLE step_executions ADD COLUMN max_attempts INT NOT NULL DEFAULT 3;

CREATE INDEX idx_step_executions_retry ON step_executions (next_retry_at);

CREATE TABLE dead_letter_entries (
    id                   UUID         NOT NULL,
    workflow_instance_id UUID         NOT NULL,
    step_id              VARCHAR(255) NOT NULL,
    final_attempt_count  INT          NOT NULL,
    error_message        VARCHAR(2000),
    created_at           TIMESTAMP    NOT NULL,
    CONSTRAINT pk_dead_letter_entries PRIMARY KEY (id),
    CONSTRAINT fk_dle_instance FOREIGN KEY (workflow_instance_id) REFERENCES workflow_instances (id)
);

INSERT INTO workflows (id, name, created_at)
VALUES ('flaky-workflow', 'Flaky Workflow', now());

INSERT INTO steps (id, workflow_id, name, step_order, created_at)
VALUES ('flaky-workflow-step-1', 'flaky-workflow', 'Prepare', 1, now()),
       ('flaky-workflow-step-2', 'flaky-workflow', 'Flaky Execute', 2, now());
