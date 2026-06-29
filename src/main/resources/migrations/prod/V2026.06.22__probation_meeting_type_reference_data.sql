update reference_code
   set enabled = false, display_sequence = 99
 where group_code = 'PROBATION_MEETING_TYPE' and code in ('FTR56', 'RR') and enabled = true;

insert into reference_code (group_code, code, description,created_by, created_time, enabled, display_sequence)
values ('PROBATION_MEETING_TYPE', 'RECALL', 'Recall (PRARR part B, part C or FTR56)', 'MATT', current_timestamp, true, 2) ON CONFLICT DO NOTHING;
