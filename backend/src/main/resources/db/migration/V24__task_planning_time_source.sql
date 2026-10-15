ALTER TABLE tasks ADD COLUMN planning_time_source VARCHAR(20) NOT NULL DEFAULT 'PREDICTION';
ALTER TABLE tasks ADD CONSTRAINT task_planning_time_source_check CHECK (planning_time_source IN ('USER_ESTIMATE', 'PREDICTION'));
