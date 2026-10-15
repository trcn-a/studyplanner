CREATE TABLE google_calendar_connection (
 id INTEGER PRIMARY KEY CHECK(id=1), refresh_token TEXT NOT NULL,
 revision UUID NOT NULL, last_sync TIMESTAMP WITH TIME ZONE, error VARCHAR(300)
);
CREATE TABLE google_calendar_events (
 id UUID PRIMARY KEY, title VARCHAR(500) NOT NULL,
 starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
 ends_at TIMESTAMP WITH TIME ZONE NOT NULL CHECK(ends_at>starts_at)
);
