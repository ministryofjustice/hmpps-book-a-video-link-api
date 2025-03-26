insert into reference_code (group_code, code, description,created_by, created_time, enabled)
values ('PROBATION_MEETING_TYPE', 'OTHER', 'Other', 'MATT', current_timestamp, true) on conflict do nothing;
