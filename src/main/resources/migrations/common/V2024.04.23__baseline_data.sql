-- =============================================
-- Base data
-- =============================================

insert into reference_code (reference_code_id, group_code, code, description,created_by, created_time)
values ( 1, 'COURT_HEARING_TYPE', 'APPEAL', 'Appeal', 'TIM', current_timestamp),
       ( 2, 'COURT_HEARING_TYPE', 'APPLICATION', 'Application', 'TIM', current_timestamp),
       ( 3, 'COURT_HEARING_TYPE', 'BACKER',  'Backer trial', 'TIM', current_timestamp),
       ( 4, 'COURT_HEARING_TYPE', 'BAIL', 'Bail', 'TIM', current_timestamp),
       ( 5, 'COURT_HEARING_TYPE', 'CIVIL', 'Civil', 'TIM', current_timestamp),
       ( 6, 'COURT_HEARING_TYPE', 'CSE', '', 'Committal for sentence (CSE)', current_timestamp),
       ( 7, 'COURT_HEARING_TYPE', 'CTA', 'Custody time limit application (CTA)', 'TIM', current_timestamp),
       ( 8, 'COURT_HEARING_TYPE', 'IMMIGRATION_DEPORTATION', 'Immigration/deportation', 'TIM', current_timestamp),
       ( 9, 'COURT_HEARING_TYPE', 'FAMILY', 'Family', 'TIM', current_timestamp),
       (10, 'COURT_HEARING_TYPE', 'TRIAL', 'Trial', 'TIM', current_timestamp),
       (11, 'COURT_HEARING_TYPE', 'FCMH', 'Further case management hearing (FCMH)', 'TIM', current_timestamp),
       (12, 'COURT_HEARING_TYPE', 'FTR', 'Future trial review hearing (FTR)', 'TIM', current_timestamp),
       (13, 'COURT_HEARING_TYPE', 'GRH', 'Ground rules hearing (GRH)', 'TIM', current_timestamp),
       (14, 'COURT_HEARING_TYPE', 'MDA', 'Mention hearing (MDA)', 'TIM', current_timestamp),
       (15, 'COURT_HEARING_TYPE', 'MEF', 'Mention to fix (MEF)', 'TIM', current_timestamp),
       (16, 'COURT_HEARING_TYPE', 'NEWTON', 'Newton hearing', 'TIM', current_timestamp),
       (17, 'COURT_HEARING_TYPE', 'PLE', 'Plea hearing (PLE)', 'TIM', current_timestamp),
       (18, 'COURT_HEARING_TYPE', 'PTPH', 'Plea trial and preparation hearing (PTPH)', 'TIM', current_timestamp),
       (19, 'COURT_HEARING_TYPE', 'PTR', 'Pre-trial review (PTR)', 'TIM', current_timestamp),
       (20, 'COURT_HEARING_TYPE', 'POCA', 'Proceeds of crime appliction (POCA)', 'TIM', current_timestamp),
       (21, 'COURT_HEARING_TYPE', 'REMAND', 'Remand hearing', 'TIM', current_timestamp),
       (22, 'COURT_HEARING_TYPE', 'SECTION_28', 'Section 28', 'TIM', current_timestamp),
       (23, 'COURT_HEARING_TYPE', 'SEN', 'Sentence (SEN)', 'TIM', current_timestamp),
       (24, 'COURT_HEARING_TYPE', 'TRIBUNAL', 'Tribunal', 'TIM', current_timestamp),
       (25, 'COURT_HEARING_TYPE', 'OTHER', 'Other', 'TIM', current_timestamp),
       (26, 'PROBATION_MEETING_TYPE', 'PSR', 'Pre-sentence report', 'TIM', current_timestamp),
       (27, 'PROBATION_MEETING_TYPE', 'RR', 'Recall report', 'TIM', current_timestamp),
       (28, 'BOOKING_TYPE', 'COURT', 'Court hearing', 'TIM', current_timestamp),
       (29, 'BOOKING_TYPE', 'PROBATION', 'Probation meeting', 'TIM', current_timestamp),
       (30, 'STATUS_CODE', 'ACTIVE', 'Active', 'TIM', current_timestamp),
       (31, 'STATUS_CODE', 'EXPIRED', 'Expired', 'TIM', current_timestamp),
       (32, 'STATUS_CODE', 'CANCELLED', 'Cancelled', 'TIM', current_timestamp),
       (33, 'THIRD_PARTY_TYPE', 'INTERPRETER', 'Interpreter', 'TIM', current_timestamp),
       (34, 'THIRD_PARTY_TYPE', 'LEGAL', 'Legal representative', 'TIM', current_timestamp),
       (35, 'THIRD_PARTY_TYPE', 'PROBATION_OFFICER', 'Probation officer', 'TIM', current_timestamp),
       (36, 'THIRD_PARTY_TYPE', 'OTHER_PARTY', 'Other third party', 'TIM', current_timestamp),
       (37, 'APPOINTMENT_TYPE', 'VLB_COURT_PRE', 'Video link booking - pre conference', 'TIM', current_timestamp),
       (38, 'APPOINTMENT_TYPE', 'VLB_COURT_MAIN', 'Video link booking - hearing', 'TIM', current_timestamp),
       (39, 'APPOINTMENT_TYPE', 'VLB_COURT_POST', 'Video link booking - post conference', 'TIM', current_timestamp),
       (40, 'APPOINTMENT_TYPE', 'VLB_PROBATION', 'Video link booking - probation', 'TIM', current_timestamp),
       (41, 'EXTERNAL_SYSTEM', 'AA', 'Activities and appointments', 'TIM', current_timestamp),
       (42, 'EXTERNAL_SYSTEM', 'NOMIS', 'NOMIS', 'TIM', current_timestamp),
       (43, 'NOTIFY_REASON', 'NOTIFY_3RD_PARTY', 'Notify third party', 'TIM', current_timestamp),
       (44, 'NOTIFY_REASON', 'NOTIFY_COURT', 'Notify court contact', 'TIM', current_timestamp),
       (45, 'NOTIFY_REASON', 'NOTIFY_PROBATION', 'Notify probation contact', 'TIM', current_timestamp),
       (46, 'NOTIFY_REASON', 'NOTIFY_PRISON', 'Notify prison contact', 'TIM', current_timestamp),
       (47, 'NOTIFY_REASON', 'NOTIFY_OWNER', 'Notify owner', 'TIM', current_timestamp),
       (48, 'HISTORY_TYPE', 'HIST_CREATED', 'Created', 'TIM', current_timestamp),
       (49, 'HISTORY_TYPE', 'HIST_AMENDED', 'Amended', 'TIM', current_timestamp),
       (50, 'HISTORY_TYPE', 'HIST_CANCELLED', 'Cancelled', 'TIM', current_timestamp);

alter sequence if exists reference_code_reference_code_id_seq restart with 49;

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
