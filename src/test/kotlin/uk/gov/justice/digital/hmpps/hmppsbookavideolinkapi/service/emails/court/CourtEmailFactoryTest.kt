package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.CvpLinkDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.bookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsEntry
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isInstanceOf
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingAction
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
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

  private val courtBooking = courtBooking(notesForStaff = "Court hearing staff notes", cvpLinkDetails = CvpLinkDetails.hmctsNumber("54321"))
    .addAppointment(
      prison = prison(prisonCode = BIRMINGHAM),
      prisonerNumber = "123456",
      appointmentType = "VLB_COURT_MAIN",
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      locationId = wandsworthLocation.id,
    )

  private val probationBooking = probationBooking(notesForStaff = "Court hearing staff notes")
    .addAppointment(
      prison = prison(prisonCode = BIRMINGHAM),
      prisonerNumber = "123456",
      appointmentType = "VLB_PROBATION",
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      locationId = wandsworthLocation.id,
    )

  private val prison = prison(prisonCode = WANDSWORTH)

  companion object {
    @JvmStatic
    fun supportedUserBookingActions() = setOf(BookingAction.CREATE, BookingAction.AMEND, BookingAction.CANCEL)

    @JvmStatic
    fun unsupportedUserBookingActions() = setOf(BookingAction.RELEASED, BookingAction.TRANSFERRED, BookingAction.COURT_HEARING_LINK_REMINDER)

    @JvmStatic
    fun supportedCourtBookingActions() = setOf(BookingAction.CREATE, BookingAction.AMEND, BookingAction.CANCEL, BookingAction.RELEASED, BookingAction.TRANSFERRED, BookingAction.COURT_HEARING_LINK_REMINDER)
  }

  private val userEmails = mapOf(
    BookingAction.CREATE to NewCourtBookingUserEmail::class.java,
    BookingAction.AMEND to AmendedCourtBookingUserEmail::class.java,
    BookingAction.CANCEL to CancelledCourtBookingUserEmail::class.java,
  )

  private val courtEmails = mapOf(
    BookingAction.CREATE to NewCourtBookingCourtEmail::class.java,
    BookingAction.AMEND to AmendedCourtBookingCourtEmail::class.java,
    BookingAction.CANCEL to CancelledCourtBookingCourtEmail::class.java,
    BookingAction.RELEASED to ReleasedCourtBookingCourtEmail::class.java,
    BookingAction.TRANSFERRED to TransferredCourtBookingCourtEmail::class.java,
    BookingAction.COURT_HEARING_LINK_REMINDER to CourtHearingLinkReminderEmail::class.java,
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
      locations = mapOf(wandsworthLocation.id to wandsworthLocation.toModel()),
    )

    email isInstanceOf userEmails[action]!!

    email?.personalisation()!! containsEntry Pair("comments", "Court hearing staff notes")

    if (listOf(BookingAction.CREATE, BookingAction.AMEND, BookingAction.CANCEL).contains(action)) {
      email.personalisation() containsEntry Pair("courtHearingLink", "https://join.meet.video.justice.gov.uk/#?conference=HMCTS54321@meet.video.justice.gov.uk")
    }
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
      locations = mapOf(wandsworthLocation.id to wandsworthLocation.toModel()),
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
        locations = mapOf(wandsworthLocation.id to wandsworthLocation.toModel()),
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
        locations = mapOf(wandsworthLocation.id to wandsworthLocation.toModel()),
      )
    }

    error.message isEqualTo "Booking ID 0 is not a court booking"
  }

  @ParameterizedTest
  @MethodSource("supportedCourtBookingActions")
  fun `should return court emails for supported court based actions with staff notes and HMCTS number`(action: BookingAction) {
    val email = CourtEmailFactory.court(
      action = action,
      contact = courtBookingContact,
      prisoner = prisoner,
      booking = if (action == BookingAction.CANCEL || action == BookingAction.RELEASED || action == BookingAction.TRANSFERRED) courtBooking.apply { cancel(COURT_USER) } else courtBooking,
      prison = prison,
      pre = null,
      main = courtBooking.appointments().single(),
      post = null,
      locations = mapOf(wandsworthLocation.id to wandsworthLocation.toModel()),
    )

    email isInstanceOf courtEmails[action]!!

    email?.personalisation()!! containsEntry Pair("comments", "Court hearing staff notes")

    if (listOf(BookingAction.CREATE, BookingAction.AMEND, BookingAction.CANCEL).contains(action)) {
      email.personalisation() containsEntry Pair("courtHearingLink", "https://join.meet.video.justice.gov.uk/#?conference=HMCTS54321@meet.video.justice.gov.uk")
    }
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
        locations = mapOf(wandsworthLocation.id to wandsworthLocation.toModel()),
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
        locations = mapOf(wandsworthLocation.id to wandsworthLocation.toModel()),
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
        locations = mapOf(wandsworthLocation.id to wandsworthLocation.toModel()),
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
        locations = mapOf(wandsworthLocation.id to wandsworthLocation.toModel()),
        contacts = listOf(bookingContact(ContactType.PRISON, email = "contact@email.com")),
      )
    }

    error.message isEqualTo "Booking ID 0 is not a court booking"
  }
}
