-- Update the name of a court - was incomplete
update court
set description = 'Torquay and Newton Abbot County'
where court_id = 373
and name = 'TORQCC';