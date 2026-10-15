CREATE TABLE notification_settings (
 user_id UUID PRIMARY KEY REFERENCES app_users(id),
 reminders_enabled BOOLEAN NOT NULL DEFAULT TRUE,
 reminder_minutes INTEGER NOT NULL DEFAULT 15 CHECK(reminder_minutes BETWEEN 5 AND 1440)
);
INSERT INTO notification_settings(user_id,reminders_enabled,reminder_minutes)
 SELECT id,reminders_enabled,reminder_minutes FROM app_users;
ALTER TABLE app_users DROP COLUMN reminders_enabled;
ALTER TABLE app_users DROP COLUMN reminder_minutes;
