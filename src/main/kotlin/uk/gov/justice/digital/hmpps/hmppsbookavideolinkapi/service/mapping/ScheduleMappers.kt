package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.BookingStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.ScheduleItem
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ScheduleItem as ScheduleItemEntity

fun ScheduleItemEntity.toModel() = ScheduleItem(
  videoBookingId = videoBookingId,
  prisonAppointmentId = prisonAppointmentId,
  bookingType = BookingType.valueOf(bookingType),
  statusCode = BookingStatus.valueOf(statusCode),
  videoUrl = videoUrl,
  bookingComments = bookingComments,
  createdByPrison = createdByPrison,
  courtId = courtId,
  courtCode = courtCode,
  courtDescription = courtDescription,
  hearingType = hearingType?.let { CourtHearingType.valueOf(hearingType) },
  hearingTypeDescription = hearingTypeDescription,
  probationTeamId = probationTeamId,
  probationTeamCode = probationTeamCode,
  probationTeamDescription = probationTeamDescription,
  probationMeetingType = probationMeetingType?.let { ProbationMeetingType.valueOf(probationMeetingType) },
  probationMeetingTypeDescription = probationMeetingTypeDescription,
  prisonCode = prisonCode,
  prisonName = prisonName,
  prisonerNumber = prisonerNumber,
  appointmentType = AppointmentType.valueOf(appointmentType),
  appointmentTypeDescription = appointmentTypeDescription,
  appointmentComments = appointmentComments,
  prisonLocKey = prisonLocationId,
  appointmentDate = appointmentDate,
  startTime = startTime,
  endTime = endTime,
)

fun List<ScheduleItemEntity>.toModel() = map { it.toModel() }
