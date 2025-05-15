ALTER TABLE video_booking ADD COLUMN notes_for_staff VARCHAR(1000);

ALTER TABLE video_booking ADD COLUMN notes_for_prisoners VARCHAR(1000);

ALTER TABLE prison_appointment ADD COLUMN notes_for_prisoners VARCHAR(1000);
