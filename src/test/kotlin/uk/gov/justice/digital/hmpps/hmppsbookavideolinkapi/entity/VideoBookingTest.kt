package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday
import java.time.LocalDateTime.now
import java.time.LocalTime
import java.util.UUID

class VideoBookingTest {

  private val booking = courtBooking().also {
    it.isBookingType(BookingType.COURT) isBool true
    it.isBookingType(BookingType.PROBATION) isBool false
    it.statusCode isEqualTo StatusCode.ACTIVE
    it.amendedBy isEqualTo null
    it.amendedTime isEqualTo null
  }

  @Test
  fun `should be court booking created by prison user`() {
    val courtBooking = VideoBooking.newCourtBooking(
      court(code = "COURT_CODE"),
      hearingType = "TRIBUNAL",
      comments = "Some prison user comments",
      videoUrl = "prison-user-video-url",
      createdBy = PRISON_USER_BIRMINGHAM,
    )

    with(courtBooking) {
      court isEqualTo court(code = "COURT_CODE")
      isBookingType(BookingType.COURT) isBool true
      hearingType isEqualTo "TRIBUNAL"
      comments isEqualTo "Some prison user comments"
      videoUrl isEqualTo "prison-user-video-url"
      createdBy isEqualTo PRISON_USER_BIRMINGHAM.username
      createdTime isCloseTo now()
      createdByPrison isBool true
      statusCode isEqualTo StatusCode.ACTIVE
    }
  }

  @Test
  fun `should be court booking created by court user`() {
    val courtBooking = VideoBooking.newCourtBooking(
      court(code = "COURT_CODE"),
      hearingType = "APPEAL",
      comments = "Some court user comments",
      videoUrl = "court-user-video-url",
      createdBy = COURT_USER,
    )

    with(courtBooking) {
      court isEqualTo court(code = "COURT_CODE")
      isBookingType(BookingType.COURT) isBool true
      hearingType isEqualTo "APPEAL"
      comments isEqualTo "Some court user comments"
      videoUrl isEqualTo "court-user-video-url"
      createdBy isEqualTo COURT_USER.username
      createdByPrison isBool false
      statusCode isEqualTo StatusCode.ACTIVE
    }
  }

  @Test
  fun `should be probation booking created by prison user`() {
    val probationBooking = VideoBooking.newProbationBooking(
      probationTeam(code = "TEAM_CODE"),
      probationMeetingType = "OTHER",
      comments = "Some prison user comments",
      createdBy = PRISON_USER_BIRMINGHAM,
    )

    with(probationBooking) {
      probationTeam isEqualTo probationTeam(code = "TEAM_CODE")
      isBookingType(BookingType.PROBATION) isBool true
      probationMeetingType isEqualTo "OTHER"
      comments isEqualTo "Some prison user comments"
      createdBy isEqualTo PRISON_USER_BIRMINGHAM.username
      createdTime isCloseTo now()
      createdByPrison isBool true
      statusCode isEqualTo StatusCode.ACTIVE
    }
  }

  @Test
  fun `should be probation booking created by probation user`() {
    val probationBooking = VideoBooking.newProbationBooking(
      probationTeam(code = "TEAM_CODE"),
      probationMeetingType = "PSR",
      comments = "Some probation user comments",
      createdBy = PROBATION_USER,
    )

    with(probationBooking) {
      probationTeam isEqualTo probationTeam(code = "TEAM_CODE")
      isBookingType(BookingType.PROBATION) isBool true
      probationMeetingType isEqualTo "PSR"
      comments isEqualTo "Some probation user comments"
      createdBy isEqualTo PROBATION_USER.username
      createdByPrison isBool false
      statusCode isEqualTo StatusCode.ACTIVE
    }
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
      locationId = UUID.randomUUID(),
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
      locationId = UUID.randomUUID(),
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
      locationId = UUID.randomUUID(),
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
  fun `should reject new booking if probation team is read only`() {
    assertThrows<IllegalArgumentException> {
      VideoBooking.newProbationBooking(
        probationTeam = probationTeam("READ_ONLY", readOnly = true),
        probationMeetingType = "PSR",
        comments = null,
        createdBy = PROBATION_USER,
      )
    }.message isEqualTo "Probation team with code READ_ONLY is read only"
  }

  @Test
  fun `should accept new booking if probation team is not read only`() {
    assertDoesNotThrow {
      VideoBooking.newProbationBooking(
        probationTeam = probationTeam("NOT_READ_ONLY", readOnly = false),
        probationMeetingType = "PSR",
        comments = null,
        createdBy = PROBATION_USER,
      )
    }
  }
}
