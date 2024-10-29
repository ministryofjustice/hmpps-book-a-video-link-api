CREATE OR REPLACE VIEW v_video_appointments
AS
select
  pa.prison_appointment_id,
  vlb.video_booking_id,
  vlb.booking_type,
  vlb.status_code,
  c.code as court_code,
  pt.code as probation_team_code,
  p.code as prison_code,
  pa.prisoner_number,
  pa.appointment_type,
  pa.prison_loc_uuid,
  pa.appointment_date,
  pa.start_time,
  pa.end_time
from video_booking vlb
  left join court c on c.court_id = vlb.court_id and c.enabled = true
  left join probation_team pt on pt.probation_team_id = vlb.probation_team_id and pt.enabled = true
  join prison_appointment pa on pa.video_booking_id = vlb.video_booking_id
  join prison p on p.prison_id = pa.prison_id;
