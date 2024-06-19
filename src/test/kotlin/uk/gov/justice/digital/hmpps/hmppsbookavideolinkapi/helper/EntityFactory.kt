package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ProbationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import java.time.LocalDate
import java.time.LocalTime

fun court(code: String = DERBY_JUSTICE_CENTRE, enabled: Boolean = true) = Court(
  courtId = 0,
  code = code,
  description = DERBY_JUSTICE_CENTRE,
  enabled = enabled,
  notes = null,
  createdBy = "Test",
)

fun prison(prisonCode: String, enabled: Boolean = true) = Prison(
  prisonId = 0,
  code = prisonCode,
  name = prisonNames[prisonCode] ?: "Unknown prison",
  enabled = enabled,
  notes = null,
  createdBy = "Test",
)

fun probationTeam(code: String = "BLKPPP", enabled: Boolean = true) = ProbationTeam(
  probationTeamId = 0,
  code = code,
  description = "probation team description",
  enabled = enabled,
  notes = null,
  createdBy = "Test",
)

fun courtBooking(createdBy: String = "court_user") = VideoBooking.newCourtBooking(
  court = court(),
  hearingType = "TRIBUNAL",
  comments = "Court hearing comments",
  videoUrl = "https://court.hearing.link",
  createdBy = createdBy,
  createdByPrison = false,
)

fun probationBooking() = VideoBooking.newProbationBooking(
  probationTeam = probationTeam(),
  probationMeetingType = "PSR",
  comments = "Probation meeting comments",
  videoUrl = "https://probation.meeting.link",
  createdBy = "Probation team user",
  createdByPrison = false,
)

fun appointment(
  booking: VideoBooking,
  prisonCode: String,
  prisonerNumber: String,
  date: LocalDate = tomorrow(),
  startTime: LocalTime = LocalTime.of(9, 0),
  endTime: LocalTime = LocalTime.of(10, 0),
  appointmentType: String,
  locationKey: String,
  createdBy: String = "Test",
) = PrisonAppointment.newAppointment(
  videoBooking = booking,
  prisonCode = prisonCode,
  prisonerNumber = prisonerNumber,
  appointmentType = appointmentType,
  appointmentDate = date,
  startTime = startTime,
  endTime = endTime,
  locationKey = locationKey,
  createdBy = createdBy,
)
