CREATE TABLE app_users (
 id UUID PRIMARY KEY, email VARCHAR(254) NOT NULL UNIQUE, display_name VARCHAR(100) NOT NULL,
 password_hash VARCHAR(100) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE,
 reminders_enabled BOOLEAN NOT NULL DEFAULT TRUE, reminder_minutes INTEGER NOT NULL DEFAULT 15 CHECK(reminder_minutes BETWEEN 5 AND 1440),
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO app_users(id,email,display_name,password_hash,enabled) VALUES('00000000-0000-0000-0000-000000000001','legacy@local.invalid','Попередні дані','!',FALSE);
CREATE TABLE account_setup_lock(id INTEGER PRIMARY KEY);
INSERT INTO account_setup_lock VALUES(1);
ALTER TABLE subjects ADD COLUMN user_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES app_users(id);
CREATE INDEX idx_subjects_user ON subjects(user_id);
ALTER TABLE planning_settings RENAME TO planning_settings_legacy;
CREATE TABLE planning_settings(user_id UUID PRIMARY KEY REFERENCES app_users(id),id INTEGER NOT NULL DEFAULT 1, zone_id VARCHAR(80) NOT NULL, auto_replan BOOLEAN NOT NULL, horizon_days INTEGER NOT NULL CHECK(horizon_days BETWEEN 1 AND 90), session_minutes INTEGER NOT NULL CHECK(session_minutes BETWEEN 15 AND 240), fingerprint VARCHAR(64),habit_aware BOOLEAN NOT NULL);
INSERT INTO planning_settings(user_id,id,zone_id,auto_replan,horizon_days,session_minutes,fingerprint,habit_aware) SELECT '00000000-0000-0000-0000-000000000001',id,zone_id,auto_replan,horizon_days,session_minutes,fingerprint,habit_aware FROM planning_settings_legacy;
DROP TABLE planning_settings_legacy;
ALTER TABLE google_calendar_connection RENAME TO google_calendar_connection_legacy;
CREATE TABLE google_calendar_connection(user_id UUID PRIMARY KEY REFERENCES app_users(id),id INTEGER NOT NULL DEFAULT 1, refresh_token TEXT NOT NULL,revision UUID NOT NULL,last_sync TIMESTAMP WITH TIME ZONE,error VARCHAR(300));
INSERT INTO google_calendar_connection(user_id,id,refresh_token,revision,last_sync,error) SELECT '00000000-0000-0000-0000-000000000001',id,refresh_token,revision,last_sync,error FROM google_calendar_connection_legacy;
DROP TABLE google_calendar_connection_legacy;
ALTER TABLE calendar_events RENAME TO calendar_events_legacy;
CREATE TABLE calendar_events(id UUID PRIMARY KEY,external_uid VARCHAR(500) NOT NULL,title VARCHAR(500) NOT NULL,starts_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,ends_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,user_id UUID NOT NULL REFERENCES app_users(id),UNIQUE(user_id,external_uid),CHECK(ends_at>starts_at));
INSERT INTO calendar_events SELECT id,external_uid,title,starts_at,ends_at,'00000000-0000-0000-0000-000000000001' FROM calendar_events_legacy;
DROP TABLE calendar_events_legacy;
ALTER TABLE google_calendar_events RENAME TO google_calendar_events_legacy;
CREATE TABLE google_calendar_events(id UUID NOT NULL,title VARCHAR(500) NOT NULL,starts_at TIMESTAMP WITH TIME ZONE NOT NULL,ends_at TIMESTAMP WITH TIME ZONE NOT NULL,user_id UUID NOT NULL REFERENCES app_users(id),PRIMARY KEY(user_id,id),CHECK(ends_at>starts_at));
INSERT INTO google_calendar_events SELECT id,title,starts_at,ends_at,'00000000-0000-0000-0000-000000000001' FROM google_calendar_events_legacy;
DROP TABLE google_calendar_events_legacy;
ALTER TABLE study_availability ADD COLUMN user_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES app_users(id);
CREATE INDEX idx_study_availability_user ON study_availability(user_id);
ALTER TABLE study_sessions ADD COLUMN user_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES app_users(id);
CREATE INDEX idx_study_sessions_user ON study_sessions(user_id);
ALTER TABLE planning_issues ADD COLUMN user_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES app_users(id);
CREATE INDEX idx_planning_issues_user ON planning_issues(user_id);
ALTER TABLE completion_rewards ADD COLUMN user_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES app_users(id);
CREATE INDEX idx_completion_rewards_user ON completion_rewards(user_id);
