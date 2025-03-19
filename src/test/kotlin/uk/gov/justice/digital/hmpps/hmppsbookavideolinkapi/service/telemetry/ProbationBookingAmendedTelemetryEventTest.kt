package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDateTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BLACKPOOL_MC_PPOC
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
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

class ProbationBookingAmendedTelemetryEventTest {

  private val amendedAt = LocalDateTime.now().plusMinutes(1)

  @Test
  fun `should raise a probation booking amended telemetry event by probation`() {
    val booking = VideoBooking.newProbationBooking(
      probationTeam = probationTeam(BLACKPOOL_MC_PPOC),
      probationMeetingType = "PSR",
      createdBy = PROBATION_USER,
      comments = null,
    ).addAppointment(
      prison = prison(BIRMINGHAM),
      prisonerNumber = "ABC123",
      appointmentType = "VLB_PROBATION",
      date = tomorrow(),
      startTime = LocalTime.of(14, 0),
      endTime = LocalTime.of(15, 0),
      locationId = birminghamLocation.id,
    ).apply {
      amendedTime = amendedAt
      amendedBy = probationUser().username
    }

    with(ProbationBookingAmendedTelemetryEvent(booking, probationUser())) {
      eventType isEqualTo "BVLS-probation-booking-amended"
      properties() containsEntriesExactlyInAnyOrder mapOf(
        "video_booking_id" to "0",
        "amended_by" to "probation",
        "team_code" to BLACKPOOL_MC_PPOC,
        "meeting_type" to "PSR",
        "prison_code" to BIRMINGHAM,
        "location_id" to birminghamLocation.id.toString(),
        "start" to tomorrow().atTime(LocalTime.of(14, 0)).toIsoDateTime(),
        "end" to tomorrow().atTime(LocalTime.of(15, 0)).toIsoDateTime(),
      )

      metrics() containsEntriesExactlyInAnyOrder mapOf(
        "hoursBeforeStartTime" to hoursBetween(
          amendedAt,
          tomorrow().atTime(LocalTime.of(14, 0)),
        ).toDouble(),
      )
    }
  }

  @Test
  fun `should raise a probation booking amended telemetry event by prison`() {
    val booking = VideoBooking.newProbationBooking(
      probationTeam = probationTeam(BLACKPOOL_MC_PPOC),
      probationMeetingType = "PSR",
      createdBy = PROBATION_USER,
      comments = null,
    ).addAppointment(
      prison = prison(BIRMINGHAM),
      prisonerNumber = "ABC123",
      appointmentType = "VLB_PROBATION",
      date = tomorrow(),
      startTime = LocalTime.of(14, 0),
      endTime = LocalTime.of(15, 0),
      locationId = birminghamLocation.id,
    ).apply {
      amendedTime = amendedAt
      amendedBy = prisonUser().username
    }

    with(ProbationBookingAmendedTelemetryEvent(booking, prisonUser())) {
      eventType isEqualTo "BVLS-probation-booking-amended"
      properties() containsEntriesExactlyInAnyOrder mapOf(
        "video_booking_id" to "0",
        "amended_by" to "prison",
        "team_code" to BLACKPOOL_MC_PPOC,
        "meeting_type" to "PSR",
        "prison_code" to BIRMINGHAM,
        "location_id" to birminghamLocation.id.toString(),
        "start" to tomorrow().atTime(LocalTime.of(14, 0)).toIsoDateTime(),
        "end" to tomorrow().atTime(LocalTime.of(15, 0)).toIsoDateTime(),
      )

      metrics() containsEntriesExactlyInAnyOrder mapOf(
        "hoursBeforeStartTime" to hoursBetween(
          amendedAt,
          tomorrow().atTime(LocalTime.of(14, 0)),
        ).toDouble(),
      )
    }
  }

  private fun hoursBetween(from: LocalDateTime, to: LocalDateTime) = ChronoUnit.HOURS.between(from, to)

  @Test
  fun `should fail to create event if court booking`() {
    val error = assertThrows<IllegalArgumentException> { ProbationBookingCreatedTelemetryEvent(courtBooking()) }

    error.message isEqualTo "Cannot create probation created metric, video booking with ID '0' is not a probation booking."
  }

  @Test
  fun `should fail to create event when booking has not been amended`() {
    val error = assertThrows<IllegalArgumentException> { ProbationBookingAmendedTelemetryEvent(probationBooking(), probationUser()) }

    error.message isEqualTo "Cannot create probation amended metric, video booking with ID '0' has not been amended."
  }
}
