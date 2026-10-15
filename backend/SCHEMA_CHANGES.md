# Оновлення схеми V11

Міграція V11__schema_integrity розташована в src/main/java/db/migration.
Вона виконується Flyway під час наступного запуску сервера. Попередні міграції
V1–V10 не змінюються. Перед оновленням робочої БД збережіть її резервну копію.

- Власник завдання та запису часу зберігається в user_id. Складені зовнішні
  ключі узгоджують власників дисципліни, завдання, записів часу, сесій та проблем
  планування. Перенесення старих дисциплін каскадно оновлює власників залежних записів.
- Підзавдання пов’язується з батьківським у тій самій дисципліні й акаунті.
  Один рівень вкладеності перевіряє TaskService; зміна батьківського завдання
  через API не передбачена. БД не перевіряє довільні цикли при прямому SQL-записі.
- База гарантує один активний таймер на користувача. Блокування операцій
  виконується через рядок app_users, а не через спільні таблиці блокувань.
- Видалено subjects.archived, технічні id у planning_settings та
  google_calendar_connection, таблиці timer_guard, gamification_lock і account_setup_lock.
- Додано перевірки кольору, типу сповіщення, початкових характеристик завдання,
  невід’ємного нерозподіленого часу та послідовності часових позначок запису роботи.
- Унікальність електронної адреси не залежить від регістру. password_hash
  розширено до VARCHAR(255); алгоритм хешування залишається BCrypt.
- Календарні позначки в tasks, study_sessions та calendar_events переведено
  на TIMESTAMP WITH TIME ZONE. Старі значення інтерпретуються в часовому поясі
  власника. API й календар продовжують працювати з локальним часом через UserTime.
  Щотижнева доступність зберігає локальні TIME та день тижня.

Неіснуючий локальний час під час весняного переходу відхиляється. Для повторюваної
осінньої години при ручному введенні обирається перше входження. Автопланувальник
пропускає неоднозначні проміжки й розділяє доступність на межі зміни зміщення.
За наявності старих записів у неіснуючу годину міграція припиняється з поясненням,
а не пересуває їх непомітно. Парність дат перевіряє БД; межі одного локального дня — TaskInput і TaskService.

Журнал винагород залишається незалежним від видалення завдань. Google-події
та імпортовані iCalendar-події залишаються в окремих таблицях через різний
життєвий цикл синхронізації. Обмеження довжини описів збережені.

Перевірено повний набір серверних тестів та додаткові сценарії перенесення
даних, ізоляції акаунтів, таймерів, підзавдань і переходів літнього часу.
Тести виконано на H2 у режимі PostgreSQL. Перевірка на окремій PostgreSQL
не виконана: локальний Docker не запускається через помилку WSL.

# Оновлення схеми V15

У поточній схемі власник завдання визначається через tasks.subject_id → subjects.user_id. Поле tasks.user_id вилучене; попередній опис V11 вище стосується історичного стану схеми.

Міграція V15 зберігає завдання, підзавдання, навчальні сесії та винагороди. Зв’язок підзавдання з батьківським записом перевіряє складений зовнішній ключ (parent_task_id, subject_id). Видалення завдань каскадно видаляє сесії та проблеми планування; журнал винагород зберігається.

study_sessions.user_id і planning_issues.user_id залишаються для запитів і налаштувань поточного користувача. Після V15 відповідність цього користувача власнику дисципліни контролює серверний застосунок, а не складений зовнішній ключ до tasks. Обмеження одного активного таймера на користувача залишається в базі даних.

# Оновлення схеми V16
Навчальні дні для стріку зберігаються в planning_settings.study_days. V16 копіює наявні значення з app_users і видаляє app_users.study_days. Нові користувачі отримують типовий графік понеділок–п’ятниця. API профілю залишається сумісним; операція збереження профілю атомарно оновлює облікові дані та навчальні налаштування.

# Оновлення схеми V18

Події зберігаються в єдиній таблиці calendar_events. source має значення ICALENDAR або GOOGLE; external_uid зберігає ідентифікатор відповідного джерела. Унікальне поєднання (user_id, source, external_uid) захищає від дублювання в межах одного джерела й користувача. Внутрішній id залишається UUID-первинним ключем.

V18 зберігає всі імпортовані та синхронізовані події, перевіряє кількість записів і вилучає google_calendar_events. Для Google-записів створюються окремі внутрішні UUID, щоб однаковий зовнішній ідентифікатор у різних користувачів не конфліктував. Google Calendar connection залишається окремим сховищем токена й стану синхронізації.

Операції імпорту .ics, видалення імпортованих подій, заміни Google-знімка та відключення Google відфільтровують записи за source і поточним користувачем. У поточній схемі 12 основних таблиць; історичні міграції не змінено.

V19: activity_date removed. Completion dates are derived from completed_at in the current planning time zone. Legacy date-only records are reconstructed as midnight in that zone.

V20: notification_settings stores reminders_enabled and reminder_minutes, one row per user (user_id PK/FK). Existing settings are copied before the app_users columns are removed. Legacy data claiming retains the destination user's notification preferences. Main table count: 13.

V21: study_sessions and planning_issues no longer store user_id. Ownership follows tasks -> subjects -> app_users. All user-scoped reads follow this relationship. SessionService locks the account before checking for a running timer; the former database uniqueness by user_id is removed. completion_rewards retains user_id because rewards survive task deletion.

V22: completion_rewards has its own UUID id primary key. task_id remains NOT NULL UNIQUE with no foreign key to tasks; deleting a task preserves its reward. Existing rewards receive independent random identifiers.


V24: tasks.planning_time_source stores USER_ESTIMATE or PREDICTION. Existing tasks default to PREDICTION. The student estimate and prediction training snapshot remain separate.

V25: planning_settings.break_minutes stores a 0–120 minute break between study sessions. Existing settings default to zero. The planner reserves gaps without counting them as task time.


V24: tasks.planning_time_source stores USER_ESTIMATE or PREDICTION. Existing tasks default to PREDICTION. The student estimate and prediction training snapshot remain separate.


V26: nullable tasks.task_type, difficulty, initial_task_type, initial_difficulty with enum CHECK constraints. Existing values remain unknown. Drops initial_description_length; description length is no longer a prediction feature.
