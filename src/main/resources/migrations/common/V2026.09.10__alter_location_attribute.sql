ALTER TABLE location_attribute ADD COLUMN room_area VARCHAR(30);

UPDATE location_attribute SET room_area = 'COURT_PROBATION';