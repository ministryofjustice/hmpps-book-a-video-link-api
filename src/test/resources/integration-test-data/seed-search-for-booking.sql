insert into video_booking (video_booking_id, booking_type, status_code, court_id, hearing_type, comments, created_by, created_time)
values (1000, 'COURT', 'ACTIVE', 1, 'TRIBUNAL', 'comments about the hearing', 'test_user', current_timestamp);
insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, comments, prison_loc_key, appointment_date,  start_time, end_time)
values (1000, 17, '123456', 'VLB_COURT_MAIN', 'comments about the hearing', 'PVI-123456', current_date, '12:00', '13:00');

insert into video_booking (video_booking_id, booking_type, status_code, court_id, hearing_type, comments, created_by, created_time)
values (2000, 'COURT', 'CANCELLED', 1, 'TRIBUNAL', 'comments about the hearing', 'test_user', current_timestamp);
insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, comments, prison_loc_key, appointment_date,  start_time, end_time)
values (2000, 17, '78910', 'VLB_COURT_MAIN', 'comments about the hearing', 'PVI-78910', current_date + 1, '9:00', '10:00');

insert into video_booking (video_booking_id, booking_type, status_code, court_id, hearing_type, comments, created_by, created_time)
values (3000, 'COURT', 'ACTIVE', 1, 'TRIBUNAL', 'comments about the hearing', 'test_user', current_timestamp);
insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, comments, prison_loc_key, appointment_date,  start_time, end_time)
values (3000, 17, '78910', 'VLB_COURT_MAIN', 'comments about the hearing', 'PVI-78910', current_date + 1, '9:00', '10:00');
