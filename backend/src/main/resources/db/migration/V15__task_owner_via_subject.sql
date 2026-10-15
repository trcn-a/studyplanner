-- Task ownership is derived from subjects.user_id. Keep historical migrations unchanged.
-- Dependent records retain their user_id; application services validate their owner.
ALTER TABLE study_sessions DROP CONSTRAINT fk_session_task_owner;
ALTER TABLE planning_issues DROP CONSTRAINT fk_planning_issues_owner;
ALTER TABLE tasks DROP CONSTRAINT fk_task_parent_scope;
ALTER TABLE tasks DROP CONSTRAINT fk_task_subject_owner;
ALTER TABLE tasks DROP CONSTRAINT uq_task_subject_owner;
ALTER TABLE tasks DROP CONSTRAINT uq_task_owner;
ALTER TABLE tasks ADD CONSTRAINT uq_task_subject UNIQUE(id,subject_id);
ALTER TABLE tasks ADD CONSTRAINT fk_task_parent_subject FOREIGN KEY(parent_task_id,subject_id) REFERENCES tasks(id,subject_id) ON DELETE CASCADE;
ALTER TABLE study_sessions ADD CONSTRAINT fk_session_task FOREIGN KEY(task_id) REFERENCES tasks(id) ON DELETE CASCADE;
ALTER TABLE tasks DROP COLUMN user_id;
