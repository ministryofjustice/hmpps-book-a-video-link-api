-- Enable HMP New Hall for self-service
update prison
set enabled = true, pick_up_time = 15
where prison_id = 15
  and code = 'NHI';