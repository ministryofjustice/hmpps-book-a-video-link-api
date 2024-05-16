SET REFERENTIAL_INTEGRITY FALSE;

insert into user_probation (user_probation_id, probation_team_id, username, created_by, created_time)
values (99000, 1, 'michael.horden@channel4.com', 'TIM', current_timestamp),
       (99001, 2, 'michael.horden@channel4.com', 'TIM', current_timestamp),
       (99002, 3, 'NOT-michael.horden@nowhere.com', 'TIM', current_timestamp);

SET REFERENTIAL_INTEGRITY TRUE;