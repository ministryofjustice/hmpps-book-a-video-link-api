-- Rename Manchester City Magistrates to Manchester Magistrates
-- This has been done manually in prod, but just to keep the records straight
update court set description = 'Manchester Magistrates' where code = 'MNCHMC';