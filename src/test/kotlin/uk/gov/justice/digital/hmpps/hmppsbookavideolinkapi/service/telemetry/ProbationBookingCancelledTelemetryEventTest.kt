package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDateTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BLACKPOOL_MC_PPOC
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsEntriesExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class ProbationBookingCancelledTelemetryEventTest {

  private val booking = VideoBooking.newProbationBooking(
    probationTeam = probationTeam(BLACKPOOL_MC_PPOC),
    probationMeetingType = "PSR",
    createdBy = "probation_user",
    createdByPrison = false,
    comments = null,
    videoUrl = "http://booking.created.url",
  ).addAppointment(
    prison = prison(BIRMINGHAM),
    prisonerNumber = "ABC123",
    appointmentType = "VLB_PROBATION",
    date = tomorrow(),
    startTime = LocalTime.of(14, 0),
    endTime = LocalTime.of(15, 0),
    locationId = birminghamLocation.id,
  )

  @Test
  fun `should raise a probation booking cancelled telemetry event cancelled by a probation`() {
    booking.cancel(probationUser())

    with(ProbationBookingCancelledTelemetryEvent.user(booking, probationUser())) {
      eventType isEqualTo "BVLS-probation-booking-cancelled"

      properties() containsEntriesExactlyInAnyOrder mapOf(
        "video_booking_id" to "0",
        "cancelled_by" to "probation",
        "team_code" to BLACKPOOL_MC_PPOC,
        "meeting_type" to "PSR",
        "prison_code" to BIRMINGHAM,
        "location_id" to birminghamLocation.id.toString(),
        "start" to tomorrow().atTime(LocalTime.of(14, 0)).toIsoDateTime(),
        "end" to tomorrow().atTime(LocalTime.of(15, 0)).toIsoDateTime(),
        "cvp_link" to "true",
      )

      metrics() containsEntriesExactlyInAnyOrder mapOf(
        "hoursBeforeStartTime" to hoursBetween(
          booking.amendedTime!!,
          tomorrow().atTime(LocalTime.of(14, 0)),
        ).toDouble(),
      )
    }
  }

  @Test
  fun `should raise a probation booking cancelled telemetry event cancelled by a prison`() {
    booking.cancel(prisonUser())

    with(ProbationBookingCancelledTelemetryEvent.user(booking, prisonUser())) {
      eventType isEqualTo "BVLS-probation-booking-cancelled"

      properties() containsEntriesExactlyInAnyOrder mapOf(
        "video_booking_id" to "0",
        "cancelled_by" to "prison",
        "team_code" to BLACKPOOL_MC_PPOC,
        "meeting_type" to "PSR",
        "prison_code" to BIRMINGHAM,
        "location_id" to birminghamLocation.id.toString(),
        "start" to tomorrow().atTime(LocalTime.of(14, 0)).toIsoDateTime(),
        "end" to tomorrow().atTime(LocalTime.of(15, 0)).toIsoDateTime(),
        "cvp_link" to "true",
      )

      metrics() containsEntriesExactlyInAnyOrder mapOf(
        "hoursBeforeStartTime" to hoursBetween(
          booking.amendedTime!!,
          tomorrow().atTime(LocalTime.of(14, 0)),
        ).toDouble(),
      )
    }
  }

  @Test
  fun `should raise a probation booking cancelled telemetry event cancelled by release`() {
    booking.cancel(prisonUser())

    with(ProbationBookingCancelledTelemetryEvent.released(booking)) {
      eventType isEqualTo "BVLS-probation-booking-cancelled"

      properties() containsEntriesExactlyInAnyOrder mapOf(
        "video_booking_id" to "0",
        "cancelled_by" to "release",
        "team_code" to BLACKPOOL_MC_PPOC,
        "meeting_type" to "PSR",
        "prison_code" to BIRMINGHAM,
        "location_id" to birminghamLocation.id.toString(),
        "start" to tomorrow().atTime(LocalTime.of(14, 0)).toIsoDateTime(),
        "end" to tomorrow().atTime(LocalTime.of(15, 0)).toIsoDateTime(),
        "cvp_link" to "true",
      )

      metrics() containsEntriesExactlyInAnyOrder mapOf(
        "hoursBeforeStartTime" to hoursBetween(
          booking.amendedTime!!,
          tomorrow().atTime(LocalTime.of(14, 0)),
        ).toDouble(),
      )
    }
  }

  @Test
  fun `should raise a probation booking cancelled telemetry event cancelled by transfer`() {
    booking.cancel(prisonUser())

    with(ProbationBookingCancelledTelemetryEvent.transferred(booking)) {
      eventType isEqualTo "BVLS-probation-booking-cancelled"

      properties() containsEntriesExactlyInAnyOrder mapOf(
        "video_booking_id" to "0",
        "cancelled_by" to "transfer",
        "team_code" to BLACKPOOL_MC_PPOC,
        "meeting_type" to "PSR",
        "prison_code" to BIRMINGHAM,
        "location_id" to birminghamLocation.id.toString(),
        "start" to tomorrow().atTime(LocalTime.of(14, 0)).toIsoDateTime(),
        "end" to tomorrow().atTime(LocalTime.of(15, 0)).toIsoDateTime(),
        "cvp_link" to "true",
      )

      metrics() containsEntriesExactlyInAnyOrder mapOf(
        "hoursBeforeStartTime" to hoursBetween(
          booking.amendedTime!!,
          tomorrow().atTime(LocalTime.of(14, 0)),
        ).toDouble(),
      )
    }
  }

  private fun hoursBetween(from: LocalDateTime, to: LocalDateTime) = ChronoUnit.HOURS.between(from, to)

  @Test
  fun `should fail to create event if court booking`() {
    val error = assertThrows<IllegalArgumentException> { ProbationBookingCancelledTelemetryEvent.transferred(courtBooking()) }

    error.message isEqualTo "Cannot create probation cancelled metric, video booking with ID '0' is not a probation booking."
  }

  @Test
  fun `should fail to create event when booking has not been cancelled`() {
    val error = assertThrows<IllegalArgumentException> { ProbationBookingCancelledTelemetryEvent.transferred(probationBooking()) }

    error.message isEqualTo "Cannot create probation cancelled metric, video booking with ID '0' has not been cancelled."
  }
}
