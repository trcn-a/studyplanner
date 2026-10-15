# Єдиний доступ до даних через Spring Data JPA

Усі 13 прикладних репозиторіїв є інтерфейсами, які розширюють JpaRepository. Прямого використання JdbcTemplate або EntityManager у прикладному коді немає.

Стандартні операції виконуються через save, findById, delete та інші методи JpaRepository. Додаткові вибірки визначено назвами методів або JPQL-запитами з @Query. Для перенесення власника попередніх даних використано @Modifying із flushAutomatically і clearAutomatically, щоб зберегти відкладені зміни та не залишати застарілі сутності в контексті.

AccountDataService, PlanningDataService, CalendarEventDataService, GoogleCalendarDataService, RewardDataService, AchievementDataService та NotificationDataService координують роботу репозиторіїв і перетворюють сутності на об'єкти відповідей. Ці компоненти належать до шару service та не виконують SQL або запити EntityManager.

Блокування користувача та налаштувань планування визначено через @Lock(PESSIMISTIC_WRITE). Сервісні транзакції, ізоляція користувачів, збереження винагород після видалення завдань і розмежування джерел календарних подій збережені.

Структура БД не змінюється. Flyway і PostgreSQL-драйвер залишаються. Історичні Java-міграції та SQL-перевірки у тестах незалежні від прикладних репозиторіїв і не переписувалися. Контролери розміщені в пакетах controllers.
