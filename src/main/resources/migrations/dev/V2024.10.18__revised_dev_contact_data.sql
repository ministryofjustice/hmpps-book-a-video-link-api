truncate table court_contact;
truncate table probation_team_contact;
truncate table prison_contact;

insert into court_contact (court_contact_id, court_id, name, email, telephone, position, enabled, notes, primary_contact, created_by, created_time)
values (1, 1, 'John Hipkins', 'j@j.com', '0117 282442', 'Court Clerk', true, '', true, 'TIM', current_timestamp),
       (2, 1, 'Bob Hipkins', 'b@b.com', '0117 282443', 'Court Clerk', true, '', false, 'TIM', current_timestamp),
       (3, 1, 'Carlos Hipkins', 'c@c.com', '0117 282444', 'Court Clerk', false, '', false, 'TIM', current_timestamp);

alter sequence if exists court_contact_court_contact_id_seq restart with 4;

---

insert into probation_team_contact (probation_team_contact_id, probation_team_id, name, email, telephone, position, enabled, notes, primary_contact, created_by, created_time)
values (1, 1, 'Tim Hipkins', 't@t.com', '0117 282442', 'Probation officer', true, '', true, 'TIM', current_timestamp),
       (2, 1, 'Matt Hipkins', 'm@m.com', '0117 282443', 'Probation officer', true, '', false, 'TIM', current_timestamp),
       (3, 1, 'Steve Hipkins', 's@s.com', '0117 282444', 'Probation officer', false, '', false, 'TIM', current_timestamp);

alter sequence if exists probation_team_contact_probation_team_contact_id_seq restart with 4;

---

insert into prison_contact (prison_contact_id, prison_id, name, email, telephone, position, enabled, notes, primary_contact, created_by, created_time)
values (1, 17, 'Paul Hopkins', 'p@p.com', '0117 282442', 'Video Admin', true, '', true, 'TIM', current_timestamp),
       (2, 17, 'Gary Hopkins', 'g@g.com', '0117 282443', 'Video Admin', true, '', false, 'TIM', current_timestamp),
       (3, 17, 'Zak Hopkins', 'z@z.com', '0117 282444', 'Video Admin', false, '', false, 'TIM', current_timestamp),
       (4, 33, 'Anna Hopkins', 'a@a.com', '0117 282445', 'Video Admin', true, '', true, 'TIM', current_timestamp),
       (5, 16, 'Robbie Hopkins', 'r@r.com', '0117 282446', 'Video Admin', true, '', true, 'TIM', current_timestamp);

alter sequence if exists prison_contact_prison_contact_id_seq restart with 6;
