insert into video_booking (video_booking_id, booking_type, status_code, court_id, hearing_type, comments, created_by, created_time)
values (-1000, 'COURT', 'ACTIVE', 1, 'TRIBUNAL', 'comments about the hearing', 'test_user', '2024-07-24T01:00:00');

insert into prison_appointment (video_booking_id, prison_code, prisoner_number, appointment_type, comments, prison_loc_key, appointment_date,  start_time, end_time)
values (-1000, 'WNI', 'ABCDEF', 'VLB_COURT_MAIN', 'comments about the hearing', 'WNI-ABCDEFG', '2100-07-24', '12:00', '13:00');

insert into booking_history(booking_history_id, video_booking_id, history_type, court_id, hearing_type, comments, created_by, created_time)
values (-1000, -1000, 'CREATE', 1, 'APPEAL','comments about the hearing', 'test_user', '2024-07-24T01:00:00');

insert into booking_history_appointment (booking_history_id, prison_code, prisoner_number, appointment_date, appointment_type, prison_loc_key, start_time, end_time)
values (-1000, 'WNI', 'ABCDEF', '2100-07-24', 'VLB_COURT_MAIN', 'WNI-ABCDEFG', '12:00', '13:00');
