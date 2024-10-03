
insert into court (court_id, code, description, enabled, read_only, notes, created_by, created_time)
  values (998, 'ENABLED', 'Enabled court', true, false, 'Enabled court', 'TIM', current_timestamp),
         (999, 'NOT_ENABLED', 'Not enabled court', false, false, 'Not enabled court', 'TIM', current_timestamp);
