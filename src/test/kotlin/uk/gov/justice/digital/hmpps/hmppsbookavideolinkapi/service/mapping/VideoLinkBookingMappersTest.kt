package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.UNKNOWN_PROBATION_TEAM_CODE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.CHESTERFIELD_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.daysAgo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
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

    booking.toModel(courtHearingTypeDescription = "hearing type description") isEqualTo VideoLinkBooking(
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

  @Test
  fun `should map migrated unknown probation booking entity to model`() {
    val booking = VideoBooking.migratedProbationBooking(
      probationTeam = probationTeam(code = UNKNOWN_PROBATION_TEAM_CODE),
      createdBy = "migrated probation user",
      createdTime = 3.daysAgo().atStartOfDay(),
      createdByPrison = false,
      comments = "migrated probation comments",
      migratedVideoBookingId = 100,
      cancelledBy = null,
      cancelledAt = null,
      updatedAt = null,
      updatedBy = null,
      migratedDescription = "Free text probation team description",
    )

    booking.toModel(courtHearingTypeDescription = "hearing type description") isEqualTo VideoLinkBooking(
      videoLinkBookingId = booking.videoBookingId,
      statusCode = BookingStatus.ACTIVE,
      bookingType = BookingType.PROBATION,
      prisonAppointments = emptyList(),
      courtCode = null,
      courtDescription = null,
      courtHearingType = null,
      courtHearingTypeDescription = null,
      probationTeamCode = UNKNOWN_PROBATION_TEAM_CODE,
      probationMeetingType = ProbationMeetingType.UNKNOWN,
      probationTeamDescription = "Free text probation team description",
      probationMeetingTypeDescription = null,
      createdByPrison = false,
      videoLinkUrl = null,
      createdBy = "migrated probation user",
      createdAt = 3.daysAgo().atStartOfDay(),
      comments = "migrated probation comments",
      amendedAt = null,
      amendedBy = null,
    )
  }
}
