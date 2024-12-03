-- Uncancel some bookings at HMP Gartree which were cancelled by mistake during a migration of the activities and appointments service
-- Stems from the issue in A&A resolved here https://github.com/ministryofjustice/hmpps-activities-management-api/blob/main/src/main/resources/migrations/prod/V2024.10.31__gartree_appts_undelete.sql
WITH
    updated_booking AS (
        UPDATE video_booking
        SET status_code = 'ACTIVE'
        WHERE video_booking_id IN (164324, 164422)
        RETURNING video_booking_id
    ),
    deleted_booking_history AS (
        DELETE FROM booking_history
        WHERE video_booking_id IN (SELECT video_booking_id FROM updated_booking)
        AND history_type = 'CANCEL'
        RETURNING booking_history_id
    )
DELETE FROM booking_history_appointment
WHERE booking_history_id IN (SELECT booking_history_id FROM deleted_booking_history);
