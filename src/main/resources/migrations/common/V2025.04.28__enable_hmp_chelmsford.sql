-- Enable HMP Chelmsford for self-service
update prison set enabled = true where prison_id = 88 and code = 'CDI';
