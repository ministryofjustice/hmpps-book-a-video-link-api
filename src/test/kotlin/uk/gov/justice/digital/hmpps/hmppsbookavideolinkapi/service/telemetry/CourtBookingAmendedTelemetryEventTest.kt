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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.risleyLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation2
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation3
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit.HOURS

class CourtBookingAmendedTelemetryEventTest {

  private val amendedAt = LocalDateTime.now().plusMinutes(1)

  @Test
  fun `should raise a court booking amended telemetry event amended by a court`() {
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
    ).apply {
      amendedTime = amendedAt
      amendedBy = courtUser().username
    }

    with(CourtBookingAmendedTelemetryEvent(booking, courtUser())) {
      eventType isEqualTo "BVLS-court-booking-amended"
      properties() containsEntriesExactlyInAnyOrder mapOf(
        "video_booking_id" to "0",
        "amended_by" to "court",
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
          amendedAt,
          tomorrow().atTime(LocalTime.of(10, 0)),
        ).toDouble(),
      )
    }
  }

  @Test
  fun `should raise a court booking amended telemetry event amended by a prison`() {
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
      locationId = risleyLocation.id,
    ).apply {
      amendedTime = amendedAt
      amendedBy = prisonUser().username
    }

    with(CourtBookingAmendedTelemetryEvent(booking, prisonUser())) {
      eventType isEqualTo "BVLS-court-booking-amended"
      properties() containsEntriesExactlyInAnyOrder mapOf(
        "video_booking_id" to "0",
        "amended_by" to "prison",
        "court_code" to DERBY_JUSTICE_CENTRE,
        "hearing_type" to "APPEAL",
        "prison_code" to RISLEY,
        "pre_location_id" to "",
        "pre_start" to "",
        "pre_end" to "",
        "main_location_id" to risleyLocation.id.toString(),
        "main_start" to tomorrow().atTime(LocalTime.of(12, 0)).toIsoDateTime(),
        "main_end" to tomorrow().atTime(LocalTime.of(13, 0)).toIsoDateTime(),
        "post_location_id" to "",
        "post_start" to "",
        "post_end" to "",
        "cvp_link" to "false",
      )

      metrics() containsEntriesExactlyInAnyOrder mapOf(
        "hoursBeforeStartTime" to hoursBetween(
          amendedAt,
          tomorrow().atTime(LocalTime.of(12, 0)),
        ).toDouble(),
      )
    }
  }

  private fun hoursBetween(from: LocalDateTime, to: LocalDateTime) = HOURS.between(from, to)

  @Test
  fun `should fail to create event if probation booking`() {
    val error = assertThrows<IllegalArgumentException> { CourtBookingAmendedTelemetryEvent(probationBooking(), probationUser()) }

    error.message isEqualTo "Cannot create court amended metric, video booking with ID '0' is not a court booking."
  }

  @Test
  fun `should fail to create event when booking has not been amended`() {
    val error = assertThrows<IllegalArgumentException> { CourtBookingAmendedTelemetryEvent(courtBooking(), courtUser()) }

    error.message isEqualTo "Cannot create court amended metric, video booking with ID '0' has not been amended."
  }
}
