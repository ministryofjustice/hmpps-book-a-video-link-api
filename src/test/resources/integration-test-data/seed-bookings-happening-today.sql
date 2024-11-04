insert into video_booking (video_booking_id, booking_type, status_code, court_id, hearing_type, comments, created_by, created_time)
values (1000, 'COURT', 'ACTIVE', 1, 'TRIBUNAL', 'comments about the hearing', 'test_user', current_timestamp);
insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, comments, prison_location_id, appointment_date,  start_time, end_time)
values (1000, 17, '123456', 'VLB_COURT_MAIN', 'comments about the hearing', 'b13f9018-f22d-456f-a690-d80e3d0feb5f'::uuid, current_date, '12:00', '13:00');

insert into video_booking (video_booking_id, booking_type, status_code, probation_team_id, probation_meeting_type, comments, created_by, created_time)
values (1001, 'PROBATION', 'ACTIVE', 1, 'PSR', 'comments about the meeting', 'test_user', current_timestamp);
insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, comments, prison_location_id, appointment_date,  start_time, end_time)
values (1001, 17, '123456', 'VLB_PROBATION', 'comments about the meeting', '926d8f38-7149-4fda-b51f-85abcbcb0d00'::uuid, current_date, '12:00', '13:00');
