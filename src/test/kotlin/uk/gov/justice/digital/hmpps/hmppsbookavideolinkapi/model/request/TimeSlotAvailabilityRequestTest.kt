package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.TimeSlot
import java.time.LocalDate

class TimeSlotAvailabilityRequestTest : ValidatorBase<TimeSlotAvailabilityRequest>() {

  private val request = TimeSlotAvailabilityRequest(
    prisonCode = "RSI",
    bookingType = BookingType.PROBATION,
    probationTeamCode = "probation_team_code",
    date = LocalDate.now(),
    bookingDuration = 15,
    timeSlots = listOf(TimeSlot.AM),
  )

  @Test
  fun `should be no errors for valid requests`() {
    assertNoErrors(request)

    // Duration must be a multiple of 15
    listOf(15, 30, 45, 60, 75, 90, 105, 120, 135, 150, 165, 180).forEach {
      assertNoErrors(request.copy(bookingDuration = it))
    }
  }

  @Test
  fun `should fail when prison code is missing`() {
    request.copy(prisonCode = null) failsWithSingle ModelError("prisonCode", "The prison code is mandatory")
  }

  @Test
  fun `should fail when booking type is missing`() {
    request.copy(bookingType = null) failsWithSingle ModelError("bookingType", "The booking type is mandatory")
  }

  @Test
  fun `should fail when court code is missing`() {
    request.copy(bookingType = BookingType.COURT, courtCode = null) failsWithSingle ModelError("courtBooking", "The court code is mandatory for court bookings")
  }

  @Test
  fun `should fail when probation team code is missing`() {
    request.copy(bookingType = BookingType.PROBATION, probationTeamCode = null) failsWithSingle ModelError("probationBooking", "The probation team code is mandatory for probation bookings")
  }

  @Test
  fun `should fail when no date provided`() {
    request.copy(date = null) failsWithSingle ModelError("date", "The date is mandatory")
  }

  @Test
  fun `should fail when date is in the past`() {
    request.copy(date = yesterday()) failsWithSingle ModelError("date", "The date must be future or present")
  }

  @Test
  fun `should fail when booking not in allowed slots`() {
    IntRange(1, 1000).filterNot { it.mod(15) == 0 }.forEach {
      request.copy(bookingDuration = it) failsWithSingle ModelError("allowedDuration", "The booking duration must be a multiple of 15 minutes")
    }

    request.copy(bookingDuration = null) failsWithSingle ModelError("bookingDuration", "The booking duration is mandatory")
  }
}
