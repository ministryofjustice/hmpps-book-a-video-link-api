ALTER TABLE probation_team ADD COLUMN court_team BOOLEAN NOT NULL default false;
ALTER TABLE probation_team ADD COLUMN sentence_management_team BOOLEAN NOT NULL default false;
