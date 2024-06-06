package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday
import java.time.LocalTime

class CreateVideoBookingRequestTest : ValidatorBase<CreateVideoBookingRequest>() {

  private val appointment = Appointment(
    type = AppointmentType.VLB_COURT_MAIN,
    locationKey = "MDI-A-1-001",
    date = tomorrow(),
    startTime = LocalTime.now(),
    endTime = LocalTime.now().plusHours(1),
  )

  private val prisoner = PrisonerDetails(
    prisonCode = "MDI",
    prisonerNumber = "123456",
    appointments = listOf(appointment),
  )

  private val courtBooking = CreateVideoBookingRequest(
    bookingType = BookingType.COURT,
    courtCode = "ABC",
    courtHearingType = CourtHearingType.TRIBUNAL,
    prisoners = listOf(prisoner),
    comments = "Blah de blah",
    videoLinkUrl = "https://video.link.com",
  )

  private val probationBooking = CreateVideoBookingRequest(
    bookingType = BookingType.PROBATION,
    probationTeamCode = "DEF",
    probationMeetingType = ProbationMeetingType.RR,
    prisoners = listOf(prisoner),
    comments = "Blah de blah",
    videoLinkUrl = "https://video.link.com",
  )

  @Test
  fun `should be no errors for a valid court booking request`() {
    assertNoErrors(courtBooking)
  }

  @Test
  fun `should be no errors for a valid probation booking request`() {
    assertNoErrors(probationBooking)
  }

  @Test
  fun `should fail when missing booking type`() {
    courtBooking.copy(bookingType = null) failsWithSingle ModelError("bookingType", "The video link booking type is mandatory")
  }

  @Test
  fun `should fail when court booking missing court code`() {
    courtBooking.copy(courtCode = null) failsWithSingle ModelError("invalidCourtBooking", "The court code and court hearing type are mandatory for court bookings")
  }

  @Test
  fun `should fail when court booking missing court hearing type code`() {
    courtBooking.copy(courtHearingType = null) failsWithSingle ModelError("invalidCourtBooking", "The court code and court hearing type are mandatory for court bookings")
  }

  @Test
  fun `should fail when probation booking missing probation team code`() {
    probationBooking.copy(probationTeamCode = null) failsWithSingle ModelError("invalidProbationBooking", "The probation team code and probation meeting type are mandatory for probation bookings")
  }

  @Test
  fun `should fail when probation booking missing probation meeting type code`() {
    probationBooking.copy(probationMeetingType = null) failsWithSingle ModelError("invalidProbationBooking", "The probation team code and probation meeting type are mandatory for probation bookings")
  }

  @Test
  fun `should fail when missing prisoners`() {
    courtBooking.copy(prisoners = emptyList()) failsWithSingle ModelError("prisoners", "At least one prisoner must be supplied for a video link booking")
  }

  @Test
  fun `should fail when comments too long`() {
    courtBooking.copy(comments = "a".repeat(401)) failsWithSingle ModelError("comments", "Comments for the video link booking cannot not exceed 400 characters")
  }

  @Test
  fun `should fail when prisoner missing prison code`() {
    courtBooking.copy(prisoners = listOf(prisoner.copy(prisonCode = null))) failsWithSingle ModelError("prisoners[0].prisonCode", "Prison code is mandatory")
  }

  @Test
  fun `should fail when prisoner missing prisoner number`() {
    courtBooking.copy(prisoners = listOf(prisoner.copy(prisonerNumber = null))) failsWithSingle ModelError("prisoners[0].prisonerNumber", "Prisoner number is mandatory")
  }

  @Test
  fun `should fail when prisoner missing appointments`() {
    courtBooking.copy(prisoners = listOf(prisoner.copy(appointments = emptyList()))) failsWithSingle ModelError("prisoners[0].appointments", "At least one appointment must be supplied for the prisoner")
  }

  @Test
  fun `should fail when appointment location key is missing`() {
    courtBooking.copy(prisoners = listOf(prisoner.copy(appointments = listOf(appointment.copy(locationKey = null))))) failsWithSingle ModelError("prisoners[0].appointments[0].locationKey", "The location key for the appointment is mandatory")
  }

  @Test
  fun `should fail when appointment type is missing`() {
    courtBooking.copy(prisoners = listOf(prisoner.copy(appointments = listOf(appointment.copy(type = null))))) failsWithSingle ModelError("prisoners[0].appointments[0].type", "The appointment type for the appointment is mandatory")
  }

  @Test
  fun `should fail when location key too long`() {
    courtBooking.copy(prisoners = listOf(prisoner.copy(appointments = listOf(appointment.copy(locationKey = "a".repeat(161)))))) failsWithSingle ModelError("prisoners[0].appointments[0].locationKey", "The location key should not exceed 160 characters")
  }

  @Test
  fun `should fail when appointment date is missing`() {
    courtBooking.copy(prisoners = listOf(prisoner.copy(appointments = listOf(appointment.copy(date = null))))) failsWithSingle ModelError("prisoners[0].appointments[0].date", "The date for the appointment is mandatory")
  }

  @Test
  fun `should fail when appointment date is in the past`() {
    courtBooking.copy(prisoners = listOf(prisoner.copy(appointments = listOf(appointment.copy(date = yesterday()))))) failsWithSingle ModelError("prisoners[0].appointments[0].date", "The combination of date and start time for the appointment must be in the future")
  }

  @Test
  fun `should fail when appointment date and time is not in the future`() {
    courtBooking.copy(
      prisoners = listOf(prisoner.copy(appointments = listOf(appointment.copy(date = today(), startTime = LocalTime.now().minusSeconds(1))))),
    ) failsWithSingle ModelError("prisoners[0].appointments[0].invalidStart", "The combination of date and start time for the appointment must be in the future")
  }

  @Test
  fun `should fail when appointment start time is missing`() {
    courtBooking.copy(prisoners = listOf(prisoner.copy(appointments = listOf(appointment.copy(startTime = null))))) failsWithSingle ModelError("prisoners[0].appointments[0].startTime", "The start time for the appointment is mandatory")
  }

  @Test
  fun `should fail when appointment end time is missing`() {
    courtBooking.copy(prisoners = listOf(prisoner.copy(appointments = listOf(appointment.copy(endTime = null))))) failsWithSingle ModelError("prisoners[0].appointments[0].endTime", "The end time for the appointment is mandatory")
  }

  @Test
  fun `should fail when appointment video link URL is invalid`() {
    courtBooking.copy(videoLinkUrl = "blah") failsWithSingle ModelError("invalidUrl", "The supplied video link for the appointment is not a valid URL")
  }

  @Test
  fun `should fail when appointment video link URL is too long`() {
    courtBooking.copy(videoLinkUrl = "https://".plus("a".repeat(120).plus(".com"))) failsWithSingle ModelError("videoLinkUrl", "The video link should not exceed 120 characters")
  }

  @Test
  fun `should fail when appointment end time is not after start time`() {
    courtBooking.copy(prisoners = listOf(prisoner.copy(appointments = listOf(appointment.copy(startTime = LocalTime.of(12, 12), endTime = LocalTime.of(12, 12)))))) failsWithSingle ModelError("prisoners[0].appointments[0].invalidTime", "The end time must be after the start time for the appointment")
  }
}
