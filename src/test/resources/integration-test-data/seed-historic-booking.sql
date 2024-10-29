insert into video_booking (video_booking_id, booking_type, status_code, court_id, hearing_type, comments, created_by, created_time)
values (-1, 'COURT', 'ACTIVE', 1, 'TRIBUNAL', 'comments about the hearing', 'test_user', current_timestamp);

insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, comments, prison_loc_uuid, appointment_date,  start_time, end_time)
values (-1, 1, 'ABCDEF', 'VLB_COURT_MAIN', 'comments about the hearing', 'PVI-ABCDEFG', current_date - 1, '12:00', '13:00');