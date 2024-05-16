SET REFERENTIAL_INTEGRITY FALSE;

insert into user_court (user_court_id, court_id, username, created_by, created_time)
values (99000, 1, 'michael.horden@itv.com', 'TIM', current_timestamp),
       (99001, 2, 'michael.horden@itv.com', 'TIM', current_timestamp),
       (99002, 3, 'NOT-michael.horden@itv.com', 'TIM', current_timestamp);

SET REFERENTIAL_INTEGRITY TRUE;