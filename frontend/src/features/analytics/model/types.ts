export interface AnalyticsSummary {
    completedTasks: number;
    openTasks: number;
    actualMinutes: number;
    activeDays: number;
    periodDays: number;
    averageMinutesPerActiveDay: number;
    sessions: number;
    averageSessionMinutes: number;
    currentStreak: number;
    longestStreak: number;
}
export interface AnalyticsDeadlines {
    due: number;
    onTime: number;
    late: number;
    overdueOpen: number;
    upcoming: number;
    unknownCompletion: number;
    onTimePercent: number | null;
}
export interface AnalyticsPlan {
    plannedMinutes: number;
    fulfilledMinutes: number;
    missedMinutes: number;
    extraMinutes: number;
    futureMinutes: number;
    completionPercent: number | null;
}
export interface AnalyticsDay {
    date: string;
    actualMinutes: number;
    plannedMinutes: number;
    fulfilledMinutes: number;
    completedTasks: number;
}
export interface AnalyticsSubject {
    id: string;
    name: string;
    color: string;
    actualMinutes: number;
    completedTasks: number;
    doneTasks: number;
    totalTasks: number;
}
export interface AnalyticsWeekday {
    dayOfWeek: number;
    actualMinutes: number;
}
export interface AnalyticsTaskProgress {
    id: string;
    title: string;
    subjectId: string;
    dueDate: string | null;
    status: string;
    actualMinutes: number;
    expectedMinutes: number | null;
    percent: number | null;
}
export interface Analytics {
    from: string;
    to: string;
    summary: AnalyticsSummary;
    deadlines: AnalyticsDeadlines;
    plan: AnalyticsPlan;
    days: AnalyticsDay[];
    subjects: AnalyticsSubject[];
    weekdays: AnalyticsWeekday[];
    progress: AnalyticsTaskProgress[];
}
