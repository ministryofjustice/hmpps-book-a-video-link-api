package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDateTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsEntriesExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.risleyLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation2
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation3
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class CourtBookingCreatedTelemetryEventTest {

  @Test
  fun `should raise a court booking created telemetry event created by a court`() {
    val booking = VideoBooking.newCourtBooking(
      court = court(DERBY_JUSTICE_CENTRE),
      hearingType = "APPEAL",
      createdBy = "court_user",
      createdByPrison = false,
      comments = null,
      videoUrl = "http://booking.created.url",
    ).addAppointment(
      prison = prison(WANDSWORTH),
      prisonerNumber = "ABC123",
      appointmentType = "VLB_COURT_PRE",
      date = tomorrow(),
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(10, 0),
      locationKey = wandsworthLocation.key,
    ).addAppointment(
      prison = prison(WANDSWORTH),
      prisonerNumber = "ABC123",
      appointmentType = "VLB_COURT_MAIN",
      date = tomorrow(),
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      locationKey = wandsworthLocation2.key,
    ).addAppointment(
      prison = prison(WANDSWORTH),
      prisonerNumber = "ABC123",
      appointmentType = "VLB_COURT_POST",
      date = tomorrow(),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(12, 0),
      locationKey = wandsworthLocation3.key,
    )

    with(CourtBookingCreatedTelemetryEvent(booking)) {
      eventType isEqualTo "BVLS-court-booking-created"
      properties() containsEntriesExactlyInAnyOrder mapOf(
        "video_booking_id" to "0",
        "created_by" to "court",
        "court_code" to DERBY_JUSTICE_CENTRE,
        "hearing_type" to "APPEAL",
        "prison_code" to WANDSWORTH,
        "pre_location_key" to wandsworthLocation.key,
        "pre_start" to tomorrow().atTime(LocalTime.of(9, 0)).toIsoDateTime(),
        "pre_end" to tomorrow().atTime(LocalTime.of(10, 0)).toIsoDateTime(),
        "main_location_key" to wandsworthLocation2.key,
        "main_start" to tomorrow().atTime(LocalTime.of(10, 0)).toIsoDateTime(),
        "main_end" to tomorrow().atTime(LocalTime.of(11, 0)).toIsoDateTime(),
        "post_location_key" to wandsworthLocation3.key,
        "post_start" to tomorrow().atTime(LocalTime.of(11, 0)).toIsoDateTime(),
        "post_end" to tomorrow().atTime(LocalTime.of(12, 0)).toIsoDateTime(),
        "cvp_link" to "true",
      )

      metrics() containsEntriesExactlyInAnyOrder mapOf(
        "hoursBeforeStartTime" to hoursBetween(
          booking.createdTime,
          tomorrow().atTime(LocalTime.of(10, 0)),
        ).toDouble(),
      )
    }
  }

  @Test
  fun `should raise a court booking created telemetry event created by a prison`() {
    val booking = VideoBooking.newCourtBooking(
      court = court(DERBY_JUSTICE_CENTRE),
      hearingType = "APPEAL",
      createdBy = "prison_user",
      createdByPrison = true,
      comments = null,
      videoUrl = null,
    ).addAppointment(
      prison = prison(RISLEY),
      prisonerNumber = "ABC123",
      appointmentType = "VLB_COURT_MAIN",
      date = tomorrow(),
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(13, 0),
      locationKey = risleyLocation.key,
    )

    with(CourtBookingCreatedTelemetryEvent(booking)) {
      eventType isEqualTo "BVLS-court-booking-created"
      properties() containsEntriesExactlyInAnyOrder mapOf(
        "video_booking_id" to "0",
        "created_by" to "prison",
        "court_code" to DERBY_JUSTICE_CENTRE,
        "hearing_type" to "APPEAL",
        "prison_code" to RISLEY,
        "pre_location_key" to "",
        "pre_start" to "",
        "pre_end" to "",
        "main_location_key" to risleyLocation.key,
        "main_start" to tomorrow().atTime(LocalTime.of(12, 0)).toIsoDateTime(),
        "main_end" to tomorrow().atTime(LocalTime.of(13, 0)).toIsoDateTime(),
        "post_location_key" to "",
        "post_start" to "",
        "post_end" to "",
        "cvp_link" to "false",
      )

      metrics() containsEntriesExactlyInAnyOrder mapOf(
        "hoursBeforeStartTime" to hoursBetween(
          booking.createdTime,
          tomorrow().atTime(LocalTime.of(12, 0)),
        ).toDouble(),
      )
    }
  }

  private fun hoursBetween(from: LocalDateTime, to: LocalDateTime) = ChronoUnit.HOURS.between(from, to)

  @Test
  fun `should fail to create event if probation booking`() {
    val error = assertThrows<IllegalArgumentException> { CourtBookingCreatedTelemetryEvent(probationBooking()) }

    error.message isEqualTo "Cannot create court created metric, video booking with ID '0' is not a court booking."
  }
}
