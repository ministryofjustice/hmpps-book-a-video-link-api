-- Correct a probation team code for Hammersmith and Fulham
update probation_team set code = 'HMFMSM' where code = 'HNFMSM';
