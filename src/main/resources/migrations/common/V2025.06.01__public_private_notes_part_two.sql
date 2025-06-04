ALTER TABLE booking_history ADD COLUMN notes_for_prisoners VARCHAR(1000);
ALTER TABLE booking_history ADD COLUMN notes_for_staff VARCHAR(1000);
ALTER TABLE prison_appointment ADD COLUMN notes_for_staff VARCHAR(1000);