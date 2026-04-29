-- These SQL scripts were used to check, and set, the probation team
-- flags for court_team and sentence_management_team.
--
-- This is based upon the current team names and also on a checklist of teams provided to us.

-- Get the count of probation court teams
select probation_team_id, code, description, enabled, read_only, court_team, sentence_management_team
from probation_team
where enabled = true
  and (description like '% Crown%' or
       description like '% Magistrates%' or
       description like '% Justice%' or
       description like '% Criminal%' or
       description like '% Law%');

-- Get the count of probation sentence management teams
select probation_team_id, code, description, enabled, read_only, court_team, sentence_management_team
from probation_team
where enabled = true
  and (description like '% Sentence%' or description like '% Field%');

-- Get the total count of all enabled teams
select probation_team_id, description, enabled, court_team, sentence_management_team
from probation_team
where enabled = true;

-- The sum of statement 1 and 2 should equal the count in the 3rd statement.

-- Update the flags for probation court teams
update probation_team
set court_team = true, sentence_management_team = false
where enabled = true
  and (description like '% Crown%' or
       description like '% Magistrates%' or
       description like '% Justice%' or
       description like '% Criminal%' or
       description like '% Law%');

-- Update the flags for probation sentence management teams
update probation_team
set court_team = false, sentence_management_team = true
where enabled = true
  and (description like '% Sentence%' or description like '% Field%');

-- Check the flags are mutually exclusive

-- Both true - should be zero
select probation_team_id, description, enabled, court_team, sentence_management_team
from probation_team
where enabled = true
  and court_team = true
  and sentence_management_team = true;

-- Both false - should be zero
select probation_team_id, description, enabled, court_team, sentence_management_team
from probation_team
where enabled = true
  and court_team = false
  and sentence_management_team = false;

-- Final checks
select description, enabled, court_team, sentence_management_team
from probation_team
where enabled = true and court_team = true;

select description, enabled, court_team, sentence_management_team
from probation_team
where enabled = true and sentence_management_team = true;

-- End
