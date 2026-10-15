import { useCallback, useEffect, useRef, useState } from 'react';
import { planningApi } from '../api/planningApi';
import type { PlanningState, PlanningSettings } from './types';
export function usePlanning(revision: string) {
 const [state,setState]=useState<PlanningState | null>(null);
 const [busy,setBusy]=useState(false);
 const [error,setError]=useState('');
 const version=useRef(0);
 const mounted=useRef(true);
 useEffect(()=>{mounted.current=true;return ()=>{mounted.current=false;version.current++;};},[]);
 const refresh=useCallback(async()=>{
  const current=++version.current;
  try { const value=await planningApi.refresh(); if(mounted.current && current===version.current){setState(value);setError('');} }
  catch(e){if(mounted.current && current===version.current)setError(e instanceof Error?e.message:'Не вдалося оновити розклад');}
 },[]);
 useEffect(()=>{void refresh();const timer=setInterval(()=>void refresh(),60000);return ()=>clearInterval(timer);},[revision,refresh]);
 async function perform(operation: () => Promise<PlanningState>){
  setBusy(true);setError('');++version.current;
  try{const value=await operation();++version.current;if(mounted.current)setState(value);return true;}
  catch(e){if(mounted.current)setError(e instanceof Error?e.message:'Не вдалося зберегти розклад');return false;}
  finally{if(mounted.current)setBusy(false);}
 }
 return {state,busy,error,actions:{
  refresh,
  moveSession: (id: string, start: string, manual: boolean) => perform(() => planningApi.moveSession(id,start,manual)),
  saveSettings: (settings: PlanningSettings) => perform(() => planningApi.saveSettings(settings)),
  generate: () => perform(planningApi.generate),
  importCalendar: (content: string) => perform(() => planningApi.importCalendar(content)),
  removeEvent: (id: string) => perform(() => planningApi.removeEvent(id)),
 }};
}
