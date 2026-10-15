ALTER TABLE tasks ADD COLUMN scheduled_start TIMESTAMP;
ALTER TABLE tasks ADD COLUMN scheduled_end TIMESTAMP;
ALTER TABLE tasks ADD CONSTRAINT chk_task_schedule CHECK (
 (scheduled_start IS NULL AND scheduled_end IS NULL) OR
 (scheduled_start IS NOT NULL AND scheduled_end IS NOT NULL AND scheduled_end > scheduled_start
 AND CAST(scheduled_start AS DATE) = CAST(scheduled_end AS DATE))
);
CREATE INDEX idx_tasks_schedule ON tasks(scheduled_start);
