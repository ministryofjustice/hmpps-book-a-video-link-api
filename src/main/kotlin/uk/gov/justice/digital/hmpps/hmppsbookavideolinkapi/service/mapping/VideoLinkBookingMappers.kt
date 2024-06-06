package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.BookingStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.VideoLinkBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment as PrisonAppointmentEntity
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking as VideoBookingEntity

fun VideoBookingEntity.toModel(
  prisonAppointments: List<PrisonAppointmentEntity>,
  courtDescription: String? = null,
  probationTeamDescription: String? = null,
  courtHearingTypeDescription: String? = null,
  probationMeetingTypeDescription: String? = null,
) = VideoLinkBooking(
  videoLinkBookingId = videoBookingId,
  bookingType = bookingType.let { BookingType.valueOf(bookingType) },
  status = BookingStatus.valueOf(statusCode),
  prisonAppointments = prisonAppointments.toModel(),
  courtCode = court?.code,
  courtDescription = court?.code?.let { courtDescription },
  courtHearingType = hearingType?.let { CourtHearingType.valueOf(hearingType) },
  courtHearingTypeDescription = hearingType?.let { courtHearingTypeDescription },
  probationTeamCode = probationTeam?.code.let { probationTeam?.code },
  probationTeamDescription = probationTeam?.code?.let { probationTeamDescription },
  probationMeetingType = probationMeetingType?.let { ProbationMeetingType.valueOf(probationMeetingType) },
  probationMeetingTypeDescription = probationMeetingType?.let { probationMeetingTypeDescription },
  comments = comments,
  videoLinkUrl = videoUrl,
  createdByPrison = createdByPrison,
  createdBy = createdBy,
  createdAt = createdTime,
  amendedBy = amendedBy,
  amendedAt = amendedTime,
)

fun List<VideoBookingEntity>.toModel(prisonAppointments: List<PrisonAppointment>)
  = map { it.toModel(prisonAppointments = prisonAppointments) }
