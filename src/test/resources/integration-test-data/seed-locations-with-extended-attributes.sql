-- Used in the prisons resource integration tests for decorated prison locations
insert into location_attribute (location_attribute_id, dps_location_id, prison_id, location_status, location_usage, prison_video_url, notes, created_by, created_time)
values (801, 'e58ed763-928c-4155-bee9-fdbaaadc15f3', 27, 'ACTIVE', 'SCHEDULE', '/video-link/xxx', 'some notes', 'test_user', current_timestamp);

insert into location_schedule(location_schedule_id, location_attribute_id, start_day_of_week, end_day_of_week, start_time, end_time, location_usage, created_by, created_time)
values (901, 801, 1, 4, '00:01', '23:59', 'COURT', 'test_user', current_timestamp),
       (902, 801, 5, 5, '00:01', '23:59', 'PROBATION', 'test_user', current_timestamp),
       (903, 801, 6, 7, '00:01', '23:59', 'BLOCKED', 'test_user', current_timestamp);
