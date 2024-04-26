-- =============================================
-- Base data
-- =============================================

insert into reference_code (reference_code_id, group_code, code, description,created_by, created_time)
values ( 1, 'COURT_HEARING_TYPE', 'SENTENCING', 'Sentencing', 'TIM', current_timestamp),
       ( 2, 'COURT_HEARING_TYPE', 'TRIAL', 'Trial', 'TIM', current_timestamp),
       ( 3, 'COURT_HEARING_TYPE', 'COMMITTAL',  'Committal hearing', 'TIM', current_timestamp),
       ( 4, 'COURT_HEARING_TYPE', 'PRE_SENTENCE', 'Pre-sentence report', 'TIM', current_timestamp),
       ( 5, 'PROBATION_MEETING_TYPE', 'PSR', 'Pre-sentence report', 'TIM', current_timestamp),
       ( 6, 'PROBATION_MEETING_TYPE', 'RR', 'Recall report', 'TIM', current_timestamp),
       ( 7, 'BOOKING_TYPE', 'COURT', 'Court hearing', 'TIM', current_timestamp),
       ( 8, 'BOOKING_TYPE', 'PROBATION', 'Probation meeting', 'TIM', current_timestamp),
       ( 9, 'STATUS_CODE', 'ACTIVE', 'Active', 'TIM', current_timestamp),
       (10, 'STATUS_CODE', 'EXPIRED', 'Expired', 'TIM', current_timestamp),
       (11, 'STATUS_CODE', 'CANCELLED', 'Cancelled', 'TIM', current_timestamp),
       (12, 'THIRD_PARTY_TYPE', 'INTERPRETER', 'Interpreter', 'TIM', current_timestamp),
       (13, 'THIRD_PARTY_TYPE', 'LEGAL', 'Legal representative', 'TIM', current_timestamp),
       (14, 'THIRD_PARTY_TYPE', 'PROBATION_OFFICER', 'Probation officer', 'TIM', current_timestamp),
       (15, 'THIRD_PARTY_TYPE', 'OTHER_PARTY', 'Other third party', 'TIM', current_timestamp),
       (16, 'APPOINTMENT_TYPE', 'VLB_COURT', 'Video link booking - court', 'TIM', current_timestamp),
       (17, 'APPOINTMENT_TYPE', 'VLB_PROBATION', 'Video link booking - probation', 'TIM', current_timestamp),
       (18, 'EXTERNAL_SYSTEM', 'AA', 'Activities and appointments', 'TIM', current_timestamp),
       (19, 'EXTERNAL_SYSTEM', 'NOMIS', 'NOMIS', 'TIM', current_timestamp),
       (20, 'NOTIFY_REASON', 'NOTIFY_3RD_PARTY', 'Notify third party', 'TIM', current_timestamp),
       (21, 'NOTIFY_REASON', 'NOTIFY_COURT', 'Notify court contact', 'TIM', current_timestamp),
       (22, 'NOTIFY_REASON', 'NOTIFY_PROBATION', 'Notify probation contact', 'TIM', current_timestamp),
       (23, 'NOTIFY_REASON', 'NOTIFY_PRISON', 'Notify prison contact', 'TIM', current_timestamp),
       (24, 'NOTIFY_REASON', 'NOTIFY_OWNER', 'Notify owner', 'TIM', current_timestamp),
       (25, 'HISTORY_TYPE', 'HIST_CREATED', 'Created', 'TIM', current_timestamp),
       (26, 'HISTORY_TYPE', 'HIST_AMENDED', 'Amended', 'TIM', current_timestamp),
       (27, 'HISTORY_TYPE', 'HIST_CANCELLED', 'Cancelled', 'TIM', current_timestamp);

alter sequence if exists reference_code_reference_code_id_seq restart with 28;

---

insert into court (court_id, code, description, enabled, notes, created_by, created_time)
values (1,  'TESTC', 'Test court', true, 'Court used for testing', 'TIM', current_timestamp);

alter sequence if exists court_court_id_seq restart with 2;

---

insert into court_contact (court_contact_id, court_id, name, email, telephone, position, enabled, notes, created_by, created_time)
values (1, 1, 'Tim Hipkins', 't@t.com', '0117 282442', 'Court Clerk', true, '', 'TIM', current_timestamp),
       (2, 1, 'Matt Hipkins', 'm@m.com', '0117 282443', 'Court Clerk', true, '', 'TIM', current_timestamp),
       (3, 1, 'Steve Hipkins', 's@s.com', '0117 282444', 'Court Clerk', true, '', 'TIM', current_timestamp);

alter sequence if exists court_contact_court_contact_id_seq restart with 4;

---

insert into probation_team (probation_team_id, code, description, enabled, notes, created_by, created_time)
values (1, 'TESTP', 'Test probation team', true, 'Probation team for testing', 'TIM', current_timestamp);

alter sequence if exists probation_team_probation_team_id_seq restart with 2;

---

insert into probation_team_contact (probation_team_contact_id, probation_team_id, name, email, telephone, position, enabled, notes, created_by, created_time)
values (1, 1, 'Tim Hipkins', 't@t.com', '0117 282442', 'Court Clerk', true, '', 'TIM', current_timestamp),
       (2, 1, 'Matt Hipkins', 'm@m.com', '0117 282443', 'Court Clerk', true, '', 'TIM', current_timestamp),
       (3, 1, 'Steve Hipkins', 's@s.com', '0117 282444', 'Court Clerk', true, '', 'TIM', current_timestamp);

alter sequence if exists probation_team_contact_probation_team_contact_id_seq restart with 4;

---

insert into prison (prison_id, code, name, description, enabled, notes, created_by, created_time)
values (1, 'MDI', 'Moorland', 'HMP Moorland', true, 'Video rooms: 6  Office hours: 8am-6pm', 'TIM', current_timestamp),
       (2, 'RSI', 'Risley', 'HMP Risley', true, 'Video rooms: 3 Office hours: 9am-5pm', 'TIM', current_timestamp),
       (3, 'BMI', 'Birmingham', 'HMP Birmingham', true, 'Video rooms: 5 Office hours: 8.30am-5.30pm', 'TIM', current_timestamp);

alter sequence if exists prison_prison_id_seq restart with 4;

---

insert into prison_contact (prison_contact_id, prison_id, name, email, telephone, position, enabled, notes, created_by, created_time)
values (1, 1, 'Tim Hopkins', 't@t.com', '0117 282442', 'Video Admin', true, '', 'TIM', current_timestamp),
       (2, 2, 'Matt Hopkins', 'm@m.com', '0117 282443', 'Video Admin', true, '', 'TIM', current_timestamp),
       (3, 3, 'Steve Hopkins', 's@s.com', '0117 282444', 'Video Admin', true, '', 'TIM', current_timestamp);

alter sequence if exists prison_contact_prison_contact_id_seq restart with 4;
