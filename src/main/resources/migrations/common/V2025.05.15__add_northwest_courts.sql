-- These are new North West region courts - which already exist in the court register.
insert into court (code, description, enabled, read_only, notes, created_by, created_time)
values ('WIGNCT', 'Wigan County', true, false, null, 'TIM', current_timestamp),
       ('MANCCT', 'Manchester Civil', true, false, null, 'TIM', current_timestamp),
       ('STHECT', 'St Helens County', true, false, null, 'TIM', current_timestamp),
       ('BRKNCT', 'Birkenhead County', true, false, null, 'TIM', current_timestamp),
       ('BLKPCT', 'Blackpool County', true, false, null, 'TIM', current_timestamp),
       ('BRRWCT', 'Barrow County', true, false, null, 'TIM', current_timestamp),
       ('BURNCT', 'Burnley County', true, false, null, 'TIM', current_timestamp),
       ('LANCCT', 'Lancaster County', true, false, null, 'TIM', current_timestamp),
       ('WAKFCT', 'Wakefield County', true, false, null, 'TIM', current_timestamp);

-- These are new North West region courts - which do not exist in the court register, and have made-up codes.
-- Add a note to say they are not in the register so we can identify later.
insert into court (code, description, enabled, read_only, notes, created_by, created_time)
values ('BOLSSC', 'Bolton Social Security and Child Support Tribunal', true, false, 'Not in register', 'TIM', current_timestamp),
       ('LIVSSC', 'Liverpool Social Security and Child Support Tribunal', true, false, 'Not in register', 'TIM', current_timestamp),
       ('CHSTFA', 'Chester Family', true, false, 'Not in register', 'TIM', current_timestamp),
       ('BLASSC', 'Blackburn Social Security and Child Support Tribunal', true, false, 'Not in register', 'TIM', current_timestamp),
       ('BLKSSC', 'Blackpool Social Security and Child Support Tribunal', true, false, 'Not in register', 'TIM', current_timestamp),
       ('BURSSC', 'Burnley Social Security and Child Support Tribunal', true, false, 'Not in register', 'TIM', current_timestamp),
       ('LIVCIV', 'Liverpool Civil', true, false, 'Not in register', 'TIM', current_timestamp),
       ('LIVFAM', 'Liverpool Family', true, false, 'Not in register', 'TIM', current_timestamp),
       ('CHSCIV', 'Chester Civil', true, false, 'Not in register', 'TIM', current_timestamp),
       ('BLAFAM', 'Blackburn Family', true, false, 'Not in register', 'TIM', current_timestamp),
       ('PRSFAM', 'Preston Family', true, false, 'Not in register', 'TIM', current_timestamp),
       ('REEFAM', 'Reedley Family', true, false, 'Not in register', 'TIM', current_timestamp),
       ('ROCSSC', 'Rochdale Social Security and Child Support Tribunal', true, false, 'Not in register', 'TIM', current_timestamp),
       ('STKPCC', 'Stockport County', true, false, 'Not in register', 'TIM', current_timestamp),
       ('MANFAM', 'Manchester Family', true, false, 'Not in register', 'TIM', current_timestamp),
       ('PRESCY', 'Preston County', true, false, 'Not in register - should be PRESCT but exists already', 'TIM', current_timestamp),
       ('WORMCC', 'West Cumbria County (Workington)', true, false, 'Not in register - should be WORNMC but exists already', 'TIM', current_timestamp),
       ('CRWCFM', 'Crewe Civil and Family', true, false, 'Not in register', 'TIM', current_timestamp);

-- These are existing North West courts in BVLS which are not yet enabled so
-- update them to enabled and remove the notes which contain info about not being enabled.
-- They all match their codes in the court register.
update court
  set enabled = true,
      notes = null
where code in (
  'BOLTCC',
  'BOLTMC',
  'MNCCCC',
  'MNCMCC',
  'MNCHMC',
  'STOCMC',
  'WIGNMC'
);

-- Correct the spelling for Tameside Magistrates and enable the court.
-- The code is mismatched with court register which has ASHTMC
update court
set description = 'Tameside Magistrates',
    enabled = true,
    notes = 'In register with a different code ASHTMC'
where code = 'THAMMC' and description = 'Thameside Magistrates';

-- Update North West Civil and Family to not enabled and read only true
-- This is a virtual court which temporarily stood in for the above new courts
-- It was previously enabled = false, read_only = false
update court
set read_only = true,
    notes = 'Virtual court for NW now redundant - not in register'
where code = 'NWCFAC';
