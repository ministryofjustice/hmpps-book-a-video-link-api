package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.bookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isInstanceOf
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.BookingDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.time.LocalDate
import java.time.LocalTime

class ReplacementProbationEmailFactoryTest {
  private val prisoner = prisoner(
    prisonerNumber = "123456",
    prisonCode = BIRMINGHAM,
    firstName = "Fred",
    lastName = "Bloggs",
  )

  private val probationBooking = probationBooking(notesForStaff = "probation meeting staff notes")
    .addAppointment(
      prison = prison(prisonCode = BIRMINGHAM),
      prisonerNumber = "123456",
      appointmentType = "VLB_PROBATION",
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      locationId = wandsworthLocation.id,
    )

  private val probationBookingEntity = probationBooking
  private val probationBookingDto = probationBookingEntity.toModel(locations = setOf(wandsworthLocation.toModel()))

  @Nested
  inner class UserEmails {
    private val userContact = bookingContact(contactType = ContactType.USER, email = "user@email.com", name = "Fred")

    @Test
    fun `should return new probation booking user email`() {
      val email = ReplacementProbationEmailFactory.user(
        BookingDetails.create(
          newBooking = probationBookingDto,
          prisoner = prisoner,
        ),
        userContact = userContact,
      )

      email isInstanceOf NewProbationBookingUserEmail::class.java
    }

    @Test
    fun `should return rescheduled probation booking user email`() {
      val email = ReplacementProbationEmailFactory.user(
        BookingDetails.amended(
          oldBooking = probationBookingDto,
          prisoner = prisoner,
          amendedBooking = probationBookingDto.copy(prisonAppointments = listOf(probationBookingDto.prisonAppointments.single().copy(appointmentDate = LocalDate.of(2099, 1, 1)))),
        ),
        userContact = userContact,
      )

      email isInstanceOf RescheduledProbationBookingUserEmail::class.java
    }

    @Test
    fun `should return amended probation booking user email`() {
      val email = ReplacementProbationEmailFactory.user(
        BookingDetails.amended(
          oldBooking = probationBookingDto,
          prisoner = prisoner,
          amendedBooking = probationBookingDto,
        ),
        userContact = userContact,
      )

      email isInstanceOf AmendedProbationBookingUserEmail::class.java
    }

    @Test
    fun `should return cancelled probation booking user email`() {
      val email = ReplacementProbationEmailFactory.user(
        BookingDetails.cancelled(
          cancelledBooking = probationBookingEntity.cancel(PROBATION_USER).toModel(locations = setOf(wandsworthLocation.toModel())),
          prisoner = prisoner,
        ),
        userContact = userContact,
      )

      email isInstanceOf CancelledProbationBookingUserEmail::class.java
    }
  }

  @Nested
  inner class ProbationEmails {
    private val probationContact = bookingContact(contactType = ContactType.PROBATION, email = "probation@email.com", name = "Fred")

    @Test
    fun `should return new probation booking probation email`() {
      val email = ReplacementProbationEmailFactory.probation(
        BookingDetails.create(
          newBooking = probationBookingDto,
          prisoner = prisoner,
        ),
        probationContact = probationContact,
      )

      email isInstanceOf NewProbationBookingProbationEmail::class.java
    }

    @Test
    fun `should return rescheduled probation booking probation email`() {
      val email = ReplacementProbationEmailFactory.probation(
        BookingDetails.amended(
          oldBooking = probationBookingDto,
          prisoner = prisoner,
          amendedBooking = probationBookingDto.copy(prisonAppointments = listOf(probationBookingDto.prisonAppointments.single().copy(appointmentDate = LocalDate.of(2099, 1, 1)))),
        ),
        probationContact = probationContact,
      )

      email isInstanceOf RescheduledProbationBookingProbationEmail::class.java
    }

    @Test
    fun `should return amended probation booking probation email`() {
      val email = ReplacementProbationEmailFactory.probation(
        BookingDetails.amended(
          oldBooking = probationBookingDto,
          prisoner = prisoner,
          amendedBooking = probationBookingDto,
        ),
        probationContact = probationContact,
      )

      email isInstanceOf AmendedProbationBookingProbationEmail::class.java
    }

    @Test
    fun `should return cancelled probation booking probation email`() {
      val email = ReplacementProbationEmailFactory.probation(
        BookingDetails.cancelled(
          cancelledBooking = probationBookingEntity.cancel(PROBATION_USER).toModel(locations = setOf(wandsworthLocation.toModel())),
          prisoner = prisoner,
        ),
        probationContact = probationContact,
      )

      email isInstanceOf CancelledProbationBookingProbationEmail::class.java
    }

    @Test
    fun `should return released probation booking probation email`() {
      val email = ReplacementProbationEmailFactory.probation(
        BookingDetails.released(
          cancelledBooking = probationBookingEntity.cancel(PROBATION_USER).toModel(locations = setOf(wandsworthLocation.toModel())),
          prisoner = prisoner,
        ),
        probationContact = probationContact,
      )

      email isInstanceOf ReleasedProbationBookingProbationEmail::class.java
    }

    @Test
    fun `should return transferred probation booking probation email`() {
      val email = ReplacementProbationEmailFactory.probation(
        BookingDetails.transferred(
          cancelledBooking = probationBookingEntity.cancel(PROBATION_USER).toModel(locations = setOf(wandsworthLocation.toModel())),
          prisoner = prisoner,
        ),
        probationContact = probationContact,
      )

      email isInstanceOf TransferredProbationBookingProbationEmail::class.java
    }

    @Test
    fun `should return reminder probation booking probation email`() {
      val email = ReplacementProbationEmailFactory.probation(
        BookingDetails.probationOfficerEmailReminder(
          booking = probationBookingDto,
          prisoner = prisoner,
        ),
        probationContact = probationContact,
      )

      email isInstanceOf ProbationOfficerDetailsReminderEmail::class.java
    }
  }

  @Nested
  inner class PrisonEmails {
    private val prisonContact = bookingContact(contactType = ContactType.PRISON, email = "prison@email.com", name = "Fred")
    private val primaryProbationContact = bookingContact(contactType = ContactType.PROBATION, email = "probation@email.com", name = "Fred")

    @Test
    fun `should return new probation booking prison email - no primary contact`() {
      val email = ReplacementProbationEmailFactory.prison(
        BookingDetails.create(
          newBooking = probationBookingDto,
          prisoner = prisoner,
        ),
        prisonContact = prisonContact,
        probationContacts = emptySet(),
      )

      email isInstanceOf NewProbationBookingPrisonNoProbationEmail::class.java
    }

    @Test
    fun `should return new probation booking prison email - primary contact`() {
      val email = ReplacementProbationEmailFactory.prison(
        BookingDetails.create(
          newBooking = probationBookingDto,
          prisoner = prisoner,
        ),
        prisonContact = prisonContact,
        probationContacts = setOf(primaryProbationContact),
      )

      email isInstanceOf NewProbationBookingPrisonProbationEmail::class.java
    }

    @Test
    fun `should return rescheduled probation booking prison email - no primary contact`() {
      val email = ReplacementProbationEmailFactory.prison(
        BookingDetails.amended(
          oldBooking = probationBookingDto,
          prisoner = prisoner,
          amendedBooking = probationBookingDto.copy(prisonAppointments = listOf(probationBookingDto.prisonAppointments.single().copy(appointmentDate = LocalDate.of(2099, 1, 1)))),
        ),
        prisonContact = prisonContact,
        probationContacts = emptySet(),
      )

      email isInstanceOf RescheduledProbationBookingPrisonNoProbationEmail::class.java
    }

    @Test
    fun `should return rescheduled probation booking prison email - primary contact`() {
      val email = ReplacementProbationEmailFactory.prison(
        BookingDetails.amended(
          oldBooking = probationBookingDto,
          prisoner = prisoner,
          amendedBooking = probationBookingDto.copy(prisonAppointments = listOf(probationBookingDto.prisonAppointments.single().copy(appointmentDate = LocalDate.of(2099, 1, 1)))),
        ),
        prisonContact = prisonContact,
        probationContacts = setOf(primaryProbationContact),
      )

      email isInstanceOf RescheduledProbationBookingPrisonProbationEmail::class.java
    }

    @Test
    fun `should return amended probation booking prison email - no primary contact`() {
      val email = ReplacementProbationEmailFactory.prison(
        BookingDetails.amended(
          oldBooking = probationBookingDto,
          prisoner = prisoner,
          amendedBooking = probationBookingDto,
        ),
        prisonContact = prisonContact,
        probationContacts = emptySet(),
      )

      email isInstanceOf AmendedProbationBookingPrisonNoProbationEmail::class.java
    }

    @Test
    fun `should return amended probation booking prison email - primary contact`() {
      val email = ReplacementProbationEmailFactory.prison(
        BookingDetails.amended(
          oldBooking = probationBookingDto,
          prisoner = prisoner,
          amendedBooking = probationBookingDto,
        ),
        prisonContact = prisonContact,
        probationContacts = setOf(primaryProbationContact),
      )

      email isInstanceOf AmendedProbationBookingPrisonProbationEmail::class.java
    }

    @Test
    fun `should return cancelled probation booking prison email - no primary contact`() {
      val email = ReplacementProbationEmailFactory.prison(
        BookingDetails.cancelled(
          cancelledBooking = probationBookingEntity.cancel(PROBATION_USER).toModel(locations = setOf(wandsworthLocation.toModel())),
          prisoner = prisoner,
        ),
        prisonContact = prisonContact,
        probationContacts = emptySet(),
      )

      email isInstanceOf CancelledProbationBookingPrisonNoProbationEmail::class.java
    }

    @Test
    fun `should return cancelled probation booking prison email - primary contact`() {
      val email = ReplacementProbationEmailFactory.prison(
        BookingDetails.cancelled(
          cancelledBooking = probationBookingEntity.cancel(PROBATION_USER).toModel(locations = setOf(wandsworthLocation.toModel())),
          prisoner = prisoner,
        ),
        prisonContact = prisonContact,
        probationContacts = setOf(primaryProbationContact),
      )

      email isInstanceOf CancelledProbationBookingPrisonProbationEmail::class.java
    }

    @Test
    fun `should return released probation booking prison email - no primary contact`() {
      val email = ReplacementProbationEmailFactory.prison(
        BookingDetails.released(
          cancelledBooking = probationBookingEntity.cancel(PROBATION_USER).toModel(locations = setOf(wandsworthLocation.toModel())),
          prisoner = prisoner,
        ),
        prisonContact = prisonContact,
        probationContacts = emptySet(),
      )

      email isInstanceOf ReleasedProbationBookingPrisonNoProbationEmail::class.java
    }

    @Test
    fun `should return released probation booking prison email - primary contact`() {
      val email = ReplacementProbationEmailFactory.prison(
        BookingDetails.released(
          cancelledBooking = probationBookingEntity.cancel(PROBATION_USER).toModel(locations = setOf(wandsworthLocation.toModel())),
          prisoner = prisoner,
        ),
        prisonContact = prisonContact,
        probationContacts = setOf(primaryProbationContact),
      )

      email isInstanceOf ReleasedProbationBookingPrisonProbationEmail::class.java
    }

    @Test
    fun `should return transferred probation booking prison email - no primary contact`() {
      val email = ReplacementProbationEmailFactory.prison(
        BookingDetails.transferred(
          cancelledBooking = probationBookingEntity.cancel(PROBATION_USER).toModel(locations = setOf(wandsworthLocation.toModel())),
          prisoner = prisoner,
        ),
        prisonContact = prisonContact,
        probationContacts = emptySet(),
      )

      email isInstanceOf TransferredProbationBookingPrisonNoProbationEmail::class.java
    }

    @Test
    fun `should return transferred probation booking prison email - primary contact`() {
      val email = ReplacementProbationEmailFactory.prison(
        BookingDetails.transferred(
          cancelledBooking = probationBookingEntity.cancel(PROBATION_USER).toModel(locations = setOf(wandsworthLocation.toModel())),
          prisoner = prisoner,
        ),
        prisonContact = prisonContact,
        probationContacts = setOf(primaryProbationContact),
      )

      email isInstanceOf TransferredProbationBookingPrisonProbationEmail::class.java
    }
  }
}
