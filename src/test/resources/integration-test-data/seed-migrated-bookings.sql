insert into video_booking (video_booking_id, booking_type, status_code, court_id, hearing_type, comments, created_by, created_time, migrated_video_booking_id)
values (1002, 'COURT', 'ACTIVE', 1, 'TRIBUNAL', 'comments about the hearing', 'test_user', current_timestamp, 10001);
insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, comments, prison_loc_uuid, appointment_date,  start_time, end_time)
values (1002, 17, '123456', 'VLB_COURT_MAIN', 'comments about the hearing', 'PVI-123456', current_date + 1, '12:00', '13:00');

insert into video_booking (video_booking_id, booking_type, status_code, probation_team_id, probation_meeting_type, comments, created_by, created_time, migrated_video_booking_id)
values (1003, 'PROBATION', 'ACTIVE', 1, 'PSR', 'comments about the meeting', 'test_user', current_timestamp, 10002);
insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, comments, prison_loc_uuid, appointment_date,  start_time, end_time)
values (1003, 17, '123456', 'VLB_PROBATION', 'comments about the meeting', 'PVI-ABCDEFG', current_date + 1, '12:00', '13:00');
