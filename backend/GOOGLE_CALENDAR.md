# Google Calendar

Одностороння синхронізація зайнятості: основний календар та календарі з позначкою
selected у Google Calendar. Сервер перевіряє їх раз на хвилину, також є кнопка
«Оновити зараз». Відкрита вкладка не потрібна; сервер і PostgreSQL мають працювати.
Це періодична синхронізація, не миттєві push-повідомлення.

## Налаштування

1. У Google Cloud увімкнути Google Calendar API, налаштувати OAuth consent screen.
2. Створити OAuth client типу Web application.
3. Додати Authorized redirect URI:
   `http://127.0.0.1:5173/api/google-calendar/callback`.
   У Google Auth Platform / Audience додати свій Google-акаунт до Test users,
   якщо застосунок працює в режимі Testing. У Data Access використовується scope
   `https://www.googleapis.com/auth/calendar.readonly`.
   Завантажити JSON створеного клієнта та виконати з каталогу analysis-source:
   `powershell -File .\setup-google-calendar.ps1 -OAuthJson "C:\path\client_secret.json"`.
   Скрипт зберігає налаштування в backend/config/google-calendar.properties та
   створює постійний ключ шифрування. Повторний запуск зберігає наявний ключ.
   Файл налаштувань виключено з Git; сервер читає його при запуску з каталогу
   backend або analysis-source. Після імпорту потрібно перезапустити сервер.
4. У Run Configuration серверу в IntelliJ задати environment variables:
   - `GOOGLE_CLIENT_ID` — client ID;
   - `GOOGLE_CLIENT_SECRET` — client secret;
   - `GOOGLE_TOKEN_KEY` — Base64 від 32 криптографічно випадкових байтів.
   Ключ потрібно зберігати постійно: його зміна потребує повторного підключення.
   Не комітити секрети та не передавати їх на фронтенд.
   Цей спосіб є альтернативою локальному файлу налаштувань.
5. Запустити сервер і фронтенд. Відкривати саме `http://127.0.0.1:5173`,
   щоб OAuth callback отримав ту саму сесійну cookie.
6. У «Зовнішні календарі» натиснути «Підключити Google Calendar» та надати доступ.

Для іншої адреси задати `GOOGLE_REDIRECT_URI` і `APP_FRONTEND_URL` та змінити
дозволений redirect URI у Google Cloud. В інтернеті використовувати HTTPS.
Google OAuth у режимі External / Testing зазвичай видає refresh token на 7 днів
для Calendar scope. Для тривалого підключення потрібний належний production
статус OAuth застосунку; Google може вимагати перевірку.

## Поведінка

- Завантажується повний знімок від учора до 92 днів наперед із усіма сторінками.
  Це покриває максимальний 90-денний горизонт планування.
- Google розгортає повторювані події через singleEvents=true. Враховуються
  винятки, перенесення, видалення й цілоденні події з часовим поясом календаря.
- Вільні (transparent), скасовані та відхилені користувачем події не блокують час.
- Знімок замінюється атомарно після успішного отримання всіх календарів.
  Помилки мережі залишають останні події та видиме повідомлення про збій.
- Події Google зберігаються окремо від ICS. Їх змінюють у Google Calendar.
- Перепланування відбувається, якщо ввімкнена відповідна опція планування.
- Відключення прибирає локальне підключення та його зайнятість. Дозвіл Google
  можна окремо відкликати в налаштуваннях Google Account.
- Refresh token зашифрований AES-256-GCM; OAuth використовує state, PKCE,
  одноразовий callback і обмеження спроби підключення до 10 хвилин.

Підключення, події та план належать обліковому запису StudyPlanner. Спочатку
потрібно увійти. OAuth callback перевіряє той самий обліковий запис і сеанс,
з якого розпочато підключення. Поточний сервер слухає loopback.

Документація:
- https://developers.google.com/identity/protocols/oauth2/web-server
- https://developers.google.com/workspace/calendar/api/v3/reference/events/list
