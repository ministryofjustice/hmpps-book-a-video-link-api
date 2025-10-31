delete from reference_code where group_code = 'PROBATION_MEETING_TYPE';

insert into reference_code (group_code, code, description,created_by, created_time, enabled, display_sequence)
values
  ('PROBATION_MEETING_TYPE', 'PSR', 'Pre-sentence report (PSR)', 'MATT', current_timestamp, true, 1),
  ('PROBATION_MEETING_TYPE', 'RR', 'Recall report (PRARR - parts B or C)', 'MATT', current_timestamp, true, 2),
  ('PROBATION_MEETING_TYPE', 'PR', 'Parole Report (PAROM)', 'MATT', current_timestamp, true, 3),
  ('PROBATION_MEETING_TYPE', 'HDC', 'HDC (home detention curfew)', 'MATT', current_timestamp, true, 4),
  ('PROBATION_MEETING_TYPE', 'OASYS', 'OAYSys', 'MATT', current_timestamp, true, 5),
  ('PROBATION_MEETING_TYPE', 'MALRAP', 'Multi-agency lifer risk assessment panel (MALRAP)', 'MATT', current_timestamp, true, 6),
  ('PROBATION_MEETING_TYPE', 'PRP', 'Pre-release planning', 'MATT', current_timestamp, true, 7),
  ('PROBATION_MEETING_TYPE', 'IOM', 'Integrated offender management (IOM)', 'MATT', current_timestamp, true, 8),
  ('PROBATION_MEETING_TYPE', 'RTSCR', 'Response to supervision (court report)', 'MATT', current_timestamp, true, 9),
  ('PROBATION_MEETING_TYPE', 'BR', 'Bail report', 'MATT', current_timestamp, true, 10),
  ('PROBATION_MEETING_TYPE', 'RCAT', 'R-CAT (recategorisation) assessments', 'MATT', current_timestamp, true, 11),
  ('PROBATION_MEETING_TYPE', 'ROTL', 'ROTL (release on temporary licence)', 'MATT', current_timestamp, true, 12),
  ('PROBATION_MEETING_TYPE', 'OTHER', 'Other', 'MATT', current_timestamp, true, 13),
  ('PROBATION_MEETING_TYPE', 'UNKNOWN', 'Unknown', 'MATT', current_timestamp, true, 14);
