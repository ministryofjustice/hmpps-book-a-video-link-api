SET REFERENTIAL_INTEGRITY FALSE;

delete from user_probation where user_probation_id in (99000, 99001, 99002);

SET REFERENTIAL_INTEGRITY TRUE;