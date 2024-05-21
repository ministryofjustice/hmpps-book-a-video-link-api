CREATE OR REPLACE VIEW v_booking_contacts
    AS
    select vlb.video_booking_id, 'COURT' as contact_type, cc.name, cc.position, cc.email, cc.telephone
    from video_booking vlb, court c, court_contact cc
    where vlb.court_id = c.court_id
    and cc.court_id = c.court_id
  UNION
    select vlb.video_booking_id, 'PROBATION' as contact_type, ptc.name, ptc.position, ptc.email, ptc.telephone
    from video_booking vlb, probation_team pt, probation_team_contact ptc
    where vlb.probation_team_id = pt.probation_team_id
    and ptc.probation_team_id = pt.probation_team_id
  UNION
    select vlb.video_booking_id, 'PRISON' as contact_type, pc.name, pc.position, pc.email, pc.telephone
    from video_booking vlb, prison_appointment pa, prison p, prison_contact pc
    where pa.video_booking_id = vlb.video_booking_id
    and pa.prison_code = p.code
    and pc.prison_id = p.prison_id
  UNION
    select vlb.video_booking_id, 'THIRD_PARTY' as contact_type, tp.name, tp.organisation as position, tp.email, tp.telephone
    from video_booking vlb, third_party tp
    where tp.video_booking_id = vlb.video_booking_id;