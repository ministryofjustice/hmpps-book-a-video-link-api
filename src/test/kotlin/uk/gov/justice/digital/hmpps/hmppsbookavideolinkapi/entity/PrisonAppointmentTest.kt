package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday
import java.time.LocalDateTime
import java.time.LocalTime

class PrisonAppointmentTest {

  private val now = LocalDateTime.now()
  private val booking = probationBooking()

  @Test
  fun `should start after current date and time`() {
    appointment(
      booking,
      BIRMINGHAM,
      "123456",
      date = tomorrow(),
      startTime = LocalTime.now(),
      endTime = LocalTime.now().plusMinutes(1),
      appointmentType = "VLB_PROBATION",
      locationKey = "LOCATION-KEY",
    ).isStartsAfter(now) isBool true

    appointment(
      booking,
      BIRMINGHAM,
      "123456",
      date = today(),
      startTime = now.toLocalTime().plusHours(1),
      endTime = now.toLocalTime().plusHours(2),
      appointmentType = "VLB_PROBATION",
      locationKey = "LOCATION-KEY",
    ).isStartsAfter(now) isBool true
  }

  @Test
  fun `should start before current date and time`() {
    appointment(
      booking,
      BIRMINGHAM,
      "123456",
      date = yesterday(),
      startTime = LocalTime.now(),
      endTime = LocalTime.now().plusMinutes(1),
      appointmentType = "VLB_PROBATION",
      locationKey = "LOCATION-KEY",
    ).isStartsAfter(now) isBool false

    appointment(
      booking,
      BIRMINGHAM,
      "123456",
      date = now.toLocalDate(),
      startTime = now.toLocalTime(),
      endTime = now.toLocalTime().plusMinutes(1),
      appointmentType = "VLB_PROBATION",
      locationKey = "LOCATION-KEY",
    ).isStartsAfter(now) isBool false
  }
}
