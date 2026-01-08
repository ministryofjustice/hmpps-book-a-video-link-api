-- New probation teams for the separation of Devon and Torbay court teams into 4
insert into probation_team (code, description, enabled, read_only, notes, created_by, created_time)
values
    ( 'BSPMCP', 'Barnstaple Magistrates - Probation', true, false, null, 'TIM', current_timestamp),
    ( 'EXECCP', 'Exeter Crown - Probation', true, false, null, 'TIM', current_timestamp),
    ( 'EXEMCP', 'Exeter Magistrates - Probation', true, false, null, 'TIM', current_timestamp),
    ( 'NABMCP', 'Newton Abbot Magistrates - Probation', true, false, null, 'TIM', current_timestamp);
