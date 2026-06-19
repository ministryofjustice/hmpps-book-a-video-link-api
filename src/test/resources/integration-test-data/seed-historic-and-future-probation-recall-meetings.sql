-- Historic RR meeting
insert into video_booking (video_booking_id, booking_type, status_code, probation_team_id, probation_meeting_type, notes_for_staff, created_by, created_time)
values (-3000, 'PROBATION', 'ACTIVE', 1, 'RR', 'comments about the meeting', 'probation_user', '2024-01-01T01:00:00');
insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, notes_for_staff, prison_location_id, appointment_date,  start_time, end_time)
values (-3000, 1, 'ABCDEF', 'VLB_PROBATION', 'comments about the meeting', '926d8f38-7149-4fda-b51f-85abcbcb0d00'::uuid, current_date - 2, '16:00', '17:00');
insert into booking_history(booking_history_id, video_booking_id, history_type, probation_team_id, probation_meeting_type, notes_for_staff, created_by, created_time)
values (-3000, -3000, 'CREATE', 1, 'RR','comments about the meeting', 'probation_user', '2024-01-01T01:00:00');
insert into booking_history_appointment (booking_history_id, prison_code, prisoner_number, appointment_date, appointment_type, prison_location_id, start_time, end_time)
values (-3000, 'PVI', 'ABCDEF', current_date - 2, 'VLB_PROBATION', '926d8f38-7149-4fda-b51f-85abcbcb0d00'::uuid, '16:00', '17:00');

-- Historic FTR56 meeting
insert into video_booking (video_booking_id, booking_type, status_code, probation_team_id, probation_meeting_type, notes_for_staff, created_by, created_time)
values (-3100, 'PROBATION', 'ACTIVE', 1, 'FTR56', 'comments about the meeting', 'probation_user', '2024-01-01T01:00:00');
insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, notes_for_staff, prison_location_id, appointment_date,  start_time, end_time)
values (-3100, 1, 'ABCDEF', 'VLB_PROBATION', 'comments about the meeting', '926d8f38-7149-4fda-b51f-85abcbcb0d00'::uuid, current_date - 1, '16:00', '17:00');
insert into booking_history(booking_history_id, video_booking_id, history_type, probation_team_id, probation_meeting_type, notes_for_staff, created_by, created_time)
values (-3100, -3100, 'CREATE', 1, 'FTR56','comments about the meeting', 'probation_user', '2024-01-01T01:00:00');
insert into booking_history_appointment (booking_history_id, prison_code, prisoner_number, appointment_date, appointment_type, prison_location_id, start_time, end_time)
values (-3100, 'PVI', 'ABCDEF', current_date - 1, 'VLB_PROBATION', '926d8f38-7149-4fda-b51f-85abcbcb0d00'::uuid, '16:00', '17:00');

-- Future RR meeting
insert into video_booking (video_booking_id, booking_type, status_code, probation_team_id, probation_meeting_type, notes_for_staff, created_by, created_time)
values (-4000, 'PROBATION', 'ACTIVE', 1, 'RR', 'comments about the meeting', 'probation_user', '2024-01-01T01:00:00');
insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, notes_for_staff, prison_location_id, appointment_date,  start_time, end_time)
values (-4000, 1, 'ABCDEF', 'VLB_PROBATION', 'comments about the meeting', '926d8f38-7149-4fda-b51f-85abcbcb0d00'::uuid, '2099-01-25', '16:00', '17:00');
insert into booking_history(booking_history_id, video_booking_id, history_type, probation_team_id, probation_meeting_type, notes_for_staff, created_by, created_time)
values (-4000, -4000, 'CREATE', 1, 'RR','comments about the meeting', 'probation_user', '2024-01-01T01:00:00');
insert into booking_history_appointment (booking_history_id, prison_code, prisoner_number, appointment_date, appointment_type, prison_location_id, start_time, end_time)
values (-4000, 'PVI', 'ABCDEF', '2099-01-25', 'VLB_PROBATION', '926d8f38-7149-4fda-b51f-85abcbcb0d00'::uuid, '16:00', '17:00');

-- Future FTR56 meeting
insert into video_booking (video_booking_id, booking_type, status_code, probation_team_id, probation_meeting_type, notes_for_staff, created_by, created_time, migrated_description)
values (-4100, 'PROBATION', 'ACTIVE', 28, 'FTR56', 'comments about the meeting', 'probation_user', '2024-01-01T01:00:00', 'Free text probation team name');
insert into prison_appointment (video_booking_id, prison_id, prisoner_number, appointment_type, notes_for_staff, prison_location_id, appointment_date,  start_time, end_time)
values (-4100, 1, 'DEFGHI', 'VLB_PROBATION', 'comments about the meeting', '926d8f38-7149-4fda-b51f-85abcbcb0d00'::uuid, '2099-01-25', '16:00', '17:00');
insert into booking_history(booking_history_id, video_booking_id, history_type, probation_team_id, probation_meeting_type, notes_for_staff, created_by, created_time)
values (-4100, -4100, 'CREATE', 28, 'FTR56','comments about the meeting', 'probation_user', '2024-01-01T01:00:00');
insert into booking_history_appointment (booking_history_id, prison_code, prisoner_number, appointment_date, appointment_type, prison_location_id, start_time, end_time)
values (-4100, 'PVI', 'ABCDEF', '2099-01-25', 'VLB_PROBATION', '926d8f38-7149-4fda-b51f-85abcbcb0d00'::uuid, '16:00', '17:00');
