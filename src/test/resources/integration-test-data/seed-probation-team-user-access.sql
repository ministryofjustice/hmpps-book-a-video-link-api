insert into user_probation (user_probation_id, probation_team_id, username, created_by, created_time) values (-1, 1, 'probation_user', 'TIM', current_timestamp) on conflict do nothing ;
