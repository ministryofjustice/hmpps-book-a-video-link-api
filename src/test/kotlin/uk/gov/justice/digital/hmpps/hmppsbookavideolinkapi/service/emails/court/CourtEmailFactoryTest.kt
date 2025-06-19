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

  private val courtBookingWithCommentsAndVideoUrl = courtBooking(comments = "Court hearing comments", cvpLinkDetails = CvpLinkDetails.url("cvp-video-url"))
    .addAppointment(
      prison = prison(prisonCode = BIRMINGHAM),
      prisonerNumber = "123456",
      appointmentType = "VLB_COURT_MAIN",
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      locationId = wandsworthLocation.id,
    )

  private val courtBookingWithStaffNotesAndHmcts = courtBooking(notesForStaff = "Court hearing staff notes", cvpLinkDetails = CvpLinkDetails.hmctsNumber("54321"))
    .addAppointment(
      prison = prison(prisonCode = BIRMINGHAM),
      prisonerNumber = "123456",
      appointmentType = "VLB_COURT_MAIN",
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      locationId = wandsworthLocation.id,
    )

  private val probationBookingWithComments = probationBooking(comments = "Probation meeting comments")
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

  private val factory = CourtEmailFactory("default-video-url")

  companion object {
    @JvmStatic
    fun supportedUserBookingActions() = setOf(BookingAction.CREATE, BookingAction.AMEND, BookingAction.CANCEL)

    @JvmStatic
    fun unsupportedUserBookingActions() = setOf(BookingAction.RELEASED, BookingAction.TRANSFERRED, BookingAction.COURT_HEARING_LINK_REMINDER)

    @JvmStatic
    fun supportedCourtBookingActions() = setOf(BookingAction.CREATE, BookingAction.CANCEL, BookingAction.RELEASED, BookingAction.TRANSFERRED, BookingAction.COURT_HEARING_LINK_REMINDER)
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
  fun `should return user emails for supported user based actions using comments`(action: BookingAction) {
    val email = factory.user(
      action = action,
      contact = userBookingContact,
      prisoner = prisoner,
      booking = if (action == BookingAction.CANCEL) courtBookingWithCommentsAndVideoUrl.cancel(COURT_USER) else courtBookingWithCommentsAndVideoUrl,
      prison = prison,
      pre = null,
      main = courtBookingWithCommentsAndVideoUrl.appointments().single(),
      post = null,
      locations = mapOf(wandsworthLocation.id to wandsworthLocation.toModel()),
    )

    email isInstanceOf userEmails[action]!!

    email?.personalisation()!! containsEntry Pair("comments", "Court hearing comments")
  }

  @ParameterizedTest
  @MethodSource("supportedUserBookingActions")
  fun `should return user emails for supported user based actions using staff notes`(action: BookingAction) {
    val email = factory.user(
      action = action,
      contact = userBookingContact,
      prisoner = prisoner,
      booking = if (action == BookingAction.CANCEL) courtBookingWithStaffNotesAndHmcts.cancel(COURT_USER) else courtBookingWithStaffNotesAndHmcts,
      prison = prison,
      pre = null,
      main = courtBookingWithCommentsAndVideoUrl.appointments().single(),
      post = null,
      locations = mapOf(wandsworthLocation.id to wandsworthLocation.toModel()),
    )

    email isInstanceOf userEmails[action]!!

    email?.personalisation()!! containsEntry Pair("comments", "Court hearing staff notes")
  }

  @ParameterizedTest
  @MethodSource("unsupportedUserBookingActions")
  fun `should return no email for unsupported user based actions`(action: BookingAction) {
    val email = factory.user(
      action = action,
      contact = userBookingContact,
      prisoner = prisoner,
      booking = courtBookingWithCommentsAndVideoUrl,
      prison = prison,
      pre = null,
      main = courtBookingWithCommentsAndVideoUrl.appointments().single(),
      post = null,
      locations = mapOf(wandsworthLocation.id to wandsworthLocation.toModel()),
    )

    email isEqualTo null
  }

  @Test
  fun `should reject incorrect contact type for user emails`() {
    val error = assertThrows<IllegalArgumentException> {
      factory.user(
        action = BookingAction.CREATE,
        contact = prisonBookingContact,
        prisoner = prisoner,
        booking = courtBookingWithCommentsAndVideoUrl,
        prison = prison,
        pre = null,
        main = courtBookingWithCommentsAndVideoUrl.appointments().single(),
        post = null,
        locations = mapOf(wandsworthLocation.id to wandsworthLocation.toModel()),
      )
    }

    error.message isEqualTo "Incorrect contact type ${prisonBookingContact.contactType} for court user email"
  }

  @Test
  fun `should reject probation video bookings for user emails`() {
    val error = assertThrows<IllegalArgumentException> {
      factory.user(
        action = BookingAction.CREATE,
        contact = userBookingContact,
        prisoner = prisoner,
        booking = probationBookingWithComments,
        prison = prison,
        pre = null,
        main = probationBookingWithComments.appointments().single(),
        post = null,
        locations = mapOf(wandsworthLocation.id to wandsworthLocation.toModel()),
      )
    }

    error.message isEqualTo "Booking ID 0 is not a court booking"
  }

  @ParameterizedTest
  @MethodSource("supportedCourtBookingActions")
  fun `should return court emails for supported court based actions with comments and full video URL`(action: BookingAction) {
    val email = factory.court(
      action = action,
      contact = courtBookingContact,
      prisoner = prisoner,
      booking = if (action == BookingAction.CANCEL || action == BookingAction.RELEASED || action == BookingAction.TRANSFERRED) courtBookingWithCommentsAndVideoUrl.apply { cancel(COURT_USER) } else courtBookingWithCommentsAndVideoUrl,
      prison = prison,
      pre = null,
      main = courtBookingWithCommentsAndVideoUrl.appointments().single(),
      post = null,
      locations = mapOf(wandsworthLocation.id to wandsworthLocation.toModel()),
    )

    email isInstanceOf courtEmails[action]!!

    email?.personalisation()!! containsEntry Pair("comments", "Court hearing comments")

    if (listOf(BookingAction.CREATE, BookingAction.CANCEL).contains(action)) {
      email.personalisation() containsEntry Pair("courtHearingLink", "cvp-video-url")
    }
  }

  @ParameterizedTest
  @MethodSource("supportedCourtBookingActions")
  fun `should return court emails for supported court based actions with staff notes and HMCTS number`(action: BookingAction) {
    val email = factory.court(
      action = action,
      contact = courtBookingContact,
      prisoner = prisoner,
      booking = if (action == BookingAction.CANCEL || action == BookingAction.RELEASED || action == BookingAction.TRANSFERRED) courtBookingWithStaffNotesAndHmcts.apply { cancel(COURT_USER) } else courtBookingWithStaffNotesAndHmcts,
      prison = prison,
      pre = null,
      main = courtBookingWithCommentsAndVideoUrl.appointments().single(),
      post = null,
      locations = mapOf(wandsworthLocation.id to wandsworthLocation.toModel()),
    )

    email isInstanceOf courtEmails[action]!!

    email?.personalisation()!! containsEntry Pair("comments", "Court hearing staff notes")

    if (listOf(BookingAction.CREATE, BookingAction.CANCEL).contains(action)) {
      email.personalisation() containsEntry Pair("courtHearingLink", "HMCTS54321@default-video-url")
    }
  }

  @Test
  fun `should reject incorrect contact type for court emails`() {
    val error = assertThrows<IllegalArgumentException> {
      factory.court(
        action = BookingAction.CREATE,
        contact = userBookingContact,
        prisoner = prisoner,
        booking = courtBookingWithCommentsAndVideoUrl,
        prison = prison,
        pre = null,
        main = courtBookingWithCommentsAndVideoUrl.appointments().single(),
        post = null,
        locations = mapOf(wandsworthLocation.id to wandsworthLocation.toModel()),
      )
    }

    error.message isEqualTo "Incorrect contact type ${userBookingContact.contactType} for court court email"
  }

  @Test
  fun `should reject probation video bookings for court emails`() {
    val error = assertThrows<IllegalArgumentException> {
      factory.court(
        action = BookingAction.CREATE,
        contact = userBookingContact,
        prisoner = prisoner,
        booking = probationBookingWithComments,
        prison = prison,
        pre = null,
        main = probationBookingWithComments.appointments().single(),
        post = null,
        locations = mapOf(wandsworthLocation.id to wandsworthLocation.toModel()),
      )
    }

    error.message isEqualTo "Booking ID 0 is not a court booking"
  }

  @Test
  fun `should reject incorrect contact type for prison emails`() {
    val error = assertThrows<IllegalArgumentException> {
      factory.prison(
        action = BookingAction.CREATE,
        contact = userBookingContact,
        prisoner = prisoner,
        booking = courtBookingWithCommentsAndVideoUrl,
        prison = prison,
        pre = null,
        main = courtBookingWithCommentsAndVideoUrl.appointments().single(),
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
      factory.prison(
        action = BookingAction.CREATE,
        contact = userBookingContact,
        prisoner = prisoner,
        booking = probationBookingWithComments,
        prison = prison,
        pre = null,
        main = probationBookingWithComments.appointments().single(),
        post = null,
        locations = mapOf(wandsworthLocation.id to wandsworthLocation.toModel()),
        contacts = listOf(bookingContact(ContactType.PRISON, email = "contact@email.com")),
      )
    }

    error.message isEqualTo "Booking ID 0 is not a court booking"
  }
}
