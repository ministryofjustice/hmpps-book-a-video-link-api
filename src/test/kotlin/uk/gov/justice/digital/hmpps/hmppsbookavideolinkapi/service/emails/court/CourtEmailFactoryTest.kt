package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.bookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isInstanceOf
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingAction
import java.time.LocalDate
import java.time.LocalTime

class CourtEmailFactoryTest {

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

  private val prisoner = prisoner(
    prisonerNumber = "123456",
    prisonCode = BIRMINGHAM,
    firstName = "Fred",
    lastName = "Bloggs",
  )

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

  private val prison = prison(prisonCode = MOORLAND)

  companion object {
    @JvmStatic
    fun supportedUserBookingActions() = setOf(BookingAction.CREATE, BookingAction.AMEND, BookingAction.CANCEL)

    @JvmStatic
    fun unsupportedUserBookingActions() = setOf(BookingAction.RELEASED, BookingAction.TRANSFERRED)

    @JvmStatic
    fun supportedCourtBookingActions() = setOf(BookingAction.CREATE, BookingAction.RELEASED, BookingAction.TRANSFERRED)

    @JvmStatic
    fun unsupportedCourtBookingActions() = setOf(BookingAction.AMEND, BookingAction.CANCEL)
  }

  private val userEmails = mapOf(
    BookingAction.CREATE to NewCourtBookingUserEmail::class.java,
    BookingAction.AMEND to AmendedCourtBookingUserEmail::class.java,
    BookingAction.CANCEL to CancelledCourtBookingUserEmail::class.java,
  )

  private val courtEmails = mapOf(
    BookingAction.CREATE to NewCourtBookingCourtEmail::class.java,
    BookingAction.RELEASED to ReleasedCourtBookingCourtEmail::class.java,
    BookingAction.TRANSFERRED to TransferredCourtBookingCourtEmail::class.java,
  )

  @ParameterizedTest
  @MethodSource("supportedUserBookingActions")
  fun `should return user emails for supported user based actions`(action: BookingAction) {
    val email = CourtEmailFactory.user(
      action = action,
      contact = userBookingContact,
      prisoner = prisoner,
      booking = if (action == BookingAction.CANCEL) courtBooking.cancel(COURT_USER) else courtBooking,
      prison = prison,
      pre = null,
      main = courtBooking.appointments().single(),
      post = null,
      locations = mapOf(moorlandLocation.key to moorlandLocation),
    )

    email isInstanceOf userEmails[action]!!
  }

  @ParameterizedTest
  @MethodSource("unsupportedUserBookingActions")
  fun `should return no email for unsupported user based actions`(action: BookingAction) {
    val email = CourtEmailFactory.user(
      action = action,
      contact = userBookingContact,
      prisoner = prisoner,
      booking = courtBooking,
      prison = prison,
      pre = null,
      main = courtBooking.appointments().single(),
      post = null,
      locations = mapOf(moorlandLocation.key to moorlandLocation),
    )

    email isEqualTo null
  }

  @Test
  fun `should reject incorrect contact type for user emails`() {
    val error = assertThrows<IllegalArgumentException> {
      CourtEmailFactory.user(
        action = BookingAction.CREATE,
        contact = prisonBookingContact,
        prisoner = prisoner,
        booking = courtBooking,
        prison = prison,
        pre = null,
        main = courtBooking.appointments().single(),
        post = null,
        locations = mapOf(moorlandLocation.key to moorlandLocation),
      )
    }

    error.message isEqualTo "Incorrect contact type ${prisonBookingContact.contactType} for court user email"
  }

  @Test
  fun `should reject probation video bookings for user emails`() {
    val error = assertThrows<IllegalArgumentException> {
      CourtEmailFactory.user(
        action = BookingAction.CREATE,
        contact = userBookingContact,
        prisoner = prisoner,
        booking = probationBooking,
        prison = prison,
        pre = null,
        main = probationBooking.appointments().single(),
        post = null,
        locations = mapOf(moorlandLocation.key to moorlandLocation),
      )
    }

    error.message isEqualTo "Booking ID 0 is not a court booking"
  }

  @ParameterizedTest
  @MethodSource("supportedCourtBookingActions")
  fun `should return court emails for supported court based actions`(action: BookingAction) {
    val email = CourtEmailFactory.court(
      action = action,
      contact = courtBookingContact,
      prisoner = prisoner,
      booking = courtBooking,
      prison = prison,
      pre = null,
      main = courtBooking.appointments().single(),
      post = null,
      locations = mapOf(moorlandLocation.key to moorlandLocation),
    )

    email isInstanceOf courtEmails[action]!!
  }

  @ParameterizedTest
  @MethodSource("unsupportedCourtBookingActions")
  fun `should return no email for unsupported court based actions`(action: BookingAction) {
    val email = CourtEmailFactory.court(
      action = action,
      contact = courtBookingContact,
      prisoner = prisoner,
      booking = courtBooking,
      prison = prison,
      pre = null,
      main = courtBooking.appointments().single(),
      post = null,
      locations = mapOf(moorlandLocation.key to moorlandLocation),
    )

    email isEqualTo null
  }

  @Test
  fun `should reject incorrect contact type for court emails`() {
    val error = assertThrows<IllegalArgumentException> {
      CourtEmailFactory.court(
        action = BookingAction.CREATE,
        contact = userBookingContact,
        prisoner = prisoner,
        booking = courtBooking,
        prison = prison,
        pre = null,
        main = courtBooking.appointments().single(),
        post = null,
        locations = mapOf(moorlandLocation.key to moorlandLocation),
      )
    }

    error.message isEqualTo "Incorrect contact type ${userBookingContact.contactType} for court court email"
  }

  @Test
  fun `should reject probation video bookings for court emails`() {
    val error = assertThrows<IllegalArgumentException> {
      CourtEmailFactory.court(
        action = BookingAction.CREATE,
        contact = userBookingContact,
        prisoner = prisoner,
        booking = probationBooking,
        prison = prison,
        pre = null,
        main = probationBooking.appointments().single(),
        post = null,
        locations = mapOf(moorlandLocation.key to moorlandLocation),
      )
    }

    error.message isEqualTo "Booking ID 0 is not a court booking"
  }

  @Test
  fun `should reject incorrect contact type for prison emails`() {
    val error = assertThrows<IllegalArgumentException> {
      CourtEmailFactory.prison(
        action = BookingAction.CREATE,
        contact = userBookingContact,
        prisoner = prisoner,
        booking = courtBooking,
        prison = prison,
        pre = null,
        main = courtBooking.appointments().single(),
        post = null,
        locations = mapOf(moorlandLocation.key to moorlandLocation),
        contacts = listOf(bookingContact(ContactType.PRISON, email = "contact@email.com")),
      )
    }

    error.message isEqualTo "Incorrect contact type ${userBookingContact.contactType} for court prison email"
  }

  @Test
  fun `should reject probation video bookings for prison emails`() {
    val error = assertThrows<IllegalArgumentException> {
      CourtEmailFactory.prison(
        action = BookingAction.CREATE,
        contact = userBookingContact,
        prisoner = prisoner,
        booking = probationBooking,
        prison = prison,
        pre = null,
        main = probationBooking.appointments().single(),
        post = null,
        locations = mapOf(moorlandLocation.key to moorlandLocation),
        contacts = listOf(bookingContact(ContactType.PRISON, email = "contact@email.com")),
      )
    }

    error.message isEqualTo "Booking ID 0 is not a court booking"
  }
}
