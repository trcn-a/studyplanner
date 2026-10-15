ALTER TABLE planning_settings ADD COLUMN break_minutes INTEGER NOT NULL DEFAULT 0;
ALTER TABLE planning_settings ADD CONSTRAINT planning_break_minutes_check CHECK (break_minutes BETWEEN 0 AND 120);
