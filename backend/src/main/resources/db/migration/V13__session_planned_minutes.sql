-- Запланований обсяг сесії зберігається окремо від фактичного часу, щоб аналітика могла порівнювати план і факт.
-- Для старих PLANNED/SKIPPED записів значення обчислюється з їхніх меж; для завершених сесій воно невідоме.
ALTER TABLE study_sessions ADD COLUMN planned_minutes INTEGER CHECK (planned_minutes IS NULL OR planned_minutes > 0);
