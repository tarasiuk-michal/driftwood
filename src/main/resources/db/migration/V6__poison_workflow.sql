INSERT INTO workflows (id, name, created_at)
VALUES ('poison-workflow', 'Poison Workflow', now());

INSERT INTO steps (id, workflow_id, name, step_order, created_at)
VALUES ('poison-workflow-step-1', 'poison-workflow', 'Setup',   1, now()),
       ('poison-workflow-step-2', 'poison-workflow', 'Execute', 2, now());
