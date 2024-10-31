insert into video_booking (video_booking_id, booking_type, status_code, court_id, hearing_type, comments, created_by, created_time)
values (1000, 'COURT', 'ACTIVE', 1, 'TRIBUNAL', 'comments about the hearing', 'test_user', current_timestamp);
insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, comments, prison_location_id, appointment_date,  start_time, end_time)
values (1000, 17, '123456', 'VLB_COURT_MAIN', 'comments about the hearing', 'b13f9018-f22d-456f-a690-d80e3d0feb5f', current_date, '12:00', '13:00');

insert into video_booking (video_booking_id, booking_type, status_code, court_id, hearing_type, comments, created_by, created_time)
values (2000, 'COURT', 'CANCELLED', 1, 'TRIBUNAL', 'comments about the hearing', 'test_user', current_timestamp);
insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, comments, prison_location_id, appointment_date,  start_time, end_time)
values (2000, 17, '78910', 'VLB_COURT_MAIN', 'comments about the hearing', 'ba0df03b-7864-47d5-9729-0301b74ecbe2', current_date + 1, '9:00', '10:00');

insert into video_booking (video_booking_id, booking_type, status_code, court_id, hearing_type, comments, created_by, created_time)
values (3000, 'COURT', 'ACTIVE', 1, 'TRIBUNAL', 'comments about the hearing', 'test_user', current_timestamp);
insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, comments, prison_location_id, appointment_date,  start_time, end_time)
values (3000, 17, '78910', 'VLB_COURT_MAIN', 'comments about the hearing', 'ba0df03b-7864-47d5-9729-0301b74ecbe2', current_date + 1, '9:00', '10:00');
