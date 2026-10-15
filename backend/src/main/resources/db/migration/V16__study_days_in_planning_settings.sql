-- Preserve existing choices while moving the learning schedule out of the account table.
ALTER TABLE planning_settings ADD COLUMN study_days INTEGER NOT NULL DEFAULT 31 CHECK (study_days BETWEEN 1 AND 127);
UPDATE planning_settings SET study_days = (SELECT u.study_days FROM app_users u WHERE u.id=planning_settings.user_id);
ALTER TABLE app_users DROP COLUMN study_days;
