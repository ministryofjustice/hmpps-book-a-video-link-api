-- Enable HMP Winchester for self-service
update prison set enabled = true where prison_id = 29 and code = 'WCI';
