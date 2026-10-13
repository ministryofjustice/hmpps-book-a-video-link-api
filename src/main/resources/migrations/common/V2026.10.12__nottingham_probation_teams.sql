-- Nottingham probation teams - all probation court teams
insert into probation_team (code, description, enabled, read_only, notes, created_by, created_time, court_team)
values
    ('NOTPCC', 'Nottingham Crown – Probation', true, false, null, 'TIM', current_timestamp, true),
    ('NOTPMC', 'Nottingham Magistrates – Probation', true, false, null, 'TIM', current_timestamp, true),
    ('DERPCC', 'Derby Crown – Probation', true, false, null, 'TIM', current_timestamp, true),
    ('DERPMC', 'Derby Magistrates – Probation', true, false, null, 'TIM', current_timestamp, true),
    ('MNSPMC', 'Mansfield Crown – Probation', true, false, null, 'TIM', current_timestamp, true),
    ('CHSPMC', 'Chesterfield Magistrates – Probation', true, false, null, 'TIM', current_timestamp, true),
    ('LEIPMC', 'Leicestershire Magistrates – Probation', true, false, null, 'TIM', current_timestamp, true),
    ('LEIPCC', 'Leicestershire Crown – Probation', true, false, null, 'TIM', current_timestamp, true),
    ('LINPMC', 'Lincolnshire Magistrates – Probation', true, false, null, 'TIM', current_timestamp, true),
    ('LINPCC', 'Lincolnshore Crown – Probation', true, false, null, 'TIM', current_timestamp, true);
