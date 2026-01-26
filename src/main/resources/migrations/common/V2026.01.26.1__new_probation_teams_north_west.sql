-- New probation teams for the North West region
insert into probation_team (code, description, enabled, read_only, notes, created_by, created_time)
values
    ( 'BARWMG', 'Barrow Magistrates – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'CARLCC', 'Carlisle Crown – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'CARLMC', 'Carlisle Magistrates – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'CHSTCC', 'Chester Crown – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'CHSTMC', 'Chester Magistrates – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'CREWMC', 'Crewe Magistrates – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'LVPLCC', 'Liverpool Crown – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'LVPLMC', 'Liverpool Magistrates – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'SFTNMC', 'Sefton Magistrates – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'WARRMC', 'Warrington Magistrates – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'WIRRMC', 'Wirral Magistrates – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'WRKNMC', 'Workington Magistrates - Probation', true, false, null, 'TIM', current_timestamp);
