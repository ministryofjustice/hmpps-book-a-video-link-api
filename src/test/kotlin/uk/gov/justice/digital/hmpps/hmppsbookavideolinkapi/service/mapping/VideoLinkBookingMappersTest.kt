package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.CHESTERFIELD_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.BookingStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.VideoLinkBooking

class VideoLinkBookingMappersTest {

  @Test
  fun `should map known court booking entity to model`() {
    val booking = VideoBooking.newCourtBooking(
      court(code = CHESTERFIELD_JUSTICE_CENTRE),
      CourtHearingType.TRIBUNAL.name,
      comments = "some comments for the court booking",
      videoUrl = "court video url",
      createdBy = "test",
      createdByPrison = false,
    )

    booking.toModel(locations = emptySet(), courtHearingTypeDescription = "hearing type description") isEqualTo VideoLinkBooking(
      videoLinkBookingId = booking.videoBookingId,
      statusCode = BookingStatus.ACTIVE,
      bookingType = BookingType.COURT,
      prisonAppointments = emptyList(),
      courtCode = CHESTERFIELD_JUSTICE_CENTRE,
      courtDescription = CHESTERFIELD_JUSTICE_CENTRE,
      courtHearingType = CourtHearingType.TRIBUNAL,
      courtHearingTypeDescription = "hearing type description",
      probationTeamCode = null,
      probationMeetingType = null,
      probationTeamDescription = null,
      probationMeetingTypeDescription = null,
      createdByPrison = false,
      videoLinkUrl = "court video url",
      createdBy = "test",
      createdAt = booking.createdTime,
      comments = "some comments for the court booking",
      amendedAt = null,
      amendedBy = null,
    )
  }
}
