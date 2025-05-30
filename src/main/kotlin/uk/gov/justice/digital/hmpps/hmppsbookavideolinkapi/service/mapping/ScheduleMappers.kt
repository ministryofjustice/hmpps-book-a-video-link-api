package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.BookingStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.ScheduleItem
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ScheduleItem as ScheduleItemEntity

fun ScheduleItemEntity.toModel(locations: List<Location>) = ScheduleItem(
  videoBookingId = videoBookingId,
  prisonAppointmentId = prisonAppointmentId,
  bookingType = BookingType.valueOf(bookingType),
  statusCode = BookingStatus.valueOf(statusCode),
  videoUrl = if (courtCode != null && appointmentType == "VLB_COURT_MAIN") videoUrl else locations.find { it.dpsLocationId == prisonLocationId }?.extraAttributes?.prisonVideoUrl,
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
  prisonLocKey = locations.find { it.dpsLocationId == prisonLocationId }?.key ?: throw IllegalArgumentException("Prison location with id $prisonLocationId not found in supplied set of locations"),
  appointmentDate = appointmentDate,
  startTime = startTime,
  endTime = endTime,
  prisonLocDesc = locations.find { it.dpsLocationId == prisonLocationId }?.description ?: throw IllegalArgumentException("Prison location with id $prisonLocationId not found in supplied set of locations"),
  dpsLocationId = prisonLocationId,
  createdTime = createdTime,
  createdBy = createdBy,
  updatedTime = updatedTime,
  updatedBy = updatedBy,
  probationOfficerName = probationOfficerName,
  probationOfficerEmailAddress = probationOfficerEmailAddress,
  notesForStaff = notesForStaff,
  notesForPrisoners = notesForPrisoners,
)

fun List<ScheduleItemEntity>.toModel(locations: List<Location>) = map { it.toModel(locations) }
