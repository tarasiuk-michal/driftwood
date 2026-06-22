INSERT INTO workflows (id, name, created_at)
VALUES ('trivial-workflow', 'Trivial Workflow', now());

INSERT INTO steps (id, workflow_id, name, step_order, created_at)
VALUES ('trivial-workflow-step-1', 'trivial-workflow', 'Prepare', 1, now()),
       ('trivial-workflow-step-2', 'trivial-workflow', 'Execute', 2, now());
