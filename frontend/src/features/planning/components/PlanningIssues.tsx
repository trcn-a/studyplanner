import type { PlanningIssue } from '../model/types';
export function PlanningIssues({ issues }: { issues: PlanningIssue[] }) {
  return !issues.length ? null : <details className="planning-issues"><summary>Потребують уваги · {issues.length}</summary>{issues.map(i=><p key={i.taskId}><strong>{i.title}</strong> — {i.reason}{i.remainingMinutes>0?` (ще ${i.remainingMinutes} хв)`:''}</p>)}</details>;
}
