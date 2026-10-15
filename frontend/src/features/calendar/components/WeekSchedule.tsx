import { minute, selectWeekSchedule, layoutDay } from '../model/weekSchedule';
import { useEffect, useRef, type CSSProperties } from 'react';
import type { Task } from '../../tasks/model/types';
import type { Subject } from '../../subjects/model/types';
import type { PlanningState, ScheduleBlock } from '../../planning/model/types';
import { subjectColor } from '../../subjects/model/colors';
import { weekDates } from '../model/calendar';
interface Props { planning: PlanningState | null; date: string; tasks: Task[]; subjects: Subject[]; busy: boolean; dayView?: boolean; onSelect: (date: string) => void; onEdit: (task: Task) => void; onCreate: (date: string, time: string) => void; onMove: (block: ScheduleBlock,start: string) => Promise<void>; }
const HOUR_HEIGHT = 90;
const pixels = (minutes: number) => minutes * HOUR_HEIGHT / 60;

export function WeekSchedule({planning,date,tasks,subjects,busy,dayView=false,onSelect,onEdit,onCreate,onMove}:Props){
 const scroll=useRef<HTMLDivElement>(null);
 const dragged=useRef<{block:ScheduleBlock;offset:number}|null>(null);
 const suppressClick=useRef(false);
 useEffect(()=>{if(scroll.current)scroll.current.scrollTop=pixels(8*60);},[]);
 const days=dayView ? [date] : weekDates(date);
 const { taskMap, blocks } = selectWeekSchedule(tasks, planning);
 return <section aria-label={dayView ? 'Денний розклад' : 'Тижневий розклад'}>
  {planning && <div className="schedule-legend"><span className="availability-key">Доступний час</span><span className="auto-key">Автоматично</span><span className="manual-key">Вручну</span><span>{planning.settings.zoneId}</span></div>}
  <div className="schedule-scroll" ref={scroll}><div className={`schedule-grid${dayView?' schedule-grid-day':''}`} style={{'--hour-height': `${HOUR_HEIGHT}px`} as CSSProperties}>
   <div className="schedule-corner"/>
   {days.map(day=><button key={day} className="schedule-heading" aria-pressed={day===date} onClick={()=>onSelect(day)}>{new Intl.DateTimeFormat('uk-UA',{weekday:'short',day:'numeric',month:'short'}).format(new Date(day+'T12:00:00'))}</button>)}
   <div className="schedule-hours">{Array.from({length:24},(_,h)=><span key={h} style={{top:h*HOUR_HEIGHT}}>{String(h).padStart(2,'0')}:00</span>)}</div>
   {days.map(day=>{
    const { placed, laneCount, weekday } = layoutDay(day, blocks);
    return <div className="schedule-day" key={day}
     onDragOver={event=>{if(dragged.current&&!busy){event.preventDefault();event.dataTransfer.dropEffect='move';}}}
     onDrop={event=>{if(!dragged.current||busy)return;event.preventDefault();const {block,offset}=dragged.current;dragged.current=null;const minuteOfDay=Math.max(0,Math.min(1425,Math.round(((event.clientY-event.currentTarget.getBoundingClientRect().top)*60/HOUR_HEIGHT-offset)/15)*15));void onMove(block,`${day}T${String(Math.floor(minuteOfDay/60)).padStart(2,'0')}:${String(minuteOfDay%60).padStart(2,'0')}:00`);}}>
     {planning?.settings.availability.filter(a=>a.dayOfWeek===weekday).map((a,i)=><div key={i} className="schedule-available" style={{top:pixels(minute(day+'T'+a.start)),height:pixels(minute(day+'T'+a.end)-minute(day+'T'+a.start))}}/>)}
     {Array.from({length:24},(_,hour)=><button className="schedule-slot" key={hour} disabled={busy || !subjects.length || hour===23} aria-label={`Додати заняття: ${day}, ${hour}:00`} onClick={()=>onCreate(day,`${String(hour).padStart(2,'0')}:00`)}/>)}
     {placed.map(({block,start,end,lane})=>{
      const task=block.taskId?taskMap.get(block.taskId):undefined;
      const subject=subjects.find(s=>s.id===task?.subjectId);
      const movable=!!task&&task.status!=='DONE'&&['AUTO','MANUAL','PINNED'].includes(block.source);
      return <button key={block.id} className={`schedule-event ${movable?'movable-session':''} ${pixels(end-start)<52?'short-session':''} ${block.source==='AUTO'?'auto-session':''} ${['MANUAL','PINNED'].includes(block.source)?'manual-session':''} ${(block.source==='EXTERNAL' || block.source==='GOOGLE')?'external-session':''} ${block.source==='COMPLETED' || task?.status==='DONE'?'completed':''}`} disabled={busy} onClick={()=>{if(!suppressClick.current&&task)onEdit(task);}}
       draggable={movable&&!busy} onDragStart={event=>{if(!movable)return;dragged.current={block,offset:(event.clientY-event.currentTarget.getBoundingClientRect().top)*60/HOUR_HEIGHT};suppressClick.current=true;event.dataTransfer.setData('text/plain',block.id);event.dataTransfer.effectAllowed='move';}}
       onDragEnd={()=>{dragged.current=null;setTimeout(()=>{suppressClick.current=false;},0);}}
       onKeyDown={event=>{if(!movable||!event.altKey||!['ArrowUp','ArrowDown','ArrowLeft','ArrowRight'].includes(event.key))return;event.preventDefault();const date=new Date(block.start);date.setMinutes(date.getMinutes()+({ArrowUp:-15,ArrowDown:15,ArrowLeft:-1440,ArrowRight:1440}[event.key]??0));const start=`${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}T${String(date.getHours()).padStart(2,'0')}:${String(date.getMinutes()).padStart(2,'0')}:00`;void onMove(block,start);}}
       style={{top:pixels(start),height:Math.max(12,pixels(end-start)),left:`${lane/laneCount*100}%`,width:`${100/laneCount}%`,borderLeftColor:subject?subjectColor(subject.color):undefined}}
       title={`${block.start.replace('T',' ')} — ${block.end.replace('T',' ')} · ${block.title}`}>
       <small>{start===0?'00:00':block.start.slice(11,16)}–{end===1440?'24:00':block.end.slice(11,16)}</small><small>{(block.source==='EXTERNAL' || block.source==='GOOGLE')?(block.source==='GOOGLE'?'Google Calendar':'Зовнішня подія'):subject?.name}{block.source==='COMPLETED'?' · Виконано':block.source==='RUNNING'?' · Таймер':''}</small><strong>{block.title}</strong>
      </button>;
     })}
    </div>;
   })}
  </div></div>
 </section>;
}
