import type { Task } from '../../tasks/model/types';
import type { PlanningState, ScheduleBlock } from '../../planning/model/types';
export const minute=(value:string)=>Number(value.slice(11,13))*60+Number(value.slice(14,16));

export function selectWeekSchedule(tasks: Task[], planning: PlanningState | null, now = new Date()) {
 const taskMap=new Map(tasks.map(t=>[t.id,t]));
 const blocks=[...tasks.filter(t=>t.scheduledStart && t.scheduledEnd).map(t=>({id:t.id,taskId:t.id,title:t.title,start:t.scheduledStart!,end:t.scheduledEnd!,source:'MANUAL' as const})),...(planning?.sessions??[]).filter(s=>s.taskId && taskMap.has(s.taskId)),...(planning?.externalEvents??[])];
 const parents=new Set(tasks.map(t=>t.parentTaskId).filter(Boolean));
 const nowParts=new Intl.DateTimeFormat('sv-SE',{timeZone:planning?.settings.zoneId??'Europe/Kyiv',year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',second:'2-digit',hourCycle:'h23'}).format(now).replace(' ','T');
 const unscheduled=tasks.filter(t=>t.status!=='DONE' && !parents.has(t.id) && !blocks.some(b=>b.taskId===t.id && b.source!=='COMPLETED' && (b.source==='RUNNING' || b.end>nowParts)));
 return { taskMap, blocks, unscheduled };
}

export function layoutDay(day: string, blocks: ScheduleBlock[]) {
    const nextDate=new Date(day+'T12:00:00');nextDate.setDate(nextDate.getDate()+1);
    const next=`${nextDate.getFullYear()}-${String(nextDate.getMonth()+1).padStart(2,'0')}-${String(nextDate.getDate()).padStart(2,'0')}T00:00:00`;
    const items=blocks.filter(b=>b.start<next && b.end>day+'T00:00:00').sort((a,b)=>a.start.localeCompare(b.start));
    const lanes:number[]=[];
    const placed=items.map(block=>{const start=block.start.slice(0,10)<day?0:minute(block.start);const end=block.end.slice(0,10)>day?1440:minute(block.end);let lane=lanes.findIndex(value=>value<=start);if(lane<0)lane=lanes.length;lanes[lane]=end;return {block,start,end,lane};});
    const weekday=(new Date(day+'T12:00:00').getDay()+6)%7+1;
 return { placed, laneCount: lanes.length, weekday };
}
