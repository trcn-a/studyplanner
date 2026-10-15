export interface Availability { dayOfWeek: number; start: string; end: string }
export interface PlanningSettings { habitAware: boolean; zoneId: string; autoReplan: boolean; horizonDays: number; sessionMinutes: number; breakMinutes: number; availability: Availability[] }
export interface ScheduleBlock { id: string; taskId: string | null; title: string; start: string; end: string; source: 'AUTO' | 'EXTERNAL' | 'MANUAL' | 'GOOGLE' | 'RUNNING' | 'COMPLETED' | 'PINNED' }
export interface PlanningIssue { taskId: string; title: string; reason: string; remainingMinutes: number }
export interface PlanningState { habits: { timerSessions: number; activeDays: number; subjectProfiles: number; ready: boolean }; settings: PlanningSettings; sessions: ScheduleBlock[]; externalEvents: ScheduleBlock[]; issues: PlanningIssue[] }

export interface PlanningActions {
    refresh: () => Promise<void>;
    moveSession: (id: string, start: string, manual: boolean) => Promise<boolean>;
    saveSettings: (settings: PlanningSettings) => Promise<boolean>;
    generate: () => Promise<boolean>;
    importCalendar: (content: string) => Promise<boolean>;
    removeEvent: (id: string) => Promise<boolean>;
}
