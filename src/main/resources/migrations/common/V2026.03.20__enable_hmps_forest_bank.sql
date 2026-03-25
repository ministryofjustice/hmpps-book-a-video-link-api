-- Enable HMP Forest Bank for self-service (this is a privately operated prison - Sodexo)
update prison set enabled = true where prison_id = 118 and code = 'FBI';
