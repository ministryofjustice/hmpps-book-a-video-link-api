SET REFERENTIAL_INTEGRITY FALSE;

delete from court where code in ('ENABLED', 'NOT_ENABLED');

SET REFERENTIAL_INTEGRITY TRUE;