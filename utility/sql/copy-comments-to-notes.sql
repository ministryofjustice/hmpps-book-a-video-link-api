with updates as (
    select distinct vb.video_booking_id
    from video_booking vb
             join prison_appointment pa on vb.video_booking_id = pa.video_booking_id
    where pa.appointment_date between '2021/01/01' and '2021/12/31'
      and length(vb.comments) > 0
      and vb.notes_for_staff is null
)
update video_booking vb
set notes_for_staff = comments
    from updates u
where u.video_booking_id = vb.video_booking_id and vb.notes_for_staff is null;

with updates as (
    select pa.prison_appointment_id
    from prison_appointment pa
    where pa.appointment_date between '2021/01/01' and '2021/12/31'
      and length(pa.comments) > 0
      and pa.notes_for_staff is null
)
update prison_appointment pa
set notes_for_staff = comments
    from updates u
where u.prison_appointment_id = pa.prison_appointment_id and pa.notes_for_staff is null;
