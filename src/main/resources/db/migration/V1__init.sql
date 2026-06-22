CREATE TABLE workflows (
    id          VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    CONSTRAINT pk_workflows PRIMARY KEY (id)
);

CREATE TABLE steps (
    id          VARCHAR(255) NOT NULL,
    workflow_id VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    step_order  INT          NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    CONSTRAINT pk_steps PRIMARY KEY (id),
    CONSTRAINT fk_steps_workflow FOREIGN KEY (workflow_id) REFERENCES workflows (id)
);

CREATE TABLE workflow_instances (
    id          UUID         NOT NULL,
    workflow_id VARCHAR(255) NOT NULL,
    status      VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    CONSTRAINT pk_workflow_instances PRIMARY KEY (id),
    CONSTRAINT fk_workflow_instances_workflow FOREIGN KEY (workflow_id) REFERENCES workflows (id)
);

CREATE TABLE step_executions (
    id                   UUID         NOT NULL,
    workflow_instance_id UUID         NOT NULL,
    step_id              VARCHAR(255) NOT NULL,
    attempt_count        INT          NOT NULL DEFAULT 0,
    status               VARCHAR(50)  NOT NULL,
    started_at           TIMESTAMP,
    completed_at         TIMESTAMP,
    error_message        VARCHAR(2000),
    created_at           TIMESTAMP    NOT NULL,
    CONSTRAINT pk_step_executions PRIMARY KEY (id),
    CONSTRAINT fk_step_executions_instance FOREIGN KEY (workflow_instance_id) REFERENCES workflow_instances (id),
    CONSTRAINT fk_step_executions_step FOREIGN KEY (step_id) REFERENCES steps (id)
);
