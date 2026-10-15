ALTER TABLE tasks ADD COLUMN prediction_snapshot BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tasks ADD COLUMN initial_estimated_minutes INTEGER;
ALTER TABLE tasks ADD COLUMN initial_description_length INTEGER;
ALTER TABLE tasks ADD COLUMN initial_priority VARCHAR(10);
-- Existing records are retained, but their original features cannot be reconstructed.
CREATE TABLE timer_guard (id INTEGER PRIMARY KEY CHECK (id = 1));
INSERT INTO timer_guard(id) VALUES (1);
