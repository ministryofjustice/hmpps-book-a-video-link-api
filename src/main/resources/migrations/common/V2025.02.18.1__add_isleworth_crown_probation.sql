-- Add Isleworth Crown Probation team to the PPoC list
insert into probation_team (code, description, enabled, read_only, notes, created_by, created_time)
values ( 'ISLWCP', 'Isleworth Crown - Probation', true, false, null, 'TIM', current_timestamp);
