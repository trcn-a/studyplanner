ALTER TABLE tasks ADD COLUMN task_type VARCHAR(24);
ALTER TABLE tasks ADD COLUMN difficulty VARCHAR(10);
ALTER TABLE tasks ADD COLUMN initial_task_type VARCHAR(24);
ALTER TABLE tasks ADD COLUMN initial_difficulty VARCHAR(10);
ALTER TABLE tasks ADD CONSTRAINT tasks_task_type_check CHECK (task_type IN ('LABORATORY','PRACTICE','NOTES','EXAM_PREPARATION','OTHER'));
ALTER TABLE tasks ADD CONSTRAINT tasks_difficulty_check CHECK (difficulty IN ('LOW','MEDIUM','HIGH'));
ALTER TABLE tasks ADD CONSTRAINT tasks_initial_task_type_check CHECK (initial_task_type IN ('LABORATORY','PRACTICE','NOTES','EXAM_PREPARATION','OTHER'));
ALTER TABLE tasks ADD CONSTRAINT tasks_initial_difficulty_check CHECK (initial_difficulty IN ('LOW','MEDIUM','HIGH'));
-- Existing characteristics are unknown; historical snapshots must not be inferred retrospectively.
ALTER TABLE tasks DROP COLUMN initial_description_length;
