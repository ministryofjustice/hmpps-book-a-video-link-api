-- Used in the prisons resource integration tests for decorated prison locations
insert into location_attribute (location_attribute_id, dps_location_id, prison_id, location_status, location_usage, prison_video_url, notes, created_by, created_time)
values
    (1101, 'e58ed763-928c-4155-bee9-aaaaaaaaaaaa', 33, 'ACTIVE', 'COURT', '/link/1', '', 'test', current_timestamp),
    (1102, 'e58ed763-928c-4155-bee9-bbbbbbbbbbbb', 33, 'ACTIVE', 'PROBATION', '/link/2', '', 'test', current_timestamp),
    (1103, 'e58ed763-928c-4155-bee9-cccccccccccc', 33, 'ACTIVE', 'SCHEDULE', '/link/3', '', 'test', current_timestamp);

insert into location_schedule(location_schedule_id, location_attribute_id, start_day_of_week, end_day_of_week, start_time, end_time, location_usage, created_by, created_time)
values (1201, 1103, 1, 4, '09:00', '17:00', 'COURT', 'test', current_timestamp),
       (1202, 1103, 5, 5, '09:00', '17:00', 'PROBATION', 'test', current_timestamp),
       (1203, 1103, 6, 7, '09:00', '17:00', 'BLOCKED', 'test', current_timestamp);
