package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.CHESTERFIELD_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.locationAttributes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.risleyLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.BookingStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.VideoLinkBooking

class VideoLinkBookingMappersTest {

  @Test
  fun `should map known court booking entity to model without video URL`() {
    val booking = VideoBooking.newCourtBooking(
      court(code = CHESTERFIELD_JUSTICE_CENTRE),
      CourtHearingType.TRIBUNAL.name,
      comments = "some comments for the court booking",
      videoUrl = null,
      createdBy = COURT_USER,
      notesForStaff = "Some private staff notes",
      notesForPrisoners = "Some public prisoners notes",
    )

    booking.toModel(locations = setOf(risleyLocation.toModel(locationAttributes().copy(prisonVideoUrl = "prob-video-url"))), courtHearingTypeDescription = "hearing type description") isEqualTo VideoLinkBooking(
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
      videoLinkUrl = null,
      createdBy = COURT_USER.username,
      createdAt = booking.createdTime,
      comments = "some comments for the court booking",
      amendedAt = null,
      amendedBy = null,
      notesForStaff = "Some private staff notes",
      notesForPrisoners = "Some public prisoners notes",
    )
  }

  @Test
  fun `should map known court booking entity to model with video URL`() {
    val booking = VideoBooking.newCourtBooking(
      court(code = CHESTERFIELD_JUSTICE_CENTRE),
      CourtHearingType.TRIBUNAL.name,
      comments = "some comments for the court booking",
      videoUrl = "court-video-url",
      createdBy = COURT_USER,
      notesForStaff = "Some private staff notes",
      notesForPrisoners = "Some public prisoners notes",
    )

    booking.toModel(locations = setOf(risleyLocation.toModel(locationAttributes().copy(prisonVideoUrl = "prob-video-url"))), courtHearingTypeDescription = "hearing type description") isEqualTo VideoLinkBooking(
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
      videoLinkUrl = "court-video-url",
      createdBy = COURT_USER.username,
      createdAt = booking.createdTime,
      comments = "some comments for the court booking",
      amendedAt = null,
      amendedBy = null,
      notesForStaff = "Some private staff notes",
      notesForPrisoners = "Some public prisoners notes",
    )
  }

  @Test
  fun `should map known probation booking entity to model with prison video URL`() {
    val booking = VideoBooking.newProbationBooking(
      probationTeam(DERBY_JUSTICE_CENTRE),
      probationMeetingType = "PSR",
      comments = "some comments for the probation booking",
      createdBy = PROBATION_USER,
      notesForStaff = "Some private staff notes",
      notesForPrisoners = "Some public prisoners notes",
    )

    booking.toModel(locations = setOf(risleyLocation.toModel(locationAttributes().copy(prisonVideoUrl = "prob-video-url"))), probationMeetingTypeDescription = "meeting type description") isEqualTo VideoLinkBooking(
      videoLinkBookingId = booking.videoBookingId,
      statusCode = BookingStatus.ACTIVE,
      bookingType = BookingType.PROBATION,
      prisonAppointments = emptyList(),
      courtCode = null,
      courtDescription = null,
      courtHearingType = null,
      courtHearingTypeDescription = null,
      probationTeamCode = DERBY_JUSTICE_CENTRE,
      probationMeetingType = ProbationMeetingType.PSR,
      probationTeamDescription = "probation team description",
      probationMeetingTypeDescription = "meeting type description",
      createdByPrison = false,
      videoLinkUrl = "prob-video-url",
      createdBy = PROBATION_USER.username,
      createdAt = booking.createdTime,
      comments = "some comments for the probation booking",
      amendedAt = null,
      amendedBy = null,
      notesForStaff = "Some private staff notes",
      notesForPrisoners = "Some public prisoners notes",
    )
  }
}
