-- New probation teams for the West Midlands region
insert into probation_team (code, description, enabled, read_only, notes, created_by, created_time)
values
    ( 'CAMBCC', 'Cambridge Crown - Probation', true, false, null, 'TIM', current_timestamp),
    ( 'CAMBMC', 'Cambridge Magistrates – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'HATFMC', 'Hatfield Magistrates – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'HUNTLC', 'Huntingdon Law Courts – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'LUSBMC', 'Luton and South Bedfordshire Magistrates – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'LUTOCC', 'Luton Crown – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'NOHPCC', 'Northampton Crown – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'NOHPMC', 'Northampton Magistrates – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'PETBCC', 'Peterborough Crown – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'PETBMC', 'Peterborough Magistrates – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'STALCC', 'St Albans Crown – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'STALMC', 'St Albans Magistrates – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'STEVMC', 'Stevenage Magistrates – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'WELLJC', 'Wellingborough Justice Centre – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'GTYAMC', 'Great Yarmouth Magistrates – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'KLYNCC', 'Kings Lynn Crown – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'KLYNMC', 'Kings Lynn Magistrates – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'NORWCC', 'Norwich Crown – Probation', true, false, null, 'TIM', current_timestamp),
    ( 'NORWMC', 'Norwich Magistrates – Probation', true, false, null, 'TIM', current_timestamp);
