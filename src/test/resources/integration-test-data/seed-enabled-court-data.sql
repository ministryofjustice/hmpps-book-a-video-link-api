
insert into court (court_id, code, description, enabled, notes, created_by, created_time)
  values (2, 'ENABLED', 'Enabled court', true, 'Enabled court', 'TIM', current_timestamp),
         (3, 'NOT_ENABLED', 'Not enabled court', false, 'Not enabled court', 'TIM', current_timestamp);
