-- Do not invent availability dates for existing observations.
ALTER TABLE tasks ADD COLUMN prediction_data_at TIMESTAMP WITH TIME ZONE;
