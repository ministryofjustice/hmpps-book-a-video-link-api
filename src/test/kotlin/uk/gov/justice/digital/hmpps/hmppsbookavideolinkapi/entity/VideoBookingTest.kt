package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.daysAgo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday
import java.time.LocalDateTime.now
import java.time.LocalTime

class VideoBookingTest {

  private val booking = courtBooking().also {
    it.isCourtBooking() isBool true
    it.isProbationBooking() isBool false
    it.statusCode isEqualTo StatusCode.ACTIVE
    it.amendedBy isEqualTo null
    it.amendedTime isEqualTo null
  }

  @Test
  fun `should cancel booking when active`() {
    booking.addAppointment(
      prison = prison(prisonCode = BIRMINGHAM),
      prisonerNumber = "ABC123",
      appointmentType = "VLB_COURT_MAIN",
      date = tomorrow(),
      startTime = LocalTime.now(),
      endTime = LocalTime.now().plusHours(1),
      locationId = "loc-key",
    )

    booking.cancel(courtUser())

    booking.statusCode isEqualTo StatusCode.CANCELLED
    booking.amendedBy isEqualTo "user"
    booking.amendedTime isCloseTo now()
  }

  @Test
  fun `should reject booking cancellation if already cancelled`() {
    booking.addAppointment(
      prison = prison(prisonCode = BIRMINGHAM),
      prisonerNumber = "ABC123",
      appointmentType = "VLB_COURT_MAIN",
      date = tomorrow(),
      startTime = LocalTime.now(),
      endTime = LocalTime.now().plusHours(1),
      locationId = "loc-key",
    )

    booking.cancel(courtUser())

    val exception = assertThrows<IllegalArgumentException> {
      booking.cancel(courtUser())
    }

    exception.message isEqualTo "Video booking ${booking.videoBookingId} is already cancelled"
  }

  @Test
  fun `should reject booking cancellation if appointments in past`() {
    booking.addAppointment(
      prison = prison(prisonCode = BIRMINGHAM),
      prisonerNumber = "ABC123",
      appointmentType = "VLB_COURT_MAIN",
      date = yesterday(),
      startTime = LocalTime.now(),
      endTime = LocalTime.now().plusHours(1),
      locationId = "loc-key",
    )

    val exception = assertThrows<IllegalArgumentException> {
      booking.cancel(courtUser())
    }

    exception.message isEqualTo "Video booking ${booking.videoBookingId} cannot be cancelled"
  }

  @Test
  fun `should not be a migrated booking`() {
    booking.isMigrated() isBool false
  }

  @Test
  fun `should create a migrated court booking`() {
    val migratedBooking = VideoBooking.migratedCourtBooking(
      court = court(code = "migrated_court_code"),
      createdBy = "migrated court user",
      createdTime = yesterday().atStartOfDay(),
      createdByPrison = false,
      comments = "migrated court comments",
      migratedVideoBookingId = 100,
      cancelledBy = null,
      cancelledAt = null,
      updatedAt = null,
      updatedBy = null,
      migratedDescription = "Free text court description",
    )

    with(migratedBooking) {
      isMigrated() isBool true
      isCourtBooking() isBool true
      isProbationBooking() isBool false
      court isEqualTo court(code = "migrated_court_code")
      createdBy isEqualTo "migrated court user"
      createdTime isEqualTo yesterday().atStartOfDay()
      createdByPrison isBool false
      comments isEqualTo "migrated court comments"
      hearingType isEqualTo "UNKNOWN"
      migratedVideoBookingId isEqualTo 100
      migratedDescription isEqualTo null
    }
  }

  @Test
  fun `should create a migrated (unknown) court booking`() {
    val migratedBooking = VideoBooking.migratedCourtBooking(
      court = court(code = UNKNOWN_COURT_CODE),
      createdBy = "migrated court user",
      createdTime = yesterday().atStartOfDay(),
      createdByPrison = false,
      comments = "migrated court comments",
      migratedVideoBookingId = 100,
      cancelledBy = null,
      cancelledAt = null,
      updatedAt = null,
      updatedBy = null,
      migratedDescription = "Free text court description",
    )

    with(migratedBooking) {
      isMigrated() isBool true
      isCourtBooking() isBool true
      isProbationBooking() isBool false
      court isEqualTo court(code = UNKNOWN_COURT_CODE)
      createdBy isEqualTo "migrated court user"
      createdTime isEqualTo yesterday().atStartOfDay()
      createdByPrison isBool false
      comments isEqualTo "migrated court comments"
      hearingType isEqualTo "UNKNOWN"
      migratedVideoBookingId isEqualTo 100
      migratedDescription isEqualTo "Free text court description"
    }
  }

  @Test
  fun `should create a cancelled migrated court booking`() {
    val migratedBooking = VideoBooking.migratedCourtBooking(
      court = court(code = "migrated_court_code"),
      createdBy = "migrated court user",
      createdTime = 2.daysAgo().atStartOfDay(),
      createdByPrison = false,
      comments = "migrated court comments",
      migratedVideoBookingId = 100,
      cancelledBy = "COURT CANCELLATION USER",
      cancelledAt = 1.daysAgo().atStartOfDay(),
      updatedAt = 2.daysAgo().atStartOfDay().plusHours(1),
      updatedBy = "COURT UPDATE USER",
      migratedDescription = null,
    )

    with(migratedBooking) {
      isMigrated() isBool true
      isCourtBooking() isBool true
      isProbationBooking() isBool false
      court isEqualTo court(code = "migrated_court_code")
      createdBy isEqualTo "migrated court user"
      createdTime isEqualTo 2.daysAgo().atStartOfDay()
      createdByPrison isBool false
      comments isEqualTo "migrated court comments"
      hearingType isEqualTo "UNKNOWN"
      migratedVideoBookingId isEqualTo 100
      statusCode isEqualTo StatusCode.CANCELLED
      amendedBy isEqualTo "COURT CANCELLATION USER"
      amendedTime isEqualTo 1.daysAgo().atStartOfDay()
    }
  }

  @Test
  fun `should create an updated migrated court booking`() {
    val migratedBooking = VideoBooking.migratedCourtBooking(
      court = court(code = "migrated_court_code"),
      createdBy = "migrated court user",
      createdTime = 2.daysAgo().atStartOfDay(),
      createdByPrison = false,
      comments = "migrated court comments",
      migratedVideoBookingId = 100,
      cancelledBy = null,
      cancelledAt = null,
      updatedAt = 2.daysAgo().atStartOfDay().plusHours(1),
      updatedBy = "COURT UPDATE USER",
      migratedDescription = null,
    )

    with(migratedBooking) {
      isMigrated() isBool true
      isCourtBooking() isBool true
      isProbationBooking() isBool false
      court isEqualTo court(code = "migrated_court_code")
      createdBy isEqualTo "migrated court user"
      createdTime isEqualTo 2.daysAgo().atStartOfDay()
      createdByPrison isBool false
      comments isEqualTo "migrated court comments"
      hearingType isEqualTo "UNKNOWN"
      migratedVideoBookingId isEqualTo 100
      statusCode isEqualTo StatusCode.ACTIVE
      amendedBy isEqualTo "COURT UPDATE USER"
      amendedTime isEqualTo 2.daysAgo().atStartOfDay().plusHours(1)
    }
  }

  @Test
  fun `should truncate comments for migrated court booking to 1000 character`() {
    val migratedBooking = VideoBooking.migratedCourtBooking(
      court = court(code = "migrated_court_code"),
      createdBy = "migrated court user",
      createdTime = yesterday().atStartOfDay(),
      createdByPrison = false,
      comments = "x".repeat(2000),
      migratedVideoBookingId = 100,
      cancelledBy = null,
      cancelledAt = null,
      updatedAt = null,
      updatedBy = null,
      migratedDescription = null,
    )

    migratedBooking.comments?.length isEqualTo 1000
  }

  @Test
  fun `should create a migrated probation booking`() {
    val migratedBooking = VideoBooking.migratedProbationBooking(
      probationTeam = probationTeam(code = "migrated_team_code"),
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

    with(migratedBooking) {
      isMigrated() isBool true
      isProbationBooking() isBool true
      isCourtBooking() isBool false
      probationTeam isEqualTo probationTeam(code = "migrated_team_code")
      createdBy isEqualTo "migrated probation user"
      createdTime isEqualTo 3.daysAgo().atStartOfDay()
      createdByPrison isBool false
      comments isEqualTo "migrated probation comments"
      probationMeetingType isEqualTo "UNKNOWN"
      migratedVideoBookingId isEqualTo 100
      migratedDescription isEqualTo null
    }
  }

  @Test
  fun `should create a migrated (unknown) probation booking`() {
    val migratedBooking = VideoBooking.migratedProbationBooking(
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

    with(migratedBooking) {
      isMigrated() isBool true
      isProbationBooking() isBool true
      isCourtBooking() isBool false
      probationTeam isEqualTo probationTeam(code = UNKNOWN_PROBATION_TEAM_CODE)
      createdBy isEqualTo "migrated probation user"
      createdTime isEqualTo 3.daysAgo().atStartOfDay()
      createdByPrison isBool false
      comments isEqualTo "migrated probation comments"
      probationMeetingType isEqualTo "UNKNOWN"
      migratedVideoBookingId isEqualTo 100
      migratedDescription isEqualTo "Free text probation team description"
    }
  }

  @Test
  fun `should create a cancelled migrated probation booking`() {
    val migratedBooking = VideoBooking.migratedProbationBooking(
      probationTeam = probationTeam(code = "migrated_team_code"),
      createdBy = "migrated probation user",
      createdTime = 4.daysAgo().atStartOfDay(),
      createdByPrison = false,
      comments = "migrated probation comments",
      migratedVideoBookingId = 100,
      cancelledBy = "PROBATION CANCELLATION USER",
      cancelledAt = 2.daysAgo().atStartOfDay(),
      updatedAt = 3.daysAgo().atStartOfDay(),
      updatedBy = "PROBATION UPDATE USER",
      migratedDescription = null,
    )

    with(migratedBooking) {
      isMigrated() isBool true
      isProbationBooking() isBool true
      isCourtBooking() isBool false
      probationTeam isEqualTo probationTeam(code = "migrated_team_code")
      createdBy isEqualTo "migrated probation user"
      createdTime isEqualTo 4.daysAgo().atStartOfDay()
      createdByPrison isBool false
      comments isEqualTo "migrated probation comments"
      probationMeetingType isEqualTo "UNKNOWN"
      migratedVideoBookingId isEqualTo 100
      statusCode isEqualTo StatusCode.CANCELLED
      amendedBy isEqualTo "PROBATION CANCELLATION USER"
      amendedTime isEqualTo 2.daysAgo().atStartOfDay()
    }
  }

  @Test
  fun `should create an updated migrated probation booking`() {
    val migratedBooking = VideoBooking.migratedProbationBooking(
      probationTeam = probationTeam(code = "migrated_team_code"),
      createdBy = "migrated probation user",
      createdTime = 4.daysAgo().atStartOfDay(),
      createdByPrison = false,
      comments = "migrated probation comments",
      migratedVideoBookingId = 100,
      cancelledBy = null,
      cancelledAt = null,
      updatedAt = 3.daysAgo().atStartOfDay(),
      updatedBy = "PROBATION UPDATE USER",
      migratedDescription = null,
    )

    with(migratedBooking) {
      isMigrated() isBool true
      isProbationBooking() isBool true
      isCourtBooking() isBool false
      probationTeam isEqualTo probationTeam(code = "migrated_team_code")
      createdBy isEqualTo "migrated probation user"
      createdTime isEqualTo 4.daysAgo().atStartOfDay()
      createdByPrison isBool false
      comments isEqualTo "migrated probation comments"
      probationMeetingType isEqualTo "UNKNOWN"
      migratedVideoBookingId isEqualTo 100
      statusCode isEqualTo StatusCode.ACTIVE
      amendedBy isEqualTo "PROBATION UPDATE USER"
      amendedTime isEqualTo 3.daysAgo().atStartOfDay()
    }
  }

  @Test
  fun `should truncate comments for migrated probation booking to 1000 characters`() {
    val migratedBooking = VideoBooking.migratedProbationBooking(
      probationTeam = probationTeam(code = "migrated_team_code"),
      createdBy = "migrated probation user",
      createdTime = yesterday().atStartOfDay(),
      createdByPrison = false,
      comments = "z".repeat(2000),
      migratedVideoBookingId = 100,
      cancelledBy = null,
      cancelledAt = null,
      updatedAt = null,
      updatedBy = null,
      migratedDescription = null,
    )

    migratedBooking.comments?.length isEqualTo 1000
  }
}
