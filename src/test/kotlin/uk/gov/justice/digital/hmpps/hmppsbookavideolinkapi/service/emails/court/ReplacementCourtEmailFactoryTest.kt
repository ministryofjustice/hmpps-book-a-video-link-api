package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.CvpLinkDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.bookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isInstanceOf
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.BookingDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.time.LocalDate
import java.time.LocalTime

class ReplacementCourtEmailFactoryTest {
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

  private val courtBookingEntity = courtBooking
  private val courtBookingDto = courtBookingEntity.toModel(locations = setOf(wandsworthLocation.toModel()))

  @Nested
  inner class UserEmails {
    private val userContact = bookingContact(contactType = ContactType.USER, email = "user@email.com", name = "Fred")

    @Test
    fun `should return new court booking user email`() {
      val email = ReplacementCourtEmailFactory.user(
        BookingDetails.create(
          newBooking = courtBookingDto,
          prisoner = prisoner,
        ),
        userContact = userContact,
      )

      email isInstanceOf NewCourtBookingUserEmail::class.java
    }

    @Test
    fun `should return rescheduled court booking user email`() {
      val email = ReplacementCourtEmailFactory.user(
        BookingDetails.amended(
          oldBooking = courtBookingDto,
          prisoner = prisoner,
          amendedBooking = courtBookingDto.copy(prisonAppointments = listOf(courtBookingDto.prisonAppointments.single().copy(appointmentDate = LocalDate.of(2099, 1, 1)))),
        ),
        userContact = userContact,
      )

      email isInstanceOf RescheduledCourtBookingUserEmail::class.java
    }

    @Test
    fun `should return amended court booking user email`() {
      val email = ReplacementCourtEmailFactory.user(
        BookingDetails.amended(
          oldBooking = courtBookingDto,
          prisoner = prisoner,
          amendedBooking = courtBookingDto,
        ),
        userContact = userContact,
      )

      email isInstanceOf AmendedCourtBookingUserEmail::class.java
    }

    @Test
    fun `should return cancelled court booking user email`() {
      val email = ReplacementCourtEmailFactory.user(
        BookingDetails.cancelled(
          cancelledBooking = courtBookingEntity.cancel(COURT_USER).toModel(locations = setOf(wandsworthLocation.toModel())),
          prisoner = prisoner,
        ),
        userContact = userContact,
      )

      email isInstanceOf CancelledCourtBookingUserEmail::class.java
    }
  }

  @Nested
  inner class CourtEmails {
    private val courtContact = bookingContact(contactType = ContactType.COURT, email = "court@email.com", name = "Fred")

    @Test
    fun `should return new court booking court email`() {
      val email = ReplacementCourtEmailFactory.court(
        BookingDetails.create(
          newBooking = courtBookingDto,
          prisoner = prisoner,
        ),
        courtContact = courtContact,
      )

      email isInstanceOf NewCourtBookingCourtEmail::class.java
    }

    @Test
    fun `should return rescheduled court booking court email`() {
      val email = ReplacementCourtEmailFactory.court(
        BookingDetails.amended(
          oldBooking = courtBookingDto,
          prisoner = prisoner,
          amendedBooking = courtBookingDto.copy(prisonAppointments = listOf(courtBookingDto.prisonAppointments.single().copy(appointmentDate = LocalDate.of(2099, 1, 1)))),
        ),
        courtContact = courtContact,
      )

      email isInstanceOf RescheduledCourtBookingCourtEmail::class.java
    }

    @Test
    fun `should return amended court booking court email`() {
      val email = ReplacementCourtEmailFactory.court(
        BookingDetails.amended(
          oldBooking = courtBookingDto,
          prisoner = prisoner,
          amendedBooking = courtBookingDto,
        ),
        courtContact = courtContact,
      )

      email isInstanceOf AmendedCourtBookingCourtEmail::class.java
    }

    @Test
    fun `should return cancelled court booking court email`() {
      val email = ReplacementCourtEmailFactory.court(
        BookingDetails.cancelled(
          cancelledBooking = courtBookingEntity.cancel(COURT_USER).toModel(locations = setOf(wandsworthLocation.toModel())),
          prisoner = prisoner,
        ),
        courtContact = courtContact,
      )

      email isInstanceOf CancelledCourtBookingCourtEmail::class.java
    }

    @Test
    fun `should return released court booking court email`() {
      val email = ReplacementCourtEmailFactory.court(
        BookingDetails.released(
          cancelledBooking = courtBookingEntity.cancel(COURT_USER).toModel(locations = setOf(wandsworthLocation.toModel())),
          prisoner = prisoner,
        ),
        courtContact = courtContact,
      )

      email isInstanceOf ReleasedCourtBookingCourtEmail::class.java
    }

    @Test
    fun `should return transferred court booking court email`() {
      val email = ReplacementCourtEmailFactory.court(
        BookingDetails.transferred(
          cancelledBooking = courtBookingEntity.cancel(COURT_USER).toModel(locations = setOf(wandsworthLocation.toModel())),
          prisoner = prisoner,
        ),
        courtContact = courtContact,
      )

      email isInstanceOf TransferredCourtBookingCourtEmail::class.java
    }

    @Test
    fun `should return reminder court booking court email`() {
      val email = ReplacementCourtEmailFactory.court(
        BookingDetails.courtHearingLinkReminder(
          booking = courtBookingDto,
          prisoner = prisoner,
        ),
        courtContact = courtContact,
      )

      email isInstanceOf CourtHearingLinkReminderEmail::class.java
    }
  }

  @Nested
  inner class PrisonEmails {
    private val prisonContact = bookingContact(contactType = ContactType.PRISON, email = "prison@email.com", name = "Fred")
    private val primaryCourtContact = bookingContact(contactType = ContactType.COURT, email = "court@email.com", name = "Fred")

    @Test
    fun `should return new court booking prison email - no primary contact`() {
      val email = ReplacementCourtEmailFactory.prison(
        BookingDetails.create(
          newBooking = courtBookingDto,
          prisoner = prisoner,
        ),
        prisonContact = prisonContact,
        contacts = emptySet(),
      )

      email isInstanceOf NewCourtBookingPrisonNoCourtEmail::class.java
    }

    @Test
    fun `should return new court booking prison email - primary contact`() {
      val email = ReplacementCourtEmailFactory.prison(
        BookingDetails.create(
          newBooking = courtBookingDto,
          prisoner = prisoner,
        ),
        prisonContact = prisonContact,
        contacts = setOf(primaryCourtContact),
      )

      email isInstanceOf NewCourtBookingPrisonCourtEmail::class.java
    }

    @Test
    fun `should return rescheduled court booking prison email - no primary contact`() {
      val email = ReplacementCourtEmailFactory.prison(
        BookingDetails.amended(
          oldBooking = courtBookingDto,
          prisoner = prisoner,
          amendedBooking = courtBookingDto.copy(prisonAppointments = listOf(courtBookingDto.prisonAppointments.single().copy(appointmentDate = LocalDate.of(2099, 1, 1)))),
        ),
        prisonContact = prisonContact,
        contacts = emptySet(),
      )

      email isInstanceOf RescheduledCourtBookingPrisonNoCourtEmail::class.java
    }

    @Test
    fun `should return rescheduled court booking prison email - primary contact`() {
      val email = ReplacementCourtEmailFactory.prison(
        BookingDetails.amended(
          oldBooking = courtBookingDto,
          prisoner = prisoner,
          amendedBooking = courtBookingDto.copy(prisonAppointments = listOf(courtBookingDto.prisonAppointments.single().copy(appointmentDate = LocalDate.of(2099, 1, 1)))),
        ),
        prisonContact = prisonContact,
        contacts = setOf(primaryCourtContact),
      )

      email isInstanceOf RescheduledCourtBookingPrisonCourtEmail::class.java
    }

    @Test
    fun `should return amended court booking prison email - no primary contact`() {
      val email = ReplacementCourtEmailFactory.prison(
        BookingDetails.amended(
          oldBooking = courtBookingDto,
          prisoner = prisoner,
          amendedBooking = courtBookingDto,
        ),
        prisonContact = prisonContact,
        contacts = emptySet(),
      )

      email isInstanceOf AmendedCourtBookingPrisonNoCourtEmail::class.java
    }

    @Test
    fun `should return amended court booking prison email - primary contact`() {
      val email = ReplacementCourtEmailFactory.prison(
        BookingDetails.amended(
          oldBooking = courtBookingDto,
          prisoner = prisoner,
          amendedBooking = courtBookingDto,
        ),
        prisonContact = prisonContact,
        contacts = setOf(primaryCourtContact),
      )

      email isInstanceOf AmendedCourtBookingPrisonCourtEmail::class.java
    }

    @Test
    fun `should return cancelled court booking prison email - no primary contact`() {
      val email = ReplacementCourtEmailFactory.prison(
        BookingDetails.cancelled(
          cancelledBooking = courtBookingEntity.cancel(COURT_USER).toModel(locations = setOf(wandsworthLocation.toModel())),
          prisoner = prisoner,
        ),
        prisonContact = prisonContact,
        contacts = emptySet(),
      )

      email isInstanceOf CancelledCourtBookingPrisonNoCourtEmail::class.java
    }

    @Test
    fun `should return cancelled court booking prison email - primary contact`() {
      val email = ReplacementCourtEmailFactory.prison(
        BookingDetails.cancelled(
          cancelledBooking = courtBookingEntity.cancel(COURT_USER).toModel(locations = setOf(wandsworthLocation.toModel())),
          prisoner = prisoner,
        ),
        prisonContact = prisonContact,
        contacts = setOf(primaryCourtContact),
      )

      email isInstanceOf CancelledCourtBookingPrisonCourtEmail::class.java
    }

    @Test
    fun `should return released court booking prison email - no primary contact`() {
      val email = ReplacementCourtEmailFactory.prison(
        BookingDetails.released(
          cancelledBooking = courtBookingEntity.cancel(COURT_USER).toModel(locations = setOf(wandsworthLocation.toModel())),
          prisoner = prisoner,
        ),
        prisonContact = prisonContact,
        contacts = emptySet(),
      )

      email isInstanceOf ReleasedCourtBookingPrisonNoCourtEmail::class.java
    }

    @Test
    fun `should return released court booking prison email - primary contact`() {
      val email = ReplacementCourtEmailFactory.prison(
        BookingDetails.released(
          cancelledBooking = courtBookingEntity.cancel(COURT_USER).toModel(locations = setOf(wandsworthLocation.toModel())),
          prisoner = prisoner,
        ),
        prisonContact = prisonContact,
        contacts = setOf(primaryCourtContact),
      )

      email isInstanceOf ReleasedCourtBookingPrisonCourtEmail::class.java
    }

    @Test
    fun `should return transferred court booking prison email - no primary contact`() {
      val email = ReplacementCourtEmailFactory.prison(
        BookingDetails.transferred(
          cancelledBooking = courtBookingEntity.cancel(COURT_USER).toModel(locations = setOf(wandsworthLocation.toModel())),
          prisoner = prisoner,
        ),
        prisonContact = prisonContact,
        contacts = emptySet(),
      )

      email isInstanceOf TransferredCourtBookingPrisonNoCourtEmail::class.java
    }

    @Test
    fun `should return transferred court booking prison email - primary contact`() {
      val email = ReplacementCourtEmailFactory.prison(
        BookingDetails.transferred(
          cancelledBooking = courtBookingEntity.cancel(COURT_USER).toModel(locations = setOf(wandsworthLocation.toModel())),
          prisoner = prisoner,
        ),
        prisonContact = prisonContact,
        contacts = setOf(primaryCourtContact),
      )

      email isInstanceOf TransferredCourtBookingPrisonCourtEmail::class.java
    }
  }
}
