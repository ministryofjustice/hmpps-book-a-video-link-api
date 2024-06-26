SET REFERENTIAL_INTEGRITY FALSE;

--Common
truncate table notification restart identity;
truncate table booking_history_appointment restart identity;
truncate table booking_history restart identity;
truncate table prison_appointment restart identity;
truncate table video_booking restart identity;

SET REFERENTIAL_INTEGRITY TRUE;
