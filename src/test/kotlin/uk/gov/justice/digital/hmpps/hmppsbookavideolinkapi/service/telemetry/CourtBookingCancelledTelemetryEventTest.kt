package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDateTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsEntriesExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.serviceUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation2
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation3
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class CourtBookingCancelledTelemetryEventTest {

  private val booking = VideoBooking.newCourtBooking(
    court = court(DERBY_JUSTICE_CENTRE),
    hearingType = "APPEAL",
    createdBy = COURT_USER,
    comments = null,
    videoUrl = "http://booking.created.url",
  ).addAppointment(
    prison = prison(WANDSWORTH),
    prisonerNumber = "ABC123",
    appointmentType = "VLB_COURT_PRE",
    date = tomorrow(),
    startTime = LocalTime.of(9, 0),
    endTime = LocalTime.of(10, 0),
    locationId = wandsworthLocation.id,
  ).addAppointment(
    prison = prison(WANDSWORTH),
    prisonerNumber = "ABC123",
    appointmentType = "VLB_COURT_MAIN",
    date = tomorrow(),
    startTime = LocalTime.of(10, 0),
    endTime = LocalTime.of(11, 0),
    locationId = wandsworthLocation2.id,
  ).addAppointment(
    prison = prison(WANDSWORTH),
    prisonerNumber = "ABC123",
    appointmentType = "VLB_COURT_POST",
    date = tomorrow(),
    startTime = LocalTime.of(11, 0),
    endTime = LocalTime.of(12, 0),
    locationId = wandsworthLocation3.id,
  )

  @Test
  fun `should raise a court booking cancelled telemetry event cancelled by a court`() {
    booking.cancel(courtUser())

    with(CourtBookingCancelledTelemetryEvent.user(booking, courtUser())) {
      eventType isEqualTo "BVLS-court-booking-cancelled"

      properties() containsEntriesExactlyInAnyOrder mapOf(
        "video_booking_id" to "0",
        "cancelled_by" to "court",
        "court_code" to DERBY_JUSTICE_CENTRE,
        "hearing_type" to "APPEAL",
        "prison_code" to WANDSWORTH,
        "pre_location_id" to wandsworthLocation.id.toString(),
        "pre_start" to tomorrow().atTime(LocalTime.of(9, 0)).toIsoDateTime(),
        "pre_end" to tomorrow().atTime(LocalTime.of(10, 0)).toIsoDateTime(),
        "main_location_id" to wandsworthLocation2.id.toString(),
        "main_start" to tomorrow().atTime(LocalTime.of(10, 0)).toIsoDateTime(),
        "main_end" to tomorrow().atTime(LocalTime.of(11, 0)).toIsoDateTime(),
        "post_location_id" to wandsworthLocation3.id.toString(),
        "post_start" to tomorrow().atTime(LocalTime.of(11, 0)).toIsoDateTime(),
        "post_end" to tomorrow().atTime(LocalTime.of(12, 0)).toIsoDateTime(),
        "cvp_link" to "true",
      )

      metrics() containsEntriesExactlyInAnyOrder mapOf(
        "hoursBeforeStartTime" to hoursBetween(
          booking.amendedTime!!,
          tomorrow().atTime(LocalTime.of(10, 0)),
        ).toDouble(),
      )
    }
  }

  @Test
  fun `should raise a court booking cancelled telemetry event cancelled by a prison`() {
    booking.cancel(prisonUser())

    with(CourtBookingCancelledTelemetryEvent.user(booking, prisonUser())) {
      eventType isEqualTo "BVLS-court-booking-cancelled"

      properties() containsEntriesExactlyInAnyOrder mapOf(
        "video_booking_id" to "0",
        "cancelled_by" to "prison",
        "court_code" to DERBY_JUSTICE_CENTRE,
        "hearing_type" to "APPEAL",
        "prison_code" to WANDSWORTH,
        "pre_location_id" to wandsworthLocation.id.toString(),
        "pre_start" to tomorrow().atTime(LocalTime.of(9, 0)).toIsoDateTime(),
        "pre_end" to tomorrow().atTime(LocalTime.of(10, 0)).toIsoDateTime(),
        "main_location_id" to wandsworthLocation2.id.toString(),
        "main_start" to tomorrow().atTime(LocalTime.of(10, 0)).toIsoDateTime(),
        "main_end" to tomorrow().atTime(LocalTime.of(11, 0)).toIsoDateTime(),
        "post_location_id" to wandsworthLocation3.id.toString(),
        "post_start" to tomorrow().atTime(LocalTime.of(11, 0)).toIsoDateTime(),
        "post_end" to tomorrow().atTime(LocalTime.of(12, 0)).toIsoDateTime(),
        "cvp_link" to "true",
      )

      metrics() containsEntriesExactlyInAnyOrder mapOf(
        "hoursBeforeStartTime" to hoursBetween(
          booking.amendedTime!!,
          tomorrow().atTime(LocalTime.of(10, 0)),
        ).toDouble(),
      )
    }
  }

  @Test
  fun `should raise a court booking cancelled telemetry event cancelled by service`() {
    booking.cancel(serviceUser())

    with(CourtBookingCancelledTelemetryEvent.user(booking, serviceUser())) {
      eventType isEqualTo "BVLS-court-booking-cancelled"

      properties() containsEntriesExactlyInAnyOrder mapOf(
        "video_booking_id" to "0",
        "cancelled_by" to "prison",
        "court_code" to DERBY_JUSTICE_CENTRE,
        "hearing_type" to "APPEAL",
        "prison_code" to WANDSWORTH,
        "pre_location_id" to wandsworthLocation.id.toString(),
        "pre_start" to tomorrow().atTime(LocalTime.of(9, 0)).toIsoDateTime(),
        "pre_end" to tomorrow().atTime(LocalTime.of(10, 0)).toIsoDateTime(),
        "main_location_id" to wandsworthLocation2.id.toString(),
        "main_start" to tomorrow().atTime(LocalTime.of(10, 0)).toIsoDateTime(),
        "main_end" to tomorrow().atTime(LocalTime.of(11, 0)).toIsoDateTime(),
        "post_location_id" to wandsworthLocation3.id.toString(),
        "post_start" to tomorrow().atTime(LocalTime.of(11, 0)).toIsoDateTime(),
        "post_end" to tomorrow().atTime(LocalTime.of(12, 0)).toIsoDateTime(),
        "cvp_link" to "true",
      )

      metrics() containsEntriesExactlyInAnyOrder mapOf(
        "hoursBeforeStartTime" to hoursBetween(
          booking.amendedTime!!,
          tomorrow().atTime(LocalTime.of(10, 0)),
        ).toDouble(),
      )
    }
  }

  @Test
  fun `should raise a court booking cancelled telemetry event cancelled by release`() {
    booking.cancel(prisonUser())

    with(CourtBookingCancelledTelemetryEvent.released(booking)) {
      eventType isEqualTo "BVLS-court-booking-cancelled"

      properties() containsEntriesExactlyInAnyOrder mapOf(
        "video_booking_id" to "0",
        "cancelled_by" to "release",
        "court_code" to DERBY_JUSTICE_CENTRE,
        "hearing_type" to "APPEAL",
        "prison_code" to WANDSWORTH,
        "pre_location_id" to wandsworthLocation.id.toString(),
        "pre_start" to tomorrow().atTime(LocalTime.of(9, 0)).toIsoDateTime(),
        "pre_end" to tomorrow().atTime(LocalTime.of(10, 0)).toIsoDateTime(),
        "main_location_id" to wandsworthLocation2.id.toString(),
        "main_start" to tomorrow().atTime(LocalTime.of(10, 0)).toIsoDateTime(),
        "main_end" to tomorrow().atTime(LocalTime.of(11, 0)).toIsoDateTime(),
        "post_location_id" to wandsworthLocation3.id.toString(),
        "post_start" to tomorrow().atTime(LocalTime.of(11, 0)).toIsoDateTime(),
        "post_end" to tomorrow().atTime(LocalTime.of(12, 0)).toIsoDateTime(),
        "cvp_link" to "true",
      )

      metrics() containsEntriesExactlyInAnyOrder mapOf(
        "hoursBeforeStartTime" to hoursBetween(
          booking.amendedTime!!,
          tomorrow().atTime(LocalTime.of(10, 0)),
        ).toDouble(),
      )
    }
  }

  @Test
  fun `should raise a court booking cancelled telemetry event cancelled by transfer`() {
    booking.cancel(prisonUser())

    with(CourtBookingCancelledTelemetryEvent.transferred(booking)) {
      eventType isEqualTo "BVLS-court-booking-cancelled"

      properties() containsEntriesExactlyInAnyOrder mapOf(
        "video_booking_id" to "0",
        "cancelled_by" to "transfer",
        "court_code" to DERBY_JUSTICE_CENTRE,
        "hearing_type" to "APPEAL",
        "prison_code" to WANDSWORTH,
        "pre_location_id" to wandsworthLocation.id.toString(),
        "pre_start" to tomorrow().atTime(LocalTime.of(9, 0)).toIsoDateTime(),
        "pre_end" to tomorrow().atTime(LocalTime.of(10, 0)).toIsoDateTime(),
        "main_location_id" to wandsworthLocation2.id.toString(),
        "main_start" to tomorrow().atTime(LocalTime.of(10, 0)).toIsoDateTime(),
        "main_end" to tomorrow().atTime(LocalTime.of(11, 0)).toIsoDateTime(),
        "post_location_id" to wandsworthLocation3.id.toString(),
        "post_start" to tomorrow().atTime(LocalTime.of(11, 0)).toIsoDateTime(),
        "post_end" to tomorrow().atTime(LocalTime.of(12, 0)).toIsoDateTime(),
        "cvp_link" to "true",
      )

      metrics() containsEntriesExactlyInAnyOrder mapOf(
        "hoursBeforeStartTime" to hoursBetween(
          booking.amendedTime!!,
          tomorrow().atTime(LocalTime.of(10, 0)),
        ).toDouble(),
      )
    }
  }

  private fun hoursBetween(from: LocalDateTime, to: LocalDateTime) = ChronoUnit.HOURS.between(from, to)

  @Test
  fun `should fail to create event if probation booking`() {
    val error = assertThrows<IllegalArgumentException> { CourtBookingCancelledTelemetryEvent.transferred(probationBooking()) }

    error.message isEqualTo "Cannot create court cancelled metric, video booking with ID '0' is not a court booking."
  }

  @Test
  fun `should fail to create event when booking has not been cancelled`() {
    val error = assertThrows<IllegalArgumentException> { CourtBookingCancelledTelemetryEvent.transferred(courtBooking()) }

    error.message isEqualTo "Cannot create court cancelled metric, video booking with ID '0' has not been cancelled."
  }
}
