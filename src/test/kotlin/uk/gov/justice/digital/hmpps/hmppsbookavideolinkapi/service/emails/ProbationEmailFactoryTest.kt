package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.bookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isInstanceOf
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingAction
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewProbationBookingUserEmail
import java.time.LocalDate
import java.time.LocalTime

class ProbationEmailFactoryTest {

  private val userBookingContact = bookingContact(
    contactType = ContactType.USER,
    email = "user@email.com",
    name = "Fred",
  )

  private val courtBookingContact = bookingContact(
    contactType = ContactType.COURT,
    email = "user@email.com",
    name = "Fred",
  )

  private val prisonBookingContact = bookingContact(
    contactType = ContactType.PRISON,
    email = "user@email.com",
    name = "Fred",
  )

  private val probationBookingContact = bookingContact(
    contactType = ContactType.PROBATION,
    email = "user@email.com",
    name = "Fred",
  )

  private val prisoner = prisoner(
    prisonerNumber = "123456",
    prisonCode = BIRMINGHAM,
    firstName = "Fred",
    lastName = "Bloggs",
  )

  private val prison = prison(prisonCode = MOORLAND)

  private val courtBooking = courtBooking()
    .addAppointment(
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      appointmentType = "VLB_COURT_MAIN",
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      locationKey = moorlandLocation.key,
    )

  private val probationBooking = probationBooking()
    .addAppointment(
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      appointmentType = "VLB_PROBATION",
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      locationKey = moorlandLocation.key,
    )

  private val userEmails = mapOf(
    BookingAction.CREATE to NewProbationBookingUserEmail::class.java,
  )

  private val prisonEmails = mapOf(
    BookingAction.CREATE to NewProbationBookingPrisonProbationEmail::class.java,
  )

  companion object {
    @JvmStatic
    fun supportedUserBookingActions() = setOf(BookingAction.CREATE)

    @JvmStatic
    fun unsupportedUserBookingActions() = setOf(BookingAction.RELEASED, BookingAction.TRANSFERRED)

    @JvmStatic
    fun supportedPrisonBookingActions() = setOf(BookingAction.CREATE)

    @JvmStatic
    fun unsupportedProbationBookingActions() = setOf(BookingAction.AMEND, BookingAction.CANCEL)
  }

  @ParameterizedTest
  @MethodSource("supportedUserBookingActions")
  fun `should return user emails for supported user based actions`(action: BookingAction) {
    val email = ProbationEmailFactory.user(
      action = action,
      contact = userBookingContact,
      prisoner = prisoner,
      booking = probationBooking,
      prison = prison,
      appointment = probationBooking.appointments().single(),
      location = moorlandLocation,
    )

    email isInstanceOf userEmails[action]!!
  }

  @Test
  fun `should reject incorrect contact type for user emails`() {
    val error = assertThrows<IllegalArgumentException> {
      ProbationEmailFactory.user(
        action = BookingAction.CREATE,
        contact = prisonBookingContact,
        prisoner = prisoner,
        booking = probationBooking,
        prison = prison,
        appointment = probationBooking.appointments().single(),
        location = birminghamLocation,
      )
    }

    error.message isEqualTo "Incorrect contact type ${prisonBookingContact.contactType} for probation user email"
  }

  @Test
  fun `should reject court video bookings for user emails`() {
    val error = assertThrows<IllegalArgumentException> {
      ProbationEmailFactory.user(
        action = BookingAction.CREATE,
        contact = prisonBookingContact,
        prisoner = prisoner,
        booking = courtBooking,
        prison = prison,
        appointment = courtBooking.appointments().single(),
        location = birminghamLocation,
      )
    }

    error.message isEqualTo "Booking ID 0 is not a probation booking"
  }

  @Test
  fun `should reject court video bookings for probation emails`() {
    val error = assertThrows<IllegalArgumentException> {
      ProbationEmailFactory.prison(
        action = BookingAction.CREATE,
        contact = prisonBookingContact,
        prisoner = prisoner,
        booking = courtBooking,
        prison = prison,
        appointment = courtBooking.appointments().single(),
        location = birminghamLocation,
        contacts = emptySet(),
      )
    }

    error.message isEqualTo "Booking ID 0 is not a probation booking"
  }

  @ParameterizedTest
  @MethodSource("supportedPrisonBookingActions")
  fun `should return prison probation emails for supported user based actions`(action: BookingAction) {
    val email = ProbationEmailFactory.prison(
      action = action,
      contact = prisonBookingContact,
      prisoner = prisoner,
      booking = probationBooking,
      prison = prison,
      appointment = probationBooking.appointments().single(),
      location = moorlandLocation,
      contacts = setOf(probationBookingContact),
    )

    email isInstanceOf prisonEmails[action]!!
  }

  @Test
  fun `should reject incorrect contact type for prison probation emails`() {
    val error = assertThrows<IllegalArgumentException> {
      ProbationEmailFactory.prison(
        action = BookingAction.CREATE,
        contact = courtBookingContact,
        prisoner = prisoner,
        booking = probationBooking,
        prison = prison,
        appointment = probationBooking.appointments().single(),
        location = birminghamLocation,
        contacts = emptySet(),
      )
    }

    error.message isEqualTo "Incorrect contact type ${courtBookingContact.contactType} for prison probation email"
  }
}
