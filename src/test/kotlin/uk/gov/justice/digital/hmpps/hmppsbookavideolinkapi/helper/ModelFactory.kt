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
  locationSuffix: String = "A-1-001",
  startTime: LocalTime = LocalTime.now(),
  endTime: LocalTime = LocalTime.now().plusHours(1),
  comments: String = "court booking comments",
  appointments: List<Appointment> = emptyList(),
): CreateVideoBookingRequest {
  val prisoner = PrisonerDetails(
    prisonCode = prisonCode,
    prisonerNumber = prisonerNumber,
    appointments = appointments.ifEmpty {
      listOf(
        Appointment(
          type = AppointmentType.VLB_COURT_MAIN,
          locationKey = "$prisonCode-$locationSuffix",
          date = tomorrow(),
          startTime = startTime,
          endTime = endTime,
        ),
      )
    },
  )

  return CreateVideoBookingRequest(
    bookingType = BookingType.COURT,
    courtId = courtId,
    courtHearingType = CourtHearingType.TRIBUNAL,
    prisoners = listOf(prisoner),
    comments = comments,
    videoLinkUrl = "https://video.link.com",
  )
}

fun probationBookingRequest(
  probationTeamId: Long = 1,
  probationMeetingType: ProbationMeetingType = ProbationMeetingType.PSR,
  videoLinkUrl: String = "https://video.link.com",
  prisonCode: String = "MDI",
  prisonerNumber: String = "123456",
  locationSuffix: String = "A-1-001",
  appointmentType: AppointmentType = AppointmentType.VLB_PROBATION,
  appointmentDate: LocalDate = tomorrow(),
  startTime: LocalTime = LocalTime.now(),
  endTime: LocalTime = LocalTime.now().plusHours(1),
  comments: String = "probation booking comments",
): CreateVideoBookingRequest {
  val appointment = Appointment(
    type = appointmentType,
    locationKey = "$prisonCode-$locationSuffix",
    date = appointmentDate,
    startTime = startTime,
    endTime = endTime,
  )

  val prisoner = PrisonerDetails(
    prisonCode = prisonCode,
    prisonerNumber = prisonerNumber,
    appointments = listOf(appointment),
  )

  return CreateVideoBookingRequest(
    bookingType = BookingType.PROBATION,
    probationTeamId = probationTeamId,
    probationMeetingType = probationMeetingType,
    prisoners = listOf(prisoner),
    comments = comments,
    videoLinkUrl = videoLinkUrl,
  )
}
