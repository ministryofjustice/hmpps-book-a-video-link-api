insert into video_booking (video_booking_id, booking_type, status_code, court_id, hearing_type, comments, created_by, created_time)
values (-3, 'COURT', 'ACTIVE', 1, 'TRIBUNAL', 'comments about the hearing', 'test_user', current_timestamp);

insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, comments, prison_location_id, appointment_date,  start_time, end_time)
values (-3, 1, 'ABCDEF', 'VLB_COURT_MAIN', 'comments about the hearing', '926d8f38-7149-4fda-b51f-85abcbcb0d00'::uuid, current_date - 1, '12:00', '13:00');