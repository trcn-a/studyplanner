import { useAccount } from '../../account/model/AccountContext';
import { Flame, Trophy, Star, LockKeyhole } from 'lucide-react';
import { useGamification } from '../model/useGamification';

export function GamificationPanel({ revision }: { revision: string }) {
 const { account } = useAccount();
 const { data, error, notice, refresh } = useGamification(revision + ":" + account.studyDays);
 if (!data) return error ? <div className="error" role="alert">{error}<button className="text-button" onClick={() => void refresh()}>Спробувати ще</button></div> : null;
 return <section className="gamification" aria-label="Навчальні досягнення">
  <div className="gamification-summary">
   <div className="gamification-level"><Star size={22} aria-hidden="true"/><div><strong>Рівень {data.level}</strong><span>{data.points} балів</span></div><progress aria-label="Прогрес до наступного рівня" value={data.levelProgress} max={data.nextLevelPoints}/></div>
   <div className="gamification-streak"><Flame size={22} aria-hidden="true"/><div><strong>Серія: {data.currentStreak} навч. дн.</strong><span>Найдовша: {data.longestStreak} навч. дн.</span></div></div>
   <div className="gamification-week" aria-label="Активність за останні сім днів">{data.week.map(day => <span key={day.date} className={day.active ? 'active' : ''} title={`${day.date}: ${day.active ? 'є завершені завдання' : 'немає завершень'}`} aria-label={`${day.date}: ${day.active ? 'є завершені завдання' : 'немає завершень'}`}>{new Intl.DateTimeFormat('uk-UA', { weekday: 'short' }).format(new Date(day.date + 'T12:00:00'))}</span>)}</div>
  </div>
  {notice && <p className="gamification-notice" role="status">{notice}</p>}
  {error && <p role="alert">{error}<button className="text-button" onClick={() => void refresh()}>Оновити</button></p>}
  <h3 className="achievements-heading"><Trophy size={16} aria-hidden="true"/>Досягнення · {data.achievements.filter(a => a.unlocked).length}/{data.achievements.length}</h3>
   <div className="achievement-grid">{data.achievements.map(a => <article key={a.id} className={`achievement ${a.unlocked ? 'unlocked' : ''}`}>
    {a.unlocked ? <Trophy size={20} aria-hidden="true"/> : <LockKeyhole size={20} aria-hidden="true"/>}<div><strong>{a.title}</strong><p>{a.description}</p><span>{a.unlocked ? 'Отримано' : `${a.progress} / ${a.target}`}</span>{!a.unlocked && <progress aria-label={a.title} value={a.progress} max={a.target}/>}</div>
   </article>)}</div>
 </section>;
}
