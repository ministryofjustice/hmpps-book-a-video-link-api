insert into video_booking (video_booking_id, booking_type, status_code, court_id, hearing_type, comments, created_by, created_time)
values (-1000, 'COURT', 'ACTIVE', 1, 'TRIBUNAL', 'comments about the hearing', 'court_user', '2024-07-24T01:00:00');

insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, comments, prison_location_id, appointment_date,  start_time, end_time)
values (-1000, 1, 'ABCDEF', 'VLB_COURT_MAIN', 'comments about the hearing', '926d8f38-7149-4fda-b51f-85abcbcb0d00'::uuid, '2100-07-24', '12:00', '13:00');

insert into booking_history(booking_history_id, video_booking_id, history_type, court_id, hearing_type, comments, created_by, created_time)
values (-1000, -1000, 'CREATE', 1, 'APPEAL','comments about the hearing', 'court_user', '2024-07-24T01:00:00');

insert into booking_history_appointment (booking_history_id, prison_code, prisoner_number, appointment_date, appointment_type, prison_location_id, start_time, end_time)
values (-1000, 'PVI', 'ABCDEF', '2100-07-24', 'VLB_COURT_MAIN', '926d8f38-7149-4fda-b51f-85abcbcb0d00'::uuid, '12:00', '13:00');

insert into booking_history(booking_history_id, video_booking_id, history_type, court_id, hearing_type, comments, created_by, created_time)
values (-1100, -1000, 'AMEND', 1, 'APPEAL','comments about the hearing', 'court_user', '2024-07-24T02:00:00');

insert into booking_history_appointment (booking_history_id, prison_code, prisoner_number, appointment_date, appointment_type, prison_location_id, start_time, end_time)
values (-1100, 'PVI', 'ABCDEF', '2100-07-25', 'VLB_COURT_MAIN', '926d8f38-7149-4fda-b51f-85abcbcb0d00'::uuid, '12:00', '13:00');

insert into video_booking (video_booking_id, booking_type, status_code, court_id, hearing_type, comments, created_by, created_time, migrated_description)
values (-2000, 'COURT', 'ACTIVE', 409, 'TRIBUNAL', 'comments about the hearing', 'court_user', '2024-07-24T01:00:00', 'Free text court name');

insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, comments, prison_location_id, appointment_date,  start_time, end_time)
values (-2000, 1, 'ABCDEF', 'VLB_COURT_MAIN', 'comments about the hearing', '926d8f38-7149-4fda-b51f-85abcbcb0d00'::uuid, '2100-07-24', '12:00', '13:00');

insert into booking_history(booking_history_id, video_booking_id, history_type, court_id, hearing_type, comments, created_by, created_time)
values (-2000, -2000, 'CREATE',409, 'APPEAL','comments about the hearing', 'court_user', '2024-07-24T01:00:00');

insert into booking_history_appointment (booking_history_id, prison_code, prisoner_number, appointment_date, appointment_type, prison_location_id, start_time, end_time)
values (-2000, 'PVI', 'ABCDEF', '2100-07-24', 'VLB_COURT_MAIN', '926d8f38-7149-4fda-b51f-85abcbcb0d00'::uuid, '12:00', '13:00');
