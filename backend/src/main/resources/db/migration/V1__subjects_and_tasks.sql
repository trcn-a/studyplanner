CREATE TABLE subjects (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    color VARCHAR(7) NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    subject_id UUID NOT NULL REFERENCES subjects(id),
    parent_task_id UUID REFERENCES tasks(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    priority VARCHAR(10) NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    due_date DATE,
    estimated_minutes INTEGER CHECK (estimated_minutes > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CHECK (parent_task_id IS NULL OR parent_task_id <> id)
);
CREATE INDEX idx_tasks_subject ON tasks(subject_id);
CREATE INDEX idx_tasks_parent ON tasks(parent_task_id);
CREATE INDEX idx_tasks_due ON tasks(due_date);
