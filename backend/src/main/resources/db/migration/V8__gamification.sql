CREATE TABLE gamification_lock (id INTEGER PRIMARY KEY CHECK(id=1));
INSERT INTO gamification_lock VALUES(1);
CREATE TABLE completion_rewards (
 task_id UUID PRIMARY KEY,
 points INTEGER NOT NULL CHECK(points IN (5,10)),
 completed_at TIMESTAMP WITH TIME ZONE,
 activity_date DATE
);
-- Completed legacy tasks receive points, but their completion date is unknown.
-- No foreign key: earned rewards survive deleting a task or subject.
INSERT INTO completion_rewards(task_id,points)
SELECT id, CASE WHEN parent_task_id IS NULL THEN 10 ELSE 5 END FROM tasks WHERE status='DONE';
