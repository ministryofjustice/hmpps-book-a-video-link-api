-- Used in the job resource integration tests for decorated prison locations

-- Historically blocked, should be made active.
insert into location_attribute (location_attribute_id, dps_location_id, prison_id, location_status, blocked_from, blocked_to, location_usage, created_by, created_time)
values (1000, '00000000-0000-0000-0000-000000000000', 27, 'TEMPORARILY_BLOCKED', current_date - 1, current_date - 1, 'SHARED', 'test_user', current_timestamp);

-- Future blocked, should be left blocked.
insert into location_attribute (location_attribute_id, dps_location_id, prison_id, location_status, blocked_from, blocked_to, location_usage, created_by, created_time)
values (1001, '00000000-0000-0000-0000-000000000001', 27, 'TEMPORARILY_BLOCKED',current_date - 1, current_date + 1, 'SHARED', 'test_user', current_timestamp);

-- Not blocked, should be left active.
insert into location_attribute (location_attribute_id, dps_location_id, prison_id, location_status, location_usage, created_by, created_time)
values (1002, '00000000-0000-0000-0000-000000000002', 27, 'ACTIVE', 'SHARED', 'test_user', current_timestamp);
