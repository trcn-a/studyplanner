CREATE TABLE time_entries (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE,
    minutes INTEGER CHECK (minutes > 0),
    source VARCHAR(10) NOT NULL CHECK (source IN ('TIMER', 'MANUAL')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CHECK ((ended_at IS NULL AND minutes IS NULL) OR (ended_at IS NOT NULL AND minutes IS NOT NULL))
);
CREATE INDEX idx_time_entries_task ON time_entries(task_id);
CREATE INDEX idx_time_entries_running ON time_entries(ended_at);
