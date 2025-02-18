-- Enable HMP Liverpool for self-service
update prison set enabled = true where prison_id = 61 and code = 'LPI';
