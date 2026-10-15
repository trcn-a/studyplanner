import { useEffect, useState, type ReactNode, type FormEvent } from 'react';
import { accountApi, type Account } from '../api/accountApi';
import { AccountContext } from '../model/AccountContext';
export function AccountGate({ children }: { children: ReactNode }) {
 const [account, setAccount] = useState<Account | null>(null);
 const [loading, setLoading] = useState(true);
 const [register, setRegister] = useState(false);
 const [busy, setBusy] = useState(false);
 const [error, setError] = useState('');
 useEffect(() => {
  let active = true;
  accountApi.me().then(user => { if (active) setAccount(user); }).catch(() => {}).finally(() => { if (active) setLoading(false); });
  const expired = () => { setAccount(null); setError('Сеанс завершився. Увійди знову.'); };
  window.addEventListener('session-expired', expired);
  return () => { active = false; window.removeEventListener('session-expired', expired); };
 }, []);
 async function submit(event: FormEvent<HTMLFormElement>) {
  event.preventDefault(); const form = new FormData(event.currentTarget); setBusy(true); setError('');
  try {
   const email = String(form.get('email')), password = String(form.get('password'));
   if (register && password !== form.get('confirm')) throw new Error('Паролі не збігаються');
   const user = register ? await accountApi.register(email, String(form.get('name')), password, '') : await accountApi.login(email, password);
   setAccount(user);
  } catch (e) { setError(e instanceof Error ? e.message : 'Не вдалося увійти'); }
  finally { setBusy(false); }
 }
 if (loading) return <div className="auth-screen"><p>Завантажуємо…</p></div>;
 if (account) return <AccountContext.Provider value={{ account, update: setAccount, logout: async () => { await accountApi.logout(); setAccount(null); setError(''); } }}><div key={account.id}>{children}</div></AccountContext.Provider>;
 return <main className="auth-screen"><section className="auth-card"><h1>StudyPlanner</h1>{register && <h2>Створити обліковий запис</h2>}
  <form onSubmit={submit} key={String(register)}>
   {register && <label>Ім’я<input name="name" required maxLength={100} autoComplete="name" /></label>}
   <label>Електронна пошта<input name="email" type="email" required maxLength={254} autoComplete="email" /></label>
   <label>Пароль<input name="password" type="password" required minLength={register ? 10 : undefined} maxLength={72} autoComplete={register ? 'new-password' : 'current-password'} /></label>
   {register && <label>Повтори пароль<input name="confirm" type="password" required autoComplete="new-password" /></label>}
   {error && <p className="error" role="alert">{error}</p>}
   <button className="primary" disabled={busy}>{busy ? 'Зачекай…' : register ? 'Зареєструватися' : 'Увійти'}</button>
  </form><button className="text-button" disabled={busy} onClick={() => { setRegister(!register); setError(''); }}>{register ? 'Уже є обліковий запис? Увійти' : 'Створити обліковий запис'}</button>
 </section></main>;
}
