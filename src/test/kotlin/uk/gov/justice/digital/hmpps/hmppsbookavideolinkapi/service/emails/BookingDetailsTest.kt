package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.CvpLinkDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactly
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.locationAttributes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withPreMainPostCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AdditionalBookingDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.BookingStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.time.LocalDate
import java.time.LocalTime

class BookingDetailsTest {
  private val prisoner = prisoner(
    prisonerNumber = "123456",
    prisonCode = BIRMINGHAM,
    firstName = "Fred",
    lastName = "Bloggs",
  )

  @Nested
  inner class CourtBookingDetails {
    private val courtBooking =
      courtBooking(notesForStaff = "Court hearing staff notes", cvpLinkDetails = CvpLinkDetails.hmctsNumber("54321"))
        .withPreMainPostCourtPrisonAppointment(
          prisonCode = prisoner.prisonCode,
          prisonerNumber = prisoner.prisonerNumber,
          location = wandsworthLocation,
          date = LocalDate.of(2100, 1, 1),
          startTime = LocalTime.of(11, 0),
          endTime = LocalTime.of(11, 30),
        ).toModel(locations = setOf(wandsworthLocation.toModel()))
        .copy(videoLinkBookingId = 12345L, videoLinkUrl = "https://www.google.com")

    @Test
    fun `should initialise booking details for create`() {
      with(BookingDetails.create(courtBooking, prisoner)) {
        bookingType isEqualTo BookingType.COURT
        videoLinkBookingId isEqualTo 12345L
        courtDescription isEqualTo "DRBYMC"
        preHearing isEqualTo courtBooking.prisonAppointments.single { it.appointmentType == "VLB_COURT_PRE" }
        mainHearing isEqualTo courtBooking.prisonAppointments.single { it.appointmentType == "VLB_COURT_MAIN" }
        postHearing isEqualTo courtBooking.prisonAppointments.single { it.appointmentType == "VLB_COURT_POST" }
        hmctsNumber isEqualTo "54321"
        notesForStaff isEqualTo "Court hearing staff notes"
        videoLinkUrl isEqualTo "https://www.google.com"
        prisonName isEqualTo "Birmingham"
        prisonerNumber isEqualTo "123456"
        prisonerFirstName isEqualTo "Fred"
        prisonerLastName isEqualTo "Bloggs"
        prisonerDateOfBirth isEqualTo LocalDate.of(2000, 1, 1)
      }
    }

    @Test
    fun `should initialise booking details for amend`() {
      with(BookingDetails.amended(courtBooking, courtBooking, prisoner)) {
        bookingType isEqualTo BookingType.COURT
        videoLinkBookingId isEqualTo 12345L
        courtDescription isEqualTo "DRBYMC"
        preHearing isEqualTo courtBooking.prisonAppointments.single { it.appointmentType == "VLB_COURT_PRE" }
        mainHearing isEqualTo courtBooking.prisonAppointments.single { it.appointmentType == "VLB_COURT_MAIN" }
        postHearing isEqualTo courtBooking.prisonAppointments.single { it.appointmentType == "VLB_COURT_POST" }
        hmctsNumber isEqualTo "54321"
        notesForStaff isEqualTo "Court hearing staff notes"
        videoLinkUrl isEqualTo "https://www.google.com"
        prisonName isEqualTo "Birmingham"
        prisonerNumber isEqualTo "123456"
        prisonerFirstName isEqualTo "Fred"
        prisonerLastName isEqualTo "Bloggs"
        prisonerDateOfBirth isEqualTo LocalDate.of(2000, 1, 1)
        oldPreHearing isEqualTo courtBooking.prisonAppointments.single { it.appointmentType == "VLB_COURT_PRE" }
        oldMainHearing isEqualTo courtBooking.prisonAppointments.single { it.appointmentType == "VLB_COURT_MAIN" }
        oldPostHearing isEqualTo courtBooking.prisonAppointments.single { it.appointmentType == "VLB_COURT_POST" }
      }
    }

    @Test
    fun `should initialise booking details for cancelled`() {
      with(BookingDetails.cancelled(courtBooking.copy(statusCode = BookingStatus.CANCELLED), prisoner)) {
        bookingType isEqualTo BookingType.COURT
        videoLinkBookingId isEqualTo 12345L
        courtDescription isEqualTo "DRBYMC"
        preHearing isEqualTo courtBooking.prisonAppointments.single { it.appointmentType == "VLB_COURT_PRE" }
        mainHearing isEqualTo courtBooking.prisonAppointments.single { it.appointmentType == "VLB_COURT_MAIN" }
        postHearing isEqualTo courtBooking.prisonAppointments.single { it.appointmentType == "VLB_COURT_POST" }
        hmctsNumber isEqualTo "54321"
        notesForStaff isEqualTo "Court hearing staff notes"
        videoLinkUrl isEqualTo "https://www.google.com"
        prisonName isEqualTo "Birmingham"
        prisonerNumber isEqualTo "123456"
        prisonerFirstName isEqualTo "Fred"
        prisonerLastName isEqualTo "Bloggs"
        prisonerDateOfBirth isEqualTo LocalDate.of(2000, 1, 1)
      }
    }

    @Test
    fun `should initialise booking details for released`() {
      with(BookingDetails.released(courtBooking.copy(statusCode = BookingStatus.CANCELLED), prisoner)) {
        bookingType isEqualTo BookingType.COURT
        videoLinkBookingId isEqualTo 12345L
        courtDescription isEqualTo "DRBYMC"
        preHearing isEqualTo courtBooking.prisonAppointments.single { it.appointmentType == "VLB_COURT_PRE" }
        mainHearing isEqualTo courtBooking.prisonAppointments.single { it.appointmentType == "VLB_COURT_MAIN" }
        postHearing isEqualTo courtBooking.prisonAppointments.single { it.appointmentType == "VLB_COURT_POST" }
        hmctsNumber isEqualTo "54321"
        notesForStaff isEqualTo "Court hearing staff notes"
        videoLinkUrl isEqualTo "https://www.google.com"
        prisonName isEqualTo "Birmingham"
        prisonerNumber isEqualTo "123456"
        prisonerFirstName isEqualTo "Fred"
        prisonerLastName isEqualTo "Bloggs"
        prisonerDateOfBirth isEqualTo LocalDate.of(2000, 1, 1)
      }
    }

    @Test
    fun `should initialise booking details for transferred`() {
      with(BookingDetails.transferred(courtBooking.copy(statusCode = BookingStatus.CANCELLED), prisoner)) {
        bookingType isEqualTo BookingType.COURT
        videoLinkBookingId isEqualTo 12345L
        courtDescription isEqualTo "DRBYMC"
        preHearing isEqualTo courtBooking.prisonAppointments.single { it.appointmentType == "VLB_COURT_PRE" }
        mainHearing isEqualTo courtBooking.prisonAppointments.single { it.appointmentType == "VLB_COURT_MAIN" }
        postHearing isEqualTo courtBooking.prisonAppointments.single { it.appointmentType == "VLB_COURT_POST" }
        hmctsNumber isEqualTo "54321"
        notesForStaff isEqualTo "Court hearing staff notes"
        videoLinkUrl isEqualTo "https://www.google.com"
        prisonName isEqualTo "Birmingham"
        prisonerNumber isEqualTo "123456"
        prisonerFirstName isEqualTo "Fred"
        prisonerLastName isEqualTo "Bloggs"
        prisonerDateOfBirth isEqualTo LocalDate.of(2000, 1, 1)
      }
    }
  }

  @Nested
  inner class ProbationBookingDetails {
    private val probationBooking =
      probationBooking(notesForStaff = "Probation meeting staff notes").withProbationPrisonAppointment(
        prisonCode = prisoner.prisonCode,
        prisonerNumber = prisoner.prisonerNumber,
        date = LocalDate.of(2100, 1, 1),
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(11, 30),
        location = wandsworthLocation,
      )
        .toModel(locations = setOf(wandsworthLocation.toModel(locationAttributes().copy(prisonVideoUrl = "decorated-video-link-url"))))
        .copy(videoLinkBookingId = 54321L)

    @Test
    fun `should initialise booking details for create`() {
      with(BookingDetails.create(probationBooking, prisoner)) {
        bookingType isEqualTo BookingType.PROBATION
        videoLinkBookingId isEqualTo 54321L
        probationTeamDescription isEqualTo "probation team description"
        probationMeetingType isEqualTo "Pre-sentence report (PSR)"
        mainMeeting isEqualTo probationBooking.prisonAppointments.single { it.appointmentType == "VLB_PROBATION" }
        notesForStaff isEqualTo "Probation meeting staff notes"
        videoLinkUrl isEqualTo "decorated-video-link-url"
        prisonName isEqualTo "Birmingham"
        prisonerNumber isEqualTo "123456"
        prisonerFirstName isEqualTo "Fred"
        prisonerLastName isEqualTo "Bloggs"
        prisonerDateOfBirth isEqualTo LocalDate.of(2000, 1, 1)
      }
    }

    @Test
    fun `should initialise booking details for amend`() {
      with(BookingDetails.amended(probationBooking, probationBooking, prisoner)) {
        bookingType isEqualTo BookingType.PROBATION
        videoLinkBookingId isEqualTo 54321L
        probationTeamDescription isEqualTo "probation team description"
        probationMeetingType isEqualTo "Pre-sentence report (PSR)"
        mainMeeting isEqualTo probationBooking.prisonAppointments.single { it.appointmentType == "VLB_PROBATION" }
        notesForStaff isEqualTo "Probation meeting staff notes"
        videoLinkUrl isEqualTo "decorated-video-link-url"
        prisonName isEqualTo "Birmingham"
        prisonerNumber isEqualTo "123456"
        prisonerFirstName isEqualTo "Fred"
        prisonerLastName isEqualTo "Bloggs"
        prisonerDateOfBirth isEqualTo LocalDate.of(2000, 1, 1)
        oldMainMeeting isEqualTo probationBooking.prisonAppointments.single { it.appointmentType == "VLB_PROBATION" }
      }
    }

    @Test
    fun `should initialise booking details for cancelled`() {
      with(BookingDetails.cancelled(probationBooking.copy(statusCode = BookingStatus.CANCELLED), prisoner)) {
        bookingType isEqualTo BookingType.PROBATION
        videoLinkBookingId isEqualTo 54321L
        probationTeamDescription isEqualTo "probation team description"
        probationMeetingType isEqualTo "Pre-sentence report (PSR)"
        mainMeeting isEqualTo probationBooking.prisonAppointments.single { it.appointmentType == "VLB_PROBATION" }
        notesForStaff isEqualTo "Probation meeting staff notes"
        videoLinkUrl isEqualTo "decorated-video-link-url"
        prisonName isEqualTo "Birmingham"
        prisonerNumber isEqualTo "123456"
        prisonerFirstName isEqualTo "Fred"
        prisonerLastName isEqualTo "Bloggs"
        prisonerDateOfBirth isEqualTo LocalDate.of(2000, 1, 1)
      }
    }

    @Test
    fun `should initialise booking details for released`() {
      with(BookingDetails.released(probationBooking.copy(statusCode = BookingStatus.CANCELLED), prisoner)) {
        bookingType isEqualTo BookingType.PROBATION
        videoLinkBookingId isEqualTo 54321L
        probationTeamDescription isEqualTo "probation team description"
        probationMeetingType isEqualTo "Pre-sentence report (PSR)"
        mainMeeting isEqualTo probationBooking.prisonAppointments.single { it.appointmentType == "VLB_PROBATION" }
        notesForStaff isEqualTo "Probation meeting staff notes"
        videoLinkUrl isEqualTo "decorated-video-link-url"
        prisonName isEqualTo "Birmingham"
        prisonerNumber isEqualTo "123456"
        prisonerFirstName isEqualTo "Fred"
        prisonerLastName isEqualTo "Bloggs"
        prisonerDateOfBirth isEqualTo LocalDate.of(2000, 1, 1)
      }
    }

    @Test
    fun `should initialise booking details for transferred`() {
      with(BookingDetails.transferred(probationBooking.copy(statusCode = BookingStatus.CANCELLED), prisoner)) {
        bookingType isEqualTo BookingType.PROBATION
        videoLinkBookingId isEqualTo 54321L
        probationTeamDescription isEqualTo "probation team description"
        probationMeetingType isEqualTo "Pre-sentence report (PSR)"
        mainMeeting isEqualTo probationBooking.prisonAppointments.single { it.appointmentType == "VLB_PROBATION" }
        notesForStaff isEqualTo "Probation meeting staff notes"
        videoLinkUrl isEqualTo "decorated-video-link-url"
        prisonName isEqualTo "Birmingham"
        prisonerNumber isEqualTo "123456"
        prisonerFirstName isEqualTo "Fred"
        prisonerLastName isEqualTo "Bloggs"
        prisonerDateOfBirth isEqualTo LocalDate.of(2000, 1, 1)
      }
    }

    @DisplayName("BookingDetails.Amended::getAllAdditionalBookingDetails")
    @Nested
    inner class GetAllAdditionalBookingDetails {
      val officerOne = AdditionalBookingDetails(
        contactName = "Officer One",
        contactEmail = "one@example.com",
        contactNumber = null,
      )

      val officerTwo = AdditionalBookingDetails(
        contactName = "Officer Two",
        contactEmail = "two@example.com",
        contactNumber = null,
      )

      val probationBookingOfficerOne = probationBooking.copy(
        additionalBookingDetails = officerOne,
      )

      val probationBookingOfficerTwo = probationBooking.copy(
        additionalBookingDetails = officerTwo,
      )

      @Test
      fun `should return no probation officers if not set`() {
        with(BookingDetails.amended(probationBooking, probationBooking, prisoner)) {
          getAllAdditionalBookingDetails() hasSize 0
        }
      }

      @Test
      fun `should return probation officer if unchanged`() {
        with(BookingDetails.amended(probationBookingOfficerOne, probationBookingOfficerOne, prisoner)) {
          getAllAdditionalBookingDetails() hasSize 1
          getAllAdditionalBookingDetails() containsExactly listOf(officerOne)
        }
      }

      @Test
      fun `should return both probation officers if changed`() {
        with(BookingDetails.amended(probationBookingOfficerOne, probationBookingOfficerTwo, prisoner)) {
          getAllAdditionalBookingDetails() hasSize 2
          getAllAdditionalBookingDetails() containsExactlyInAnyOrder listOf(officerOne, officerTwo)
        }
      }

      @Test
      fun `should return probation officer if added`() {
        with(BookingDetails.amended(probationBooking, probationBookingOfficerTwo, prisoner)) {
          getAllAdditionalBookingDetails() hasSize 1
          getAllAdditionalBookingDetails() containsExactly listOf(officerTwo)
        }
      }

      @Test
      fun `should return probation officer if removed`() {
        with(BookingDetails.amended(probationBookingOfficerOne, probationBooking, prisoner)) {
          getAllAdditionalBookingDetails() hasSize 1
          getAllAdditionalBookingDetails() containsExactly listOf(officerOne)
        }
      }
    }
  }
}
