insert into video_booking (video_booking_id, booking_type, status_code, court_id, hearing_type, notes_for_staff, created_by, created_time)
values (-100, 'COURT', 'ACTIVE', 1, 'TRIBUNAL', 'comments about the hearing', 'test_user', current_timestamp);

insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, notes_for_staff, prison_location_id, appointment_date,  start_time, end_time)
values (-100, 1, 'OLD123', 'VLB_COURT_MAIN', 'comments about the hearing', '926d8f38-7149-4fda-b51f-85abcbcb0d00'::uuid, current_date, '12:00', '13:00');

insert into booking_history (booking_history_id, video_booking_id, history_type, court_id, hearing_type, notes_for_staff, created_by, created_time)
values (-1, -100, 'CREATE', 1, 'TRIBUNAL', 'comments about the hearing', 'test_user', current_timestamp);

insert into booking_history_appointment(booking_history_appointment_id, booking_history_id, prison_code, prisoner_number, appointment_date, appointment_type, prison_location_id, start_time, end_time)
values (-1, -1, 'PVI', 'OLD123', current_date, 'VLB_COURT_MAIN', '926d8f38-7149-4fda-b51f-85abcbcb0d00'::uuid, '12:00', '13:00');

insert into video_booking (video_booking_id, booking_type, status_code, court_id, hearing_type, notes_for_staff, created_by, created_time)
values (-200, 'COURT', 'ACTIVE', 1, 'TRIBUNAL', 'comments about the hearing', 'test_user', current_timestamp);

insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, notes_for_staff, prison_location_id, appointment_date,  start_time, end_time)
values (-200, 2, 'OLD123', 'VLB_COURT_MAIN', 'comments about the hearing', '926d8f38-7149-4fda-b51f-85abcbcb0d00'::uuid, current_date, '09:00', '10:00');

insert into booking_history (booking_history_id, video_booking_id, history_type, court_id, hearing_type, notes_for_staff, created_by, created_time)
values (-2, -200, 'CREATE', 1, 'TRIBUNAL', 'comments about the hearing', 'test_user', current_timestamp);

insert into booking_history_appointment(booking_history_appointment_id, booking_history_id, prison_code, prisoner_number, appointment_date, appointment_type, prison_location_id, start_time, end_time)
values (-2, -2, 'PVI', 'OLD123', current_date, 'VLB_COURT_MAIN', '926d8f38-7149-4fda-b51f-85abcbcb0d00'::uuid, '09:00', '10:00');
