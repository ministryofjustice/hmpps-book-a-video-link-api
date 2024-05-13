package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.Appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import java.time.LocalDate
import java.time.LocalTime

fun courtBookingRequest(
  courtId: Long = 1,
  prisonCode: String = "MDI",
  prisonerNumber: String = "123456",
): CreateVideoBookingRequest {
  val appointment = Appointment(
    type = AppointmentType.HEARING,
    locationKey = "$prisonCode-A-1-001",
    date = LocalDate.now().plusDays(1),
    startTime = LocalTime.now(),
    endTime = LocalTime.now().plusHours(1),
  )

  val prisoner = PrisonerDetails(
    prisonCode = prisonCode,
    prisonerNumber = prisonerNumber,
    appointments = listOf(appointment),
  )

  return CreateVideoBookingRequest(
    bookingType = BookingType.COURT,
    courtId = courtId,
    courtHearingType = CourtHearingType.TRIBUNAL,
    prisoners = listOf(prisoner),
    comments = "Blah de blah",
    videoLinkUrl = "https://video.link.com",
  )
}

fun probationBookingRequest(
  probationTeamId: Long = 1,
  prisonCode: String = "MDI",
  prisonerNumber: String = "123456",
): CreateVideoBookingRequest {
  val appointment = Appointment(
    type = AppointmentType.PRE_SENTENCE_REPORT,
    locationKey = "$prisonCode-A-1-001",
    date = LocalDate.now().plusDays(1),
    startTime = LocalTime.now(),
    endTime = LocalTime.now().plusHours(1),
  )

  val prisoner = PrisonerDetails(
    prisonCode = prisonCode,
    prisonerNumber = prisonerNumber,
    appointments = listOf(appointment),
  )

  return CreateVideoBookingRequest(
    bookingType = BookingType.PROBATION,
    probationTeamId = probationTeamId,
    probationMeetingType = ProbationMeetingType.PSR,
    prisoners = listOf(prisoner),
    comments = "Blah de blah",
    videoLinkUrl = "https://video.link.com",
  )
}
