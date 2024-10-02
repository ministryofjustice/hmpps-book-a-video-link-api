insert into video_booking (video_booking_id, booking_type, status_code, court_id, hearing_type, comments, created_by, created_time)
values (-1, 'COURT', 'ACTIVE', 1, 'TRIBUNAL', 'comments about the hearing', 'test_user', current_timestamp);

insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, comments, prison_loc_key, appointment_date,  start_time, end_time)
values (-1, 1, 'OLD123', 'VLB_COURT_MAIN', 'comments about the hearing', 'PVI-ABCDEFG', current_date, '12:00', '13:00');

insert into booking_history (booking_history_id, video_booking_id, history_type, court_id, hearing_type, comments, created_by, created_time)
values (-1, -1, 'CREATE', 1, 'TRIBUNAL', 'comments about the hearing', 'test_user', current_timestamp);

insert into booking_history_appointment(booking_history_appointment_id, booking_history_id, prison_code, prisoner_number, appointment_date, appointment_type, prison_loc_key, start_time, end_time)
values (-1, -1, 'PVI', 'OLD123', current_date, 'VLB_COURT_MAIN', 'PVI-ABCDEFG', '12:00', '13:00');
