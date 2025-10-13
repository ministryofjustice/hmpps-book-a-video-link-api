-- Retire the probation team code REMPSR
-- The read_only=true indicator prevents it from being shown in A&A and DPS profile prison select lists.
update probation_team
set enabled = false, read_only = true
where code = 'REMPSR';
