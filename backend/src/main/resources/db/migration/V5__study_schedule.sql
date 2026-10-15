CREATE TABLE planning_settings (
 id INTEGER PRIMARY KEY CHECK(id=1), zone_id VARCHAR(80) NOT NULL,
 auto_replan BOOLEAN NOT NULL DEFAULT FALSE, horizon_days INTEGER NOT NULL CHECK(horizon_days BETWEEN 1 AND 90),
 session_minutes INTEGER NOT NULL CHECK(session_minutes BETWEEN 15 AND 240), fingerprint VARCHAR(64)
);
INSERT INTO planning_settings(id,zone_id,auto_replan,horizon_days,session_minutes) VALUES(1,'Europe/Kyiv',FALSE,14,60);
CREATE TABLE study_availability (
 id UUID PRIMARY KEY, day_of_week INTEGER NOT NULL CHECK(day_of_week BETWEEN 1 AND 7),
 start_time TIME NOT NULL, end_time TIME NOT NULL, CHECK(end_time > start_time)
);
CREATE TABLE study_sessions (
 id UUID PRIMARY KEY, task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
 starts_at TIMESTAMP WITHOUT TIME ZONE NOT NULL, ends_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
 CHECK(ends_at > starts_at)
);
CREATE INDEX idx_sessions_start ON study_sessions(starts_at);
CREATE TABLE calendar_events (
 id UUID PRIMARY KEY, external_uid VARCHAR(500) NOT NULL UNIQUE, title VARCHAR(500) NOT NULL,
 starts_at TIMESTAMP WITHOUT TIME ZONE NOT NULL, ends_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
 CHECK(ends_at > starts_at)
);
CREATE TABLE planning_issues (
 task_id UUID PRIMARY KEY REFERENCES tasks(id) ON DELETE CASCADE,
 title VARCHAR(200) NOT NULL, reason VARCHAR(500) NOT NULL, remaining_minutes INTEGER NOT NULL
);
