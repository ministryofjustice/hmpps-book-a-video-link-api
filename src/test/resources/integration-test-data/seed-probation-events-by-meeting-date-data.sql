
insert into video_booking (video_booking_id, booking_type, status_code, probation_team_id, probation_meeting_type, comments, created_by, created_time)
values (-4000, 'PROBATION', 'ACTIVE', 1, 'PSR', 'comments about the meeting', 'test_user', '2024-01-01T01:00:00');

insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, comments, prison_loc_uuid, appointment_date,  start_time, end_time)
values (-4000, 1, 'ABCDEF', 'VLB_PROBATION', 'comments about the meeting', 'PVI-ABCDEFG', '2099-01-25', '16:00', '17:00');

insert into booking_history(booking_history_id, video_booking_id, history_type, probation_team_id, probation_meeting_type, comments, created_by, created_time)
values (-4000, -4000, 'CREATE', 1, 'PSR','comments about the meeting', 'test_user', '2024-01-01T01:00:00');

insert into booking_history_appointment (booking_history_id, prison_code, prisoner_number, appointment_date, appointment_type, prison_loc_uuid, start_time, end_time)
values (-4000, 'PVI', 'ABCDEF', '2099-01-25', 'VLB_PROBATION', 'PVI-ABCDEFG', '16:00', '17:00');

insert into video_booking (video_booking_id, booking_type, status_code, probation_team_id, probation_meeting_type, comments, created_by, created_time, migrated_description)
values (-4100, 'PROBATION', 'ACTIVE', 28, 'PSR', 'comments about the meeting', 'test_user', '2024-01-01T01:00:00', 'Free text probation team name');

insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, comments, prison_loc_uuid, appointment_date,  start_time, end_time)
values (-4100, 1, 'DEFGHI', 'VLB_PROBATION', 'comments about the meeting', 'PVI-ABCDEFG', '2099-01-25', '16:00', '17:00');

insert into booking_history(booking_history_id, video_booking_id, history_type, probation_team_id, probation_meeting_type, comments, created_by, created_time)
values (-4100, -4100, 'CREATE', 28, 'PSR','comments about the meeting', 'test_user', '2024-01-01T01:00:00');

insert into booking_history_appointment (booking_history_id, prison_code, prisoner_number, appointment_date, appointment_type, prison_loc_uuid, start_time, end_time)
values (-4100, 'PVI', 'ABCDEF', '2099-01-25', 'VLB_PROBATION', 'PVI-ABCDEFG', '16:00', '17:00');
