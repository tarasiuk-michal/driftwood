INSERT INTO workflows (id, name, created_at)
VALUES ('three-step-workflow', 'Three Step Workflow', now());

INSERT INTO steps (id, workflow_id, name, step_order, created_at)
VALUES ('three-step-workflow-step-1', 'three-step-workflow', 'Prepare',  1, now()),
       ('three-step-workflow-step-2', 'three-step-workflow', 'Process',  2, now()),
       ('three-step-workflow-step-3', 'three-step-workflow', 'Finalize', 3, now());
