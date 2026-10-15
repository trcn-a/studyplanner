-- Monday through Friday by default; bit 0 is Monday, bit 6 is Sunday.
ALTER TABLE app_users ADD COLUMN study_days INTEGER NOT NULL DEFAULT 31 CHECK (study_days BETWEEN 1 AND 127);
