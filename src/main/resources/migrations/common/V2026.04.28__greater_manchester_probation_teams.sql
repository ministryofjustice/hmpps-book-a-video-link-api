-- New probation teams
insert into probation_team (code, description, enabled, read_only, notes, created_by, created_time, court_team)
values
    ('BLTNCC', 'Bolton Crown - Probation', true, false, null, 'TIM', current_timestamp, true),
    ('BLTNMC', 'Bolton Magistrates - Probation', true, false, null, 'TIM', current_timestamp, true),
    ('MNCSCC', 'Manchester Crown (Crown Square) - Probation', true, false, null, 'TIM', current_timestamp, true),
    ('MNMSCC', 'Manchester Crown (Minshull Street) - Probation', true, false, null, 'TIM', current_timestamp, true),
    ('MNPRMC', 'Manchester Magistrates - Probation', true, false, null, 'TIM', current_timestamp, true),
    ('STKPMC', 'Stockport Magistrates - Probation', true, false, null, 'TIM', current_timestamp, true),
    ('TAMEMC', 'Tameside Magistrates - Probation', true, false, null, 'TIM', current_timestamp, true),
    ('WIGNMC', 'Wigan Magistrates - Probation', true, false, null, 'TIM', current_timestamp, true);

