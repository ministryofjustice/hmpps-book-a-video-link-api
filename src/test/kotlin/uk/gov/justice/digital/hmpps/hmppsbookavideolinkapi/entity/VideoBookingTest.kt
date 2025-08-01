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
  fun `should be court booking with CVP video URL created by prison user`() {
    val courtBooking = VideoBooking.newCourtBooking(
      court(code = "COURT_CODE"),
      hearingType = "TRIBUNAL",
      cvpLinkDetails = CvpLinkDetails.url("prison-user-video-url"),
      createdBy = PRISON_USER_BIRMINGHAM,
      guestPin = "123456",
      notesForStaff = "Some private staff notes",
      notesForPrisoners = "Some public prisoners notes",
    )

    with(courtBooking) {
      court isEqualTo court(code = "COURT_CODE")
      isBookingType(BookingType.COURT) isBool true
      hearingType isEqualTo "TRIBUNAL"
      videoUrl isEqualTo "prison-user-video-url"
      hmctsNumber isEqualTo null
      guestPin isEqualTo "123456"
      createdBy isEqualTo PRISON_USER_BIRMINGHAM.username
      createdTime isCloseTo now()
      createdByPrison isBool true
      statusCode isEqualTo StatusCode.ACTIVE
      notesForStaff isEqualTo "Some private staff notes"
      notesForPrisoners isEqualTo "Some public prisoners notes"
    }
  }

  @Test
  fun `should be court booking with CVP HMCTS number created by prison user`() {
    val courtBooking = VideoBooking.newCourtBooking(
      court(code = "COURT_CODE"),
      hearingType = "TRIBUNAL",
      cvpLinkDetails = CvpLinkDetails.hmctsNumber("HMCTS123"),
      createdBy = PRISON_USER_BIRMINGHAM,
      guestPin = "123456",
      notesForStaff = "Some private staff notes",
      notesForPrisoners = "Some public prisoners notes",
    )

    with(courtBooking) {
      court isEqualTo court(code = "COURT_CODE")
      isBookingType(BookingType.COURT) isBool true
      hearingType isEqualTo "TRIBUNAL"
      videoUrl isEqualTo null
      hmctsNumber isEqualTo "HMCTS123"
      guestPin isEqualTo "123456"
      createdBy isEqualTo PRISON_USER_BIRMINGHAM.username
      createdTime isCloseTo now()
      createdByPrison isBool true
      statusCode isEqualTo StatusCode.ACTIVE
      notesForStaff isEqualTo "Some private staff notes"
      notesForPrisoners isEqualTo "Some public prisoners notes"
    }
  }

  @Test
  fun `should amend prison created court booking with CVP video URL created by court user`() {
    val courtBooking = VideoBooking.newCourtBooking(
      court(code = "COURT_CODE"),
      hearingType = "TRIBUNAL",
      cvpLinkDetails = CvpLinkDetails.url("prison-user-video-url"),
      guestPin = "123456",
      createdBy = PRISON_USER_BIRMINGHAM,
      notesForStaff = "Some private staff notes",
      notesForPrisoners = "Some public prisoners notes",
    )

    with(courtBooking) {
      court isEqualTo court(code = "COURT_CODE")
      isBookingType(BookingType.COURT) isBool true
      hearingType isEqualTo "TRIBUNAL"
      videoUrl isEqualTo "prison-user-video-url"
      hmctsNumber isEqualTo null
      guestPin isEqualTo "123456"
      createdBy isEqualTo PRISON_USER_BIRMINGHAM.username
      createdTime isCloseTo now()
      createdByPrison isBool true
      statusCode isEqualTo StatusCode.ACTIVE
      notesForStaff isEqualTo "Some private staff notes"
      notesForPrisoners isEqualTo "Some public prisoners notes"
    }

    courtBooking.amendCourtBooking(
      hearingType = "APPEAL",
      notesForStaff = "Amended staff notes",
      notesForPrisoners = "Amended prisoners notes",
      cvpLinkDetails = CvpLinkDetails.url("amended-prison-user-video-url"),
      guestPin = "654321",
      COURT_USER,
    )

    with(courtBooking) {
      hearingType isEqualTo "APPEAL"
      notesForStaff isEqualTo "Amended staff notes"
      notesForPrisoners isEqualTo "Some public prisoners notes"
      videoUrl isEqualTo "amended-prison-user-video-url"
      hmctsNumber isEqualTo null
      guestPin isEqualTo "654321"
      amendedBy isEqualTo COURT_USER.username
      amendedTime isCloseTo now()
    }
  }

  @Test
  fun `should amend prison created court booking with CVP HMCTS number created by court user`() {
    val courtBooking = VideoBooking.newCourtBooking(
      court(code = "COURT_CODE"),
      hearingType = "TRIBUNAL",
      cvpLinkDetails = CvpLinkDetails.hmctsNumber("HMCTS123"),
      guestPin = "123456",
      createdBy = PRISON_USER_BIRMINGHAM,
      notesForStaff = "Some private staff notes",
      notesForPrisoners = "Some public prisoners notes",
    )

    with(courtBooking) {
      court isEqualTo court(code = "COURT_CODE")
      isBookingType(BookingType.COURT) isBool true
      hearingType isEqualTo "TRIBUNAL"
      videoUrl isEqualTo null
      hmctsNumber isEqualTo "HMCTS123"
      guestPin isEqualTo "123456"
      createdBy isEqualTo PRISON_USER_BIRMINGHAM.username
      createdTime isCloseTo now()
      createdByPrison isBool true
      statusCode isEqualTo StatusCode.ACTIVE
      notesForStaff isEqualTo "Some private staff notes"
      notesForPrisoners isEqualTo "Some public prisoners notes"
    }

    courtBooking.amendCourtBooking(
      hearingType = "APPEAL",
      notesForStaff = "Amended staff notes",
      notesForPrisoners = "Amended prisoners notes",
      cvpLinkDetails = CvpLinkDetails.hmctsNumber("HMCTS321"),
      guestPin = "654321",
      COURT_USER,
    )

    with(courtBooking) {
      hearingType isEqualTo "APPEAL"
      notesForStaff isEqualTo "Amended staff notes"
      notesForPrisoners isEqualTo "Some public prisoners notes"
      videoUrl isEqualTo null
      hmctsNumber isEqualTo "HMCTS321"
      guestPin isEqualTo "654321"
      amendedBy isEqualTo COURT_USER.username
      amendedTime isCloseTo now()
    }
  }

  @Test
  fun `should amend prison created court booking created by prison user`() {
    val courtBooking = VideoBooking.newCourtBooking(
      court(code = "COURT_CODE"),
      hearingType = "TRIBUNAL",
      cvpLinkDetails = CvpLinkDetails.url("prison-user-video-url"),
      guestPin = "123456",
      createdBy = PRISON_USER_BIRMINGHAM,
      notesForStaff = "Some private staff notes",
      notesForPrisoners = "Some public prisoners notes",
    )

    with(courtBooking) {
      court isEqualTo court(code = "COURT_CODE")
      isBookingType(BookingType.COURT) isBool true
      hearingType isEqualTo "TRIBUNAL"
      videoUrl isEqualTo "prison-user-video-url"
      hmctsNumber isEqualTo null
      guestPin isEqualTo "123456"
      createdBy isEqualTo PRISON_USER_BIRMINGHAM.username
      createdTime isCloseTo now()
      createdByPrison isBool true
      statusCode isEqualTo StatusCode.ACTIVE
      notesForStaff isEqualTo "Some private staff notes"
      notesForPrisoners isEqualTo "Some public prisoners notes"
    }

    courtBooking.amendCourtBooking(
      hearingType = "APPEAL",
      notesForStaff = "Amended staff notes",
      notesForPrisoners = "Amended prisoners notes",
      cvpLinkDetails = CvpLinkDetails.url("amended-prison-user-video-url"),
      guestPin = "654321",
      PRISON_USER_BIRMINGHAM,
    )

    with(courtBooking) {
      hearingType isEqualTo "APPEAL"
      notesForStaff isEqualTo "Amended staff notes"
      notesForPrisoners isEqualTo "Amended prisoners notes"
      videoUrl isEqualTo "amended-prison-user-video-url"
      guestPin isEqualTo "654321"
      amendedBy isEqualTo PRISON_USER_BIRMINGHAM.username
      amendedTime isCloseTo now()
    }
  }

  @Test
  fun `should be court booking created by court user`() {
    val courtBooking = VideoBooking.newCourtBooking(
      court(code = "COURT_CODE"),
      hearingType = "APPEAL",
      cvpLinkDetails = CvpLinkDetails.url("court-user-video-url"),
      guestPin = "123456",
      createdBy = COURT_USER,
      notesForStaff = "Some private staff notes",
      notesForPrisoners = "Should be ignored for external user",
    )

    with(courtBooking) {
      court isEqualTo court(code = "COURT_CODE")
      isBookingType(BookingType.COURT) isBool true
      hearingType isEqualTo "APPEAL"
      videoUrl isEqualTo "court-user-video-url"
      guestPin isEqualTo "123456"
      createdBy isEqualTo COURT_USER.username
      createdByPrison isBool false
      statusCode isEqualTo StatusCode.ACTIVE
      notesForStaff isEqualTo "Some private staff notes"
      notesForPrisoners isEqualTo null
    }
  }

  @Test
  fun `should amend court created court booking by court user`() {
    val courtBooking = VideoBooking.newCourtBooking(
      court(code = "COURT_CODE"),
      hearingType = "APPEAL",
      cvpLinkDetails = CvpLinkDetails.url("court-user-video-url"),
      guestPin = "123456",
      createdBy = COURT_USER,
      notesForStaff = "Some private staff notes",
      notesForPrisoners = "Should be ignored for external user",
    )

    with(courtBooking) {
      court isEqualTo court(code = "COURT_CODE")
      isBookingType(BookingType.COURT) isBool true
      hearingType isEqualTo "APPEAL"
      videoUrl isEqualTo "court-user-video-url"
      guestPin isEqualTo "123456"
      createdBy isEqualTo COURT_USER.username
      createdByPrison isBool false
      statusCode isEqualTo StatusCode.ACTIVE
      notesForStaff isEqualTo "Some private staff notes"
      notesForPrisoners isEqualTo null
    }

    courtBooking.amendCourtBooking(
      hearingType = "APPEAL",
      notesForStaff = "Amended staff notes",
      notesForPrisoners = "Amended prisoners notes",
      cvpLinkDetails = CvpLinkDetails.url("amended-court-user-video-url"),
      guestPin = "654321",
      COURT_USER,
    )

    with(courtBooking) {
      hearingType isEqualTo "APPEAL"
      notesForStaff isEqualTo "Amended staff notes"
      notesForPrisoners isEqualTo null
      videoUrl isEqualTo "amended-court-user-video-url"
      guestPin isEqualTo "654321"
      amendedBy isEqualTo COURT_USER.username
      amendedTime isCloseTo now()
    }
  }

  @Test
  fun `should amend court created court booking by prison user`() {
    val courtBooking = VideoBooking.newCourtBooking(
      court(code = "COURT_CODE"),
      hearingType = "APPEAL",
      cvpLinkDetails = CvpLinkDetails.hmctsNumber("HMCTS123"),
      guestPin = "123456",
      createdBy = COURT_USER,
      notesForStaff = "Some private staff notes",
      notesForPrisoners = "Should be ignored for external user",
    )

    with(courtBooking) {
      court isEqualTo court(code = "COURT_CODE")
      isBookingType(BookingType.COURT) isBool true
      hearingType isEqualTo "APPEAL"
      videoUrl isEqualTo null
      hmctsNumber isEqualTo "HMCTS123"
      guestPin isEqualTo "123456"
      createdBy isEqualTo COURT_USER.username
      createdByPrison isBool false
      statusCode isEqualTo StatusCode.ACTIVE
      notesForStaff isEqualTo "Some private staff notes"
      notesForPrisoners isEqualTo null
    }

    courtBooking.amendCourtBooking(
      hearingType = "APPEAL",
      notesForStaff = "Amended staff notes",
      notesForPrisoners = "Amended prisoners notes",
      cvpLinkDetails = CvpLinkDetails.hmctsNumber("HMCTS321"),
      guestPin = "654321",
      PRISON_USER_BIRMINGHAM,
    )

    with(courtBooking) {
      hearingType isEqualTo "APPEAL"
      notesForStaff isEqualTo "Amended staff notes"
      notesForPrisoners isEqualTo "Amended prisoners notes"
      hmctsNumber isEqualTo "HMCTS321"
      guestPin isEqualTo "654321"
      videoUrl isEqualTo null
      amendedBy isEqualTo PRISON_USER_BIRMINGHAM.username
      amendedTime isCloseTo now()
    }
  }

  @Test
  fun `should be probation booking created by prison user`() {
    val probationBooking = VideoBooking.newProbationBooking(
      probationTeam(code = "TEAM_CODE"),
      probationMeetingType = "OTHER",
      createdBy = PRISON_USER_BIRMINGHAM,
      notesForStaff = "Some private staff notes",
      notesForPrisoners = "Some public prisoners notes",
    )

    with(probationBooking) {
      probationTeam isEqualTo probationTeam(code = "TEAM_CODE")
      isBookingType(BookingType.PROBATION) isBool true
      probationMeetingType isEqualTo "OTHER"
      createdBy isEqualTo PRISON_USER_BIRMINGHAM.username
      createdTime isCloseTo now()
      createdByPrison isBool true
      statusCode isEqualTo StatusCode.ACTIVE
      notesForStaff isEqualTo "Some private staff notes"
      notesForPrisoners isEqualTo "Some public prisoners notes"
    }
  }

  @Test
  fun `should amend prison created probation booking created by prison user`() {
    val probationBooking = VideoBooking.newProbationBooking(
      probationTeam(code = "TEAM_CODE"),
      probationMeetingType = "OTHER",
      createdBy = PRISON_USER_BIRMINGHAM,
      notesForStaff = "Some private staff notes",
      notesForPrisoners = "Some public prisoners notes",
    )

    with(probationBooking) {
      probationTeam isEqualTo probationTeam(code = "TEAM_CODE")
      isBookingType(BookingType.PROBATION) isBool true
      probationMeetingType isEqualTo "OTHER"
      createdBy isEqualTo PRISON_USER_BIRMINGHAM.username
      createdTime isCloseTo now()
      createdByPrison isBool true
      statusCode isEqualTo StatusCode.ACTIVE
      notesForStaff isEqualTo "Some private staff notes"
      notesForPrisoners isEqualTo "Some public prisoners notes"
    }

    probationBooking.amendProbationBooking(
      probationMeetingType = "RR",
      notesForStaff = "Amended staff notes",
      notesForPrisoners = "Amended prisoners notes",
      amendedBy = PRISON_USER_BIRMINGHAM,
    )

    with(probationBooking) {
      probationMeetingType isEqualTo "RR"
      notesForStaff isEqualTo "Amended staff notes"
      notesForPrisoners isEqualTo "Amended prisoners notes"
      amendedBy isEqualTo PRISON_USER_BIRMINGHAM.username
      amendedTime isCloseTo now()
    }
  }

  @Test
  fun `should amend prison created probation booking created by probation user`() {
    val probationBooking = VideoBooking.newProbationBooking(
      probationTeam(code = "TEAM_CODE"),
      probationMeetingType = "OTHER",
      createdBy = PRISON_USER_BIRMINGHAM,
      notesForStaff = "Some private staff notes",
      notesForPrisoners = "Some public prisoners notes",
    )

    with(probationBooking) {
      probationTeam isEqualTo probationTeam(code = "TEAM_CODE")
      isBookingType(BookingType.PROBATION) isBool true
      probationMeetingType isEqualTo "OTHER"
      createdBy isEqualTo PRISON_USER_BIRMINGHAM.username
      createdTime isCloseTo now()
      createdByPrison isBool true
      statusCode isEqualTo StatusCode.ACTIVE
      notesForStaff isEqualTo "Some private staff notes"
      notesForPrisoners isEqualTo "Some public prisoners notes"
    }

    probationBooking.amendProbationBooking(
      probationMeetingType = "RR",
      notesForStaff = "Amended staff notes",
      notesForPrisoners = "Amended prisoners notes",
      amendedBy = PROBATION_USER,
    )

    with(probationBooking) {
      probationMeetingType isEqualTo "RR"
      notesForStaff isEqualTo "Amended staff notes"
      notesForPrisoners isEqualTo "Some public prisoners notes"
      amendedBy isEqualTo PROBATION_USER.username
      amendedTime isCloseTo now()
    }
  }

  @Test
  fun `should amend probation created probation booking created by probation user`() {
    val probationBooking = VideoBooking.newProbationBooking(
      probationTeam(code = "TEAM_CODE"),
      probationMeetingType = "OTHER",
      createdBy = PROBATION_USER,
      notesForStaff = "Some private staff notes",
      notesForPrisoners = "Some public prisoners notes",
    )

    with(probationBooking) {
      probationTeam isEqualTo probationTeam(code = "TEAM_CODE")
      isBookingType(BookingType.PROBATION) isBool true
      probationMeetingType isEqualTo "OTHER"
      createdBy isEqualTo PROBATION_USER.username
      createdTime isCloseTo now()
      createdByPrison isBool false
      statusCode isEqualTo StatusCode.ACTIVE
      notesForStaff isEqualTo "Some private staff notes"
      notesForPrisoners isEqualTo null
    }

    probationBooking.amendProbationBooking(
      probationMeetingType = "RR",
      notesForStaff = "Amended staff notes",
      notesForPrisoners = "Amended prisoners notes",
      amendedBy = PROBATION_USER,
    )

    with(probationBooking) {
      probationMeetingType isEqualTo "RR"
      notesForStaff isEqualTo "Amended staff notes"
      notesForPrisoners isEqualTo null
      amendedBy isEqualTo PROBATION_USER.username
      amendedTime isCloseTo now()
    }
  }

  @Test
  fun `should be probation booking created by probation user`() {
    val probationBooking = VideoBooking.newProbationBooking(
      probationTeam(code = "TEAM_CODE"),
      probationMeetingType = "PSR",
      createdBy = PROBATION_USER,
      notesForStaff = "Some private staff notes",
      notesForPrisoners = "Should be ignored for external user",
    )

    with(probationBooking) {
      probationTeam isEqualTo probationTeam(code = "TEAM_CODE")
      isBookingType(BookingType.PROBATION) isBool true
      probationMeetingType isEqualTo "PSR"
      createdBy isEqualTo PROBATION_USER.username
      createdByPrison isBool false
      statusCode isEqualTo StatusCode.ACTIVE
      notesForStaff isEqualTo "Some private staff notes"
      notesForPrisoners isEqualTo null
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
        createdBy = PROBATION_USER,
        notesForStaff = null,
        notesForPrisoners = null,
      )
    }.message isEqualTo "Probation team with code READ_ONLY is read only"
  }

  @Test
  fun `should accept new booking if probation team is not read only`() {
    assertDoesNotThrow {
      VideoBooking.newProbationBooking(
        probationTeam = probationTeam("NOT_READ_ONLY", readOnly = false),
        probationMeetingType = "PSR",
        createdBy = PROBATION_USER,
        notesForStaff = null,
        notesForPrisoners = null,
      )
    }
  }
}
