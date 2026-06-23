ALTER TABLE steps ADD COLUMN max_attempts INT NOT NULL DEFAULT 5;

-- Poison workflow needs fewer retries so dead-lettering is visible quickly in the demo
UPDATE steps SET max_attempts = 3 WHERE workflow_id = 'poison-workflow';
