insert into video_booking (video_booking_id, booking_type, status_code, court_id, hearing_type, notes_for_staff, created_by, created_time)
values (4000, 'COURT', 'CANCELLED', 1, 'TRIBUNAL', 'comments about the hearing', 'test_user', current_timestamp);
insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, notes_for_staff, prison_location_id, appointment_date,  start_time, end_time)
values (4000, 33, '78910', 'VLB_COURT_MAIN', 'comments about the hearing', 'ba0df03b-7864-47d5-9729-0301b74ecbe2'::uuid, current_date + 1, '9:00', '10:00');