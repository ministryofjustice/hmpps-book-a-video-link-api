insert into video_booking (video_booking_id, booking_type, status_code, court_id, hearing_type, comments, created_by, created_time, hmcts_number)
values (-2000, 'COURT', 'ACTIVE', 1, 'TRIBUNAL', 'comments about the hearing', 'court_user', '2024-01-01T01:00:00', '54321');

insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, comments, prison_location_id, appointment_date,  start_time, end_time)
values (-2000, 1, 'ABCDEF', 'VLB_COURT_MAIN', 'comments about the hearing', '926d8f38-7149-4fda-b51f-85abcbcb0d00'::uuid, '2099-01-24', '12:00', '13:00');

insert into booking_history(booking_history_id, video_booking_id, history_type, court_id, hearing_type, comments, created_by, created_time)
values (-2000, -2000, 'CREATE', 1, 'TRIBUNAL','comments about the hearing', 'court_user', '2024-01-01T01:00:00');

insert into booking_history_appointment (booking_history_id, prison_code, prisoner_number, appointment_date, appointment_type, prison_location_id, start_time, end_time)
values (-2000, 'PVI', 'ABCDEF', '2099-01-24', 'VLB_COURT_MAIN', '926d8f38-7149-4fda-b51f-85abcbcb0d00'::uuid, '12:00', '13:00');

insert into video_booking (video_booking_id, booking_type, status_code, probation_team_id, probation_meeting_type, comments, created_by, created_time)
values (-3000, 'PROBATION', 'ACTIVE', 1, 'PSR', 'comments about the meeting', 'probation_user', '2024-01-01T01:00:00');

insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, comments, prison_location_id, appointment_date,  start_time, end_time)
values (-3000, 1, 'ABCDEF', 'VLB_PROBATION', 'comments about the meeting', '926d8f38-7149-4fda-b51f-85abcbcb0d00'::uuid, '2099-01-24', '16:00', '17:00');

insert into booking_history(booking_history_id, video_booking_id, history_type, probation_team_id, probation_meeting_type, comments, created_by, created_time)
values (-3000, -3000, 'CREATE', 1, 'PSR','comments about the meeting', 'probation_user', '2024-01-01T01:00:00');

insert into booking_history_appointment (booking_history_id, prison_code, prisoner_number, appointment_date, appointment_type, prison_location_id, start_time, end_time)
values (-3000, 'PVI', 'ABCDEF', '2099-01-24', 'VLB_PROBATION', '926d8f38-7149-4fda-b51f-85abcbcb0d00'::uuid, '16:00', '17:00');
