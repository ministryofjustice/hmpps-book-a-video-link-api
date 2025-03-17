package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.TimeSlot
import java.time.LocalDate

class TimeSlotAvailabilityRequestTest : ValidatorBase<TimeSlotAvailabilityRequest>() {

  private val requestThirty = TimeSlotAvailabilityRequest(
    prisonCode = "RSI",
    bookingType = BookingType.PROBATION,
    probationTeamCode = "probation_team_code",
    date = LocalDate.now(),
    bookingDuration = 30,
    timeSlots = listOf(TimeSlot.AM),
  )

  private val requestSixty = requestThirty.copy(bookingDuration = 60)
  private val requestNinety = requestThirty.copy(bookingDuration = 90)
  private val requestOneTwenty = requestThirty.copy(bookingDuration = 120)

  @Test
  fun `should be no errors for valid requests`() {
    assertNoErrors(requestThirty)
    assertNoErrors(requestSixty)
    assertNoErrors(requestNinety)
    assertNoErrors(requestOneTwenty)
  }

  @Test
  fun `should fail when prison code is missing`() {
    requestThirty.copy(prisonCode = null) failsWithSingle ModelError("prisonCode", "The prison code is mandatory")
  }

  @Test
  fun `should fail when booking type is missing`() {
    requestThirty.copy(bookingType = null) failsWithSingle ModelError("bookingType", "The booking type is mandatory")
  }

  @Test
  fun `should fail when court code is missing`() {
    requestThirty.copy(bookingType = BookingType.COURT, courtCode = null) failsWithSingle ModelError("courtBooking", "The court code is mandatory for court bookings")
  }

  @Test
  fun `should fail when probation team code is missing`() {
    requestThirty.copy(bookingType = BookingType.PROBATION, probationTeamCode = null) failsWithSingle ModelError("probationBooking", "The probation team code is mandatory for probation bookings")
  }

  @Test
  fun `should fail when no date provided`() {
    requestThirty.copy(date = null) failsWithSingle ModelError("date", "The date is mandatory")
  }

  @Test
  fun `should fail when date is in the past`() {
    requestThirty.copy(date = yesterday()) failsWithSingle ModelError("date", "The date must be future or present")
  }

  @Test
  fun `should fail when booking not in allowed slots`() {
    IntRange(1, 29).forEach {
      requestThirty.copy(bookingDuration = it) failsWithSingle ModelError("allowedDuration", "The booking duration can only be one of 30, 60, 90 or 120 minutes")
    }

    IntRange(31, 59).forEach {
      requestThirty.copy(bookingDuration = it) failsWithSingle ModelError("allowedDuration", "The booking duration can only be one of 30, 60, 90 or 120 minutes")
    }

    IntRange(61, 89).forEach {
      requestThirty.copy(bookingDuration = it) failsWithSingle ModelError("allowedDuration", "The booking duration can only be one of 30, 60, 90 or 120 minutes")
    }

    IntRange(91, 119).forEach {
      requestThirty.copy(bookingDuration = it) failsWithSingle ModelError("allowedDuration", "The booking duration can only be one of 30, 60, 90 or 120 minutes")
    }

    requestThirty.copy(bookingDuration = 121) failsWithSingle ModelError("allowedDuration", "The booking duration can only be one of 30, 60, 90 or 120 minutes")

    requestThirty.copy(bookingDuration = null) failsWithSingle ModelError("bookingDuration", "The booking duration is mandatory")
  }
}
