export async function request<T>(path: string, method = 'GET', body?: unknown): Promise<T> {
 let response: Response;
 try {
  const headers: Record<string,string> = { 'X-StudyPlanner': '1' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
   const tokenResponse = await fetch('/api/auth/csrf', { cache: 'no-store' });
   if (!tokenResponse.ok) throw new Error('Не вдалося перевірити сеанс');
   const csrf = await tokenResponse.json(); headers[csrf.headerName] = csrf.token;
  }
  response = await fetch('/api' + path, { method, headers, cache: 'no-store', body: body === undefined ? undefined : JSON.stringify(body) });
 } catch { throw new Error('Не вдалося з’єднатися із сервером. Перевірте, чи він запущений.'); }
 if (!response.ok) {
  if (response.status === 401 && !path.startsWith('/auth/')) window.dispatchEvent(new Event('session-expired'));
  const error = await response.json().catch(() => null);
  throw new Error(error?.message || 'Не вдалося виконати запит. Спробуйте ще раз.');
 }
 return response.status === 204 ? undefined as T : response.json();
}
