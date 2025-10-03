ALTER TABLE location_attribute ADD COLUMN blocked_from DATE;
ALTER TABLE location_attribute ADD COLUMN blocked_to DATE;
ALTER TABLE location_attribute DROP COLUMN expected_active_date;
