import { ProfilePanel } from '../../features/account/components/ProfilePanel';
import { PlanningSettingsPanel } from '../../features/planning/components/PlanningSettingsPanel';
import { NotificationPanel } from '../../features/notifications/components/NotificationPanel';
import { useAccount } from '../../features/account/model/AccountContext';
import { GamificationPanel } from '../../features/gamification/components/GamificationPanel';
import { SubjectSidebar } from '../../features/subjects/components/SubjectSidebar';
import { SubjectHeading } from '../../features/subjects/components/SubjectHeading';
import { SubjectFormDialog } from '../../features/subjects/components/SubjectFormDialog';
import { TaskStats } from '../../features/tasks/components/TaskStats';
import { TaskKanban } from '../../features/tasks/components/TaskKanban';
import { TaskPanel } from '../../features/tasks/components/TaskPanel';
import { TaskCalendar } from '../../features/calendar/components/TaskCalendar';
import { TaskFormDialog } from '../../features/tasks/components/TaskFormDialog';
import { DeleteTaskDialog } from '../../features/tasks/components/DeleteTaskDialog';
import { TimeLogDialog } from '../../features/tasks/components/TimeLogDialog';
import { PredictionContext } from '../../features/prediction/model/predictionContext';
import { AnalyticsPage } from '../../features/analytics/components/AnalyticsPage';
import { Topbar } from '../../shared/ui/Topbar';
import { Welcome } from './components/Welcome';
import { usePlanner } from './model/usePlanner';
export function PlannerPage() {
 const planner=usePlanner();
 const {account}=useAccount();
 const {subjectEditor,taskEditor,deleteTarget,timeTarget,selected,busy,error}=planner;
 const revision=JSON.stringify(planner.currentTasks);
 const titles={calendar:'Календар',kanban:'Канбан',analytics:'Аналітика',subject:'Предмети',all:'Усі завдання',profile:'Профіль'};
 const listProps={total:planner.roots.length,busy,loading:planner.loading,filteredRoots:planner.filteredRoots,children:planner.children,filters:planner.filters,actions:planner.actions,onCreate:()=>planner.openTask()};
 return <PredictionContext.Provider value={planner.predictions}><div className="app-shell">
  <SubjectSidebar allActive={planner.view==='all'} onAll={planner.showAll} profileActive={planner.view==='profile'} onProfile={planner.showProfile} kanbanActive={planner.view==='kanban'} onKanban={planner.showKanban} calendarActive={planner.view==='calendar'} onCalendar={planner.showCalendar} analyticsActive={planner.view==='analytics'} onAnalytics={planner.showAnalytics} subjects={planner.subjects} selectedId={planner.view==='subject'?planner.selectedId:''} initialLoading={planner.initialLoading} onCreate={planner.newSubject} onSelect={planner.chooseSubject}/>
  <main><Topbar title={titles[planner.view]}><NotificationPanel revision={JSON.stringify([planner.currentTasks,account.remindersEnabled,account.reminderMinutes])}/></Topbar><div className="page-content">
   {error && !subjectEditor && !taskEditor && !deleteTarget && !timeTarget && <div className="error" role="alert">{error}<button className="text-button" onClick={()=>window.location.reload()}>Оновити сторінку</button></div>}
   {planner.initialLoading ? <div className="empty">Завантаження…</div>
    : planner.view==='profile' ? <div className="profile-page"><div className="profile-overview"><ProfilePanel><PlanningSettingsPanel revision={revision}/></ProfilePanel><GamificationPanel revision={revision}/></div></div>
    : planner.view==='analytics' ? <AnalyticsPage revision={revision}/>
    : planner.view==='calendar' ? <TaskCalendar onMoved={planner.reloadTasks} subjects={planner.subjects} tasks={planner.currentTasks} busy={busy} loading={planner.loading} actions={planner.actions} onSettings={planner.showProfile} onCreate={(date,time)=>planner.openTask(undefined,undefined,date,time)}/>
    : planner.view==='kanban' ? <TaskKanban tasks={planner.currentTasks} subjects={planner.subjects} busy={busy} loading={planner.loading} filters={planner.filters} actions={planner.actions} onCreate={()=>planner.openTask()} onStatus={planner.changeStatus}/>
    : planner.view==='all' ? <><TaskStats total={planner.roots.length} done={planner.done} overdue={planner.overdue}/><TaskPanel {...listProps} title="Усі завдання" subjects={planner.subjects} canCreate={planner.subjects.length>0}/></>
    : !selected ? <Welcome onCreate={planner.newSubject}/>
    : <><SubjectHeading selected={selected} busy={busy} onEdit={planner.editSubject}/><TaskStats total={planner.roots.length} done={planner.done} overdue={planner.overdue}/><TaskPanel {...listProps}/></>}
  </div></main>
  {subjectEditor && <SubjectFormDialog subjectEditor={subjectEditor} setSubjectEditor={planner.setSubjectEditor} busy={busy} error={error} onSubmit={planner.saveSubject} onClose={()=>planner.setSubjectEditor(null)}/>}
  {taskEditor && <TaskFormDialog task={planner.currentTasks.find(task=>task.id===taskEditor.id)} subjects={planner.subjects} taskEditor={taskEditor} setTaskEditor={planner.setTaskEditor} parentTitle={planner.currentTasks.find(task=>task.id===taskEditor.parentId)?.title} busy={busy} error={error} onSubmit={planner.saveTask} onClose={()=>planner.setTaskEditor(null)}/>}
  {deleteTarget && <DeleteTaskDialog task={deleteTarget} childCount={planner.children(deleteTarget.id).length} busy={busy} error={error} onClose={()=>planner.setDeleteTarget(null)} onConfirm={planner.confirmDelete}/>}
  {timeTarget && <TimeLogDialog task={timeTarget} entries={planner.timeEntries} busy={busy} error={error} onLog={planner.logTime} onRemove={planner.removeTimeEntry} onClose={planner.closeTimeLog}/>}
 </div></PredictionContext.Provider>;
}
