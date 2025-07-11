insert into video_booking (video_booking_id, booking_type, status_code, court_id, hearing_type, notes_for_staff, created_by, created_time)
values (1000, 'COURT', 'ACTIVE', 1, 'TRIBUNAL', 'comments about the hearing', 'test_user', current_timestamp);
insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, notes_for_staff, prison_location_id, appointment_date,  start_time, end_time)
values (1000, 67, '123456', 'VLB_COURT_MAIN', 'comments about the hearing', '103f3f31-4cc7-4c71-aa32-994b3de471b1'::uuid, current_date + 1, '12:00', '13:00');

insert into video_booking (video_booking_id, booking_type, status_code, probation_team_id, probation_meeting_type, notes_for_staff, created_by, created_time)
values (1001, 'PROBATION', 'ACTIVE', 1, 'PSR', 'comments about the meeting', 'test_user', current_timestamp);
insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, notes_for_staff, prison_location_id, appointment_date,  start_time, end_time)
values (1001, 67, '123456', 'VLB_PROBATION', 'comments about the meeting', '103f3f31-4cc7-4c71-aa32-994b3de471b1'::uuid, current_date + 1, '12:00', '13:00');
