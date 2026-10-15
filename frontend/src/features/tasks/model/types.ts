export type Status = 'TODO' | 'IN_PROGRESS' | 'DONE';
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH';
export type TaskType = 'LABORATORY' | 'PRACTICE' | 'NOTES' | 'EXAM_PREPARATION' | 'OTHER';
export type Difficulty = 'LOW' | 'MEDIUM' | 'HIGH';
export interface Task {
    id: string;
    subjectId: string;
    parentTaskId: string | null;
    title: string;
    description: string;
    status: Status;
    priority: Priority;
    dueDate: string | null;
    scheduledStart: string | null;
    scheduledEnd: string | null;
    estimatedMinutes: number | null;
    taskType?: TaskType | null;
    difficulty?: Difficulty | null;
    planningTimeSource?: 'USER_ESTIMATE' | 'PREDICTION';
    actualMinutes: number;
    timerStartedAt: string | null;
}
export type TaskInput = Omit<Task, 'id' | 'subjectId' | 'parentTaskId' | 'actualMinutes' | 'timerStartedAt'>;
export interface TaskEditor {
    subjectId: string;
    id?: string;
    parentId?: string;
    input: TaskInput;
}
export interface TimeEntry {
    id: string;
    taskId: string;
    startedAt: string;
    endedAt: string | null;
    minutes: number | null;
    source: 'TIMER' | 'MANUAL';
}

export interface TaskActions {
    onStatus?: (task: Task, status: Status) => void;
    onToggle: (task: Task) => void;
    onEdit: (task: Task) => void;
    onAddChild: (id: string) => void;
    onDelete: (task: Task) => void;
    onTimer: (task: Task) => void;
    onLogTime: (task: Task) => void;
}
