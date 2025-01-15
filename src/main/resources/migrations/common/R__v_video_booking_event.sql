DROP VIEW IF EXISTS v_video_booking_event;

CREATE OR REPLACE VIEW v_video_booking_event
AS
select
  bh.booking_history_id as event_id,
  vlb.video_booking_id,
  vlb.created_time::date as date_of_booking,
  bh.created_time as timestamp,
  bh.history_type as event_type,
  bha_main.prison_code,
  COALESCE(vlb.migrated_description, c.description) as court_description,
  c.code as court_code,
  null as probation_team_description,
  null as probation_team_code,
  vlb.created_by_prison,
  bha_pre.prison_location_id as pre_location_id,
  bha_pre.appointment_date as pre_date,
  bha_pre.start_time as pre_start_time,
  bha_pre.end_time as pre_end_time,
  bha_main.prison_location_id as main_location_id,
  bha_main.appointment_date as main_date,
  bha_main.start_time as main_start_time,
  bha_main.end_time as main_end_time,
  bha_post.prison_location_id as post_location_id,
  bha_post.appointment_date as post_date,
  bha_post.start_time as post_start_time,
  bha_post.end_time as post_end_time,
  true as court_booking,
  rc.description as type,
  bh.created_by as "user"
from video_booking vlb
  join booking_history bh on bh.video_booking_id = vlb.video_booking_id
  join booking_history_appointment bha_main on bha_main.booking_history_id = bh.booking_history_id and bha_main.appointment_type = 'VLB_COURT_MAIN'
  left join booking_history_appointment bha_pre on bha_pre.booking_history_id = bh.booking_history_id and bha_pre.appointment_type = 'VLB_COURT_PRE'
  left join booking_history_appointment bha_post on bha_post.booking_history_id = bh.booking_history_id and bha_post.appointment_type = 'VLB_COURT_POST'
  join court c on c.court_id = vlb.court_id
  join reference_code rc on rc.group_code = 'COURT_HEARING_TYPE' and rc.code = vlb.hearing_type
where vlb.court_id is not null
UNION
select
    bh.booking_history_id as event_id,
    vlb.video_booking_id,
    vlb.created_time::date as date_of_booking,
    bh.created_time as timestamp,
    bh.history_type as event_type,
    bha_main.prison_code,
    null as court_description,
    null as court_code,
    COALESCE(vlb.migrated_description, p.description) as probation_team_description,
    p.code as probation_team_code,
    vlb.created_by_prison,
    null as pre_location_key,
    null as pre_date,
    null as pre_start_time,
    null as pre_end_time,
    bha_main.prison_location_id as main_location_id,
    bha_main.appointment_date as main_date,
    bha_main.start_time as main_start_time,
    bha_main.end_time as main_end_time,
    null as post_location_key,
    null as post_date,
    null as post_start_time,
    null as post_end_time,
    false as court_booking,
    rc.description as type,
    bh.created_by as "user"
from video_booking vlb
  join booking_history bh on bh.video_booking_id = vlb.video_booking_id
  join booking_history_appointment bha_main on bha_main.booking_history_id = bh.booking_history_id and bha_main.appointment_type = 'VLB_PROBATION'
  join probation_team p on p.probation_team_id = vlb.probation_team_id
  join reference_code rc on rc.group_code = 'PROBATION_MEETING_TYPE' and rc.code = vlb.probation_meeting_type
where vlb.probation_team_id is not null;
