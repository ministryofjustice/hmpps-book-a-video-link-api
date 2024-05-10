package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

class CreateVideoBookingRequestTest : ValidatorBase<CreateVideoBookingRequest>() {

  private val appointment = Appointment(
    type = AppointmentType.TBD,
    locationKey = "MDI-A-1-001",
    date = LocalDate.now().plusDays(1),
    startTime = LocalTime.now(),
    endTime = LocalTime.now().plusHours(1),
    videoLinkUrl = "https://video.link.com",
  )

  private val prisoner = PrisonerDetails(
    prisonCode = "MDI",
    prisonerNumber = "123456",
    appointments = listOf(appointment),
  )

  private val request = CreateVideoBookingRequest(
    bookingType = BookingType.COURT,
    prisoners = listOf(prisoner),
    comments = "Blah de blah",
  )

  @Test
  fun `should be no errors for a valid request`() {
    assertNoErrors(request)
  }

  @Test
  fun `should fail when missing booking type`() {
    request.copy(bookingType = null) failsWithSingle ModelError("bookingType", "The video link booking type is mandatory")
  }

  @Test
  fun `should fail when missing prisoners`() {
    request.copy(prisoners = emptyList()) failsWithSingle ModelError("prisoners", "At least one prisoner must be supplied for a video link booking")
  }

  @Test
  fun `should fail when comments too long`() {
    request.copy(comments = "a".repeat(401)) failsWithSingle ModelError("comments", "Comments for the video link booking cannot not exceed 400 characters")
  }

  @Test
  fun `should fail when prisoner missing prison code`() {
    request.copy(prisoners = listOf(prisoner.copy(prisonCode = null))) failsWithSingle ModelError("prisoners[0].prisonCode", "Prison code is mandatory")
  }

  @Test
  fun `should fail when prisoner missing prisoner number`() {
    request.copy(prisoners = listOf(prisoner.copy(prisonerNumber = null))) failsWithSingle ModelError("prisoners[0].prisonerNumber", "Prisoner number is mandatory")
  }

  @Test
  fun `should fail when prisoner missing appointments`() {
    request.copy(prisoners = listOf(prisoner.copy(appointments = emptyList()))) failsWithSingle ModelError("prisoners[0].appointments", "At least one appointment must be supplied for the prisoner")
  }

  @Test
  fun `should fail when appointment type is missing`() {
    request.copy(prisoners = listOf(prisoner.copy(appointments = listOf(appointment.copy(type = null))))) failsWithSingle ModelError("prisoners[0].appointments[0].type", "The appointment type for the appointment is mandatory")
  }

  @Test
  fun `should fail when appointment location key is missing`() {
    request.copy(prisoners = listOf(prisoner.copy(appointments = listOf(appointment.copy(locationKey = null))))) failsWithSingle ModelError("prisoners[0].appointments[0].locationKey", "The location key for the appointment is mandatory")
  }

  @Test
  fun `should fail when location key too long`() {
    request.copy(prisoners = listOf(prisoner.copy(appointments = listOf(appointment.copy(locationKey = "a".repeat(161)))))) failsWithSingle ModelError("prisoners[0].appointments[0].locationKey", "The location key should not exceed 160 characters")
  }

  @Test
  fun `should fail when appointment date is missing`() {
    request.copy(prisoners = listOf(prisoner.copy(appointments = listOf(appointment.copy(date = null))))) failsWithSingle ModelError("prisoners[0].appointments[0].date", "The date for the appointment is mandatory")
  }

  @Test
  fun `should fail when appointment date is not in the future`() {
    request.copy(prisoners = listOf(prisoner.copy(appointments = listOf(appointment.copy(date = LocalDate.now()))))) failsWithSingle ModelError("prisoners[0].appointments[0].date", "The date for the appointment must be in the future")
  }

  @Test
  fun `should fail when appointment start time is missing`() {
    request.copy(prisoners = listOf(prisoner.copy(appointments = listOf(appointment.copy(startTime = null))))) failsWithSingle ModelError("prisoners[0].appointments[0].startTime", "The start time for the appointment is mandatory")
  }

  @Test
  fun `should fail when appointment end time is missing`() {
    request.copy(prisoners = listOf(prisoner.copy(appointments = listOf(appointment.copy(endTime = null))))) failsWithSingle ModelError("prisoners[0].appointments[0].endTime", "The end time for the appointment is mandatory")
  }

  @Test
  fun `should fail when appointment video link URL is invalid`() {
    request.copy(prisoners = listOf(prisoner.copy(appointments = listOf(appointment.copy(videoLinkUrl = "blah"))))) failsWithSingle ModelError("prisoners[0].appointments[0].validUrl", "The supplied video link for the appointment is not a valid URL")
  }

  @Test
  fun `should fail when appointment video link URL is too long`() {
    request.copy(prisoners = listOf(prisoner.copy(appointments = listOf(appointment.copy(videoLinkUrl = "https://".plus("a".repeat(120).plus(".com"))))))) failsWithSingle ModelError("prisoners[0].appointments[0].videoLinkUrl", "The video link should not exceed 120 characters")
  }

  @Test
  fun `should fail when appointment end time is not after start time`() {
    request.copy(prisoners = listOf(prisoner.copy(appointments = listOf(appointment.copy(startTime = LocalTime.of(12, 12), endTime = LocalTime.of(12, 12)))))) failsWithSingle ModelError("prisoners[0].appointments[0].validTime", "The end time must be after the start time for the appointment")
  }
}
