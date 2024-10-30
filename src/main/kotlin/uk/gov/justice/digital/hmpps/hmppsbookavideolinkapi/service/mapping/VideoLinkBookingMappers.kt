package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.BookingStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.VideoLinkBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking as VideoBookingEntity

fun VideoBookingEntity.toModel(
  locations: Set<Location>,
  courtHearingTypeDescription: String? = null,
  probationMeetingTypeDescription: String? = null,
) = VideoLinkBooking(
  videoLinkBookingId = videoBookingId,
  bookingType = BookingType.valueOf(bookingType),
  statusCode = BookingStatus.valueOf(statusCode.name),
  prisonAppointments = appointments().toModel(locations),
  courtCode = court?.code,
  courtDescription = court?.let { migratedDescription ?: it.description },
  courtHearingType = hearingType?.let { CourtHearingType.valueOf(hearingType!!) },
  courtHearingTypeDescription = hearingType?.let { courtHearingTypeDescription },
  probationTeamCode = probationTeam?.code,
  probationTeamDescription = probationTeam?.let { migratedDescription ?: it.description },
  probationMeetingType = probationMeetingType?.let { ProbationMeetingType.valueOf(probationMeetingType!!) },
  probationMeetingTypeDescription = probationMeetingType?.let { probationMeetingTypeDescription },
  comments = comments,
  videoLinkUrl = videoUrl,
  createdByPrison = createdByPrison,
  createdBy = createdBy,
  createdAt = createdTime,
  amendedBy = amendedBy,
  amendedAt = amendedTime,
)
