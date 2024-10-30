CREATE OR REPLACE VIEW v_prison_schedule
AS
select
    vlb.video_booking_id,
    pa.prison_appointment_id,
    vlb.booking_type,
    vlb.status_code,
    vlb.hearing_type,
    rc1.description as hearing_type_description,
    vlb.probation_meeting_type,
    rc4.description as probation_meeting_type_description,
    vlb.video_url,
    vlb.comments as booking_comments,
    vlb.created_by_prison,
    c.court_id,
    c.code as court_code,
    c.description as court_description,
    pt.probation_team_id,
    pt.code as probation_team_code,
    pt.description as probation_team_description,
    p.code as prison_code,
    p.name as prison_name,
    pa.prisoner_number,
    pa.appointment_type,
    rc3.description as appointment_type_description,
    pa.comments as appointment_comments,
    pa.prison_location_id,
    pa.appointment_date,
    pa.start_time,
    pa.end_time
from video_booking vlb
  left join court c on c.court_id = vlb.court_id and c.enabled = true
  left join probation_team pt on pt.probation_team_id = vlb.probation_team_id and pt.enabled = true
  join prison_appointment pa on pa.video_booking_id = vlb.video_booking_id
  join prison p on p.prison_id = pa.prison_id
  left join reference_code rc1 on rc1.group_code = 'COURT_HEARING_TYPE' and rc1.code = vlb.hearing_type
  left join reference_code rc2 on rc2.group_code = 'BOOKING_TYPE' and rc2.code = vlb.booking_type
  left join reference_code rc3 on rc3.group_code = 'APPOINTMENT_TYPE' and rc3.code = pa.appointment_type
  left join reference_code rc4 on rc4.group_code = 'PROBATION_MEETING_TYPE' and rc4.code = vlb.probation_meeting_type;
