-- Enable HMP High Down for self-service
update prison set enabled = true where prison_id = 6 and code = 'HOI';
