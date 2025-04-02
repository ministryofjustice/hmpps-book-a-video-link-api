-- New probation teams for the probation pilot
insert into probation_team (code, description, enabled, read_only, notes, created_by, created_time)
values ( 'DEVTSP', 'Devon and Torbay PDU - Sentence Management - Probation', true, false, null, 'TIM', current_timestamp);

insert into probation_team (code, description, enabled, read_only, notes, created_by, created_time)
values ( 'DEVTCP', 'Devon and Torbay PDU - Court Teams - Probation', true, false, null, 'TIM', current_timestamp);

insert into probation_team (code, description, enabled, read_only, notes, created_by, created_time)
values ( 'WARWCP', 'Warwickshire PDU - Coventry and Leamington Court Teams - Probation', true, false, null, 'TIM', current_timestamp);
