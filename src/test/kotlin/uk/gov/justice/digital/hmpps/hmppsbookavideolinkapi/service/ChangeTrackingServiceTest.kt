package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.CvpLinkDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendCourtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendProbationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isNotEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.AdditionalBookingDetailRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class ChangeTrackingServiceTest {
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val locationsService: LocationsService = mock()
  private val additionalBookingDetailRepository: AdditionalBookingDetailRepository = mock()
  private val service = ChangeTrackingService(videoBookingRepository, locationsService, additionalBookingDetailRepository)

  @Nested
  inner class HasCourtBookingChanged {
    private val existingCourtBooking = courtBooking(
      hearingType = CourtHearingType.TRIBUNAL,
      cvpLinkDetails = CvpLinkDetails.url("https://video.link.com"),
      notesForStaff = "staff notes",
      notesForPrisoners = "prisoner notes",
    ).withMainCourtPrisonAppointment(
      prisonCode = WANDSWORTH,
      location = wandsworthLocation,
      date = today(),
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
    )

    private val amendCourtBookingRequest = amendCourtBookingRequest(
      prisonCode = WANDSWORTH,
      location = wandsworthLocation,
      appointmentDate = today(),
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      hearingType = CourtHearingType.TRIBUNAL,
      cvpLinkDetails = CvpLinkDetails.url("https://video.link.com"),
      notesForStaff = "staff notes",
    )

    @Test
    fun `should be no changes as court user`() {
      whenever(videoBookingRepository.findById(1L)) doReturn Optional.of(existingCourtBooking)
      whenever(locationsService.getLocationByKey(amendCourtBookingRequest.prisoners.single().appointments.single().locationKey!!)) doReturn wandsworthLocation.toModel()

      service.hasBookingChanged(1L, amendCourtBookingRequest, COURT_USER) isEqualTo false
    }

    @Test
    fun `should fail when booking not found as court user`() {
      whenever(videoBookingRepository.findById(1L)) doReturn Optional.empty()

      val error = assertThrows<EntityNotFoundException> { service.hasBookingChanged(1L, amendCourtBookingRequest, COURT_USER) }
      error.message isEqualTo "Video booking with ID 1 not found."
    }

    @Test
    fun `should fail when booking types do not match as court user`() {
      whenever(videoBookingRepository.findById(1L)) doReturn Optional.of(probationBooking())

      val error = assertThrows<IllegalArgumentException> { service.hasBookingChanged(1L, amendCourtBookingRequest, COURT_USER) }
      error.message isEqualTo "Request type and existing booking type must be the same. Request type is COURT and booking type is PROBATION."
    }

    @Test
    fun `should be no changes for prisoner notes as court user`() {
      val differentNotesForPrisoner = amendCourtBookingRequest.copy(notesForPrisoners = "updated")

      whenever(videoBookingRepository.findById(1L)) doReturn Optional.of(existingCourtBooking)
      whenever(locationsService.getLocationByKey(amendCourtBookingRequest.prisoners.single().appointments.single().locationKey!!)) doReturn wandsworthLocation.toModel()

      service.hasBookingChanged(1L, differentNotesForPrisoner, COURT_USER) isEqualTo false
    }

    @Test
    fun `should be changes for prisoner notes as prison user`() {
      val differentNotesForPrisoner = amendCourtBookingRequest.copy(notesForPrisoners = "updated")

      whenever(videoBookingRepository.findById(1L)) doReturn Optional.of(existingCourtBooking)
      whenever(locationsService.getLocationByKey(amendCourtBookingRequest.prisoners.single().appointments.single().locationKey!!)) doReturn wandsworthLocation.toModel()

      service.hasBookingChanged(1L, differentNotesForPrisoner, PRISON_USER_WANDSWORTH) isEqualTo true
    }

    @Test
    fun `should be changes for hearing type as court user`() {
      val differentHearingType = amendCourtBookingRequest.copy(courtHearingType = CourtHearingType.OTHER)

      whenever(videoBookingRepository.findById(1L)) doReturn Optional.of(existingCourtBooking)
      whenever(locationsService.getLocationByKey(amendCourtBookingRequest.prisoners.single().appointments.single().locationKey!!)) doReturn wandsworthLocation.toModel()

      service.hasBookingChanged(1L, differentHearingType, COURT_USER) isEqualTo true
    }

    @Test
    fun `should be change for video link url as court user`() {
      val differentHearingType = amendCourtBookingRequest.copy(videoLinkUrl = "updated")

      whenever(videoBookingRepository.findById(1L)) doReturn Optional.of(existingCourtBooking)
      whenever(locationsService.getLocationByKey(amendCourtBookingRequest.prisoners.single().appointments.single().locationKey!!)) doReturn wandsworthLocation.toModel()

      service.hasBookingChanged(1L, differentHearingType, COURT_USER) isEqualTo true
    }
  }

  @Nested
  inner class HasProbationBookingChanged {
    private val existingProbationBooking = probationBooking(
      meetingType = ProbationMeetingType.PSR,
      notesForStaff = "staff notes",
      notesForPrisoners = "prisoner notes",
    ).withProbationPrisonAppointment(
      date = today(),
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(10, 0),
      prisonCode = WANDSWORTH,
      location = wandsworthLocation,
    )

    private val amendProbationBookingRequest = amendProbationBookingRequest(
      probationMeetingType = ProbationMeetingType.PSR,
      prisonCode = WANDSWORTH,
      notesForStaff = "staff notes",
      notesForPrisoners = "prisoner notes",
      appointmentDate = today(),
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(10, 0),
      location = wandsworthLocation,
    )

    @Test
    fun `should be no changes as probation user`() {
      whenever(videoBookingRepository.findById(1L)) doReturn Optional.of(existingProbationBooking)
      whenever(locationsService.getLocationByKey(amendProbationBookingRequest.prisoners.single().appointments.single().locationKey!!)) doReturn wandsworthLocation.toModel()

      service.hasBookingChanged(1L, amendProbationBookingRequest, PROBATION_USER) isEqualTo false
    }

    @Test
    fun `should be no changes for prisoner notes as probation user`() {
      val differentNotesForPrisoner = amendProbationBookingRequest.copy(notesForPrisoners = "updated")

      whenever(videoBookingRepository.findById(1L)) doReturn Optional.of(existingProbationBooking)
      whenever(locationsService.getLocationByKey(amendProbationBookingRequest.prisoners.single().appointments.single().locationKey!!)) doReturn wandsworthLocation.toModel()

      service.hasBookingChanged(1L, differentNotesForPrisoner, COURT_USER) isEqualTo false
    }

    @Test
    fun `should be changes for prisoner notes as prison user`() {
      val differentNotesForPrisoner = amendProbationBookingRequest.copy(notesForPrisoners = "updated")

      whenever(videoBookingRepository.findById(1L)) doReturn Optional.of(existingProbationBooking)
      whenever(locationsService.getLocationByKey(amendProbationBookingRequest.prisoners.single().appointments.single().locationKey!!)) doReturn wandsworthLocation.toModel()

      service.hasBookingChanged(1L, differentNotesForPrisoner, PRISON_USER_WANDSWORTH) isEqualTo true
    }

    @Test
    fun `should be changes for meeting type as probation user`() {
      val differentMeetingType = amendProbationBookingRequest.copy(probationMeetingType = ProbationMeetingType.OTHER)

      whenever(videoBookingRepository.findById(1L)) doReturn Optional.of(existingProbationBooking)
      whenever(locationsService.getLocationByKey(amendProbationBookingRequest.prisoners.single().appointments.single().locationKey!!)) doReturn wandsworthLocation.toModel()

      service.hasBookingChanged(1L, differentMeetingType, COURT_USER) isEqualTo true
    }

    @Test
    fun `should be changes for notes for staff as probation user`() {
      val differentNotesForStaff = amendProbationBookingRequest.copy(notesForStaff = "updated")

      whenever(videoBookingRepository.findById(1L)) doReturn Optional.of(existingProbationBooking)
      whenever(locationsService.getLocationByKey(amendProbationBookingRequest.prisoners.single().appointments.single().locationKey!!)) doReturn wandsworthLocation.toModel()

      service.hasBookingChanged(1L, differentNotesForStaff, COURT_USER) isEqualTo true
    }
  }

  @Nested
  inner class CourtBookingEquality {
    private val courtBooking = CourtBooking(
      hearingType = CourtHearingType.OTHER.name,
      date = today(),
      preStartTime = LocalTime.of(11, 0),
      preEndTime = LocalTime.of(12, 0),
      preLocation = UUID.fromString("00000000-0000-0000-0000-000000000000"),
      mainStartTime = LocalTime.of(12, 0),
      mainEndTime = LocalTime.of(13, 0),
      mainLocation = UUID.fromString("00000000-0000-0000-0000-000000000000"),
      postStartTime = LocalTime.of(13, 0),
      postEndTime = LocalTime.of(14, 0),
      postLocation = UUID.fromString("00000000-0000-0000-0000-000000000000"),
      cvpLink = "cvp-link",
      hmctsNumber = "12345",
      guestPin = "9999",
      notesForStaff = "staff notes",
      notesForPrisoners = "prisoner notes",
    )

    @Test
    fun `should be equal`() {
      courtBooking isEqualTo courtBooking.copy()
    }

    @Test
    fun `should not be equal`() {
      assertCourtBookingsNotEqual(courtBooking.copy(hearingType = "FAMILY"))
      assertCourtBookingsNotEqual(courtBooking.copy(date = yesterday()))
      assertCourtBookingsNotEqual(courtBooking.copy(preStartTime = courtBooking.preStartTime?.plusMinutes(1)))
      assertCourtBookingsNotEqual(courtBooking.copy(preEndTime = courtBooking.preEndTime?.minusMinutes(1)))
      assertCourtBookingsNotEqual(courtBooking.copy(preLocation = UUID.fromString("00000000-0000-0000-0000-000000000001")))
      assertCourtBookingsNotEqual(courtBooking.copy(mainStartTime = courtBooking.mainStartTime.minusMinutes(1)))
      assertCourtBookingsNotEqual(courtBooking.copy(mainEndTime = courtBooking.mainEndTime.minusMinutes(1)))
      assertCourtBookingsNotEqual(courtBooking.copy(mainLocation = UUID.fromString("00000000-0000-0000-0000-000000000001")))
      assertCourtBookingsNotEqual(courtBooking.copy(postStartTime = courtBooking.postStartTime?.minusMinutes(1)))
      assertCourtBookingsNotEqual(courtBooking.copy(postEndTime = courtBooking.postEndTime?.minusMinutes(1)))
      assertCourtBookingsNotEqual(courtBooking.copy(postLocation = UUID.fromString("00000000-0000-0000-0000-000000000001")))
      assertCourtBookingsNotEqual(courtBooking.copy(cvpLink = "updated"))
      assertCourtBookingsNotEqual(courtBooking.copy(hmctsNumber = "updated"))
      assertCourtBookingsNotEqual(courtBooking.copy(guestPin = "updated"))
      assertCourtBookingsNotEqual(courtBooking.copy(notesForStaff = "updated"))
      assertCourtBookingsNotEqual(courtBooking.copy(notesForPrisoners = "updated"))
    }

    private fun assertCourtBookingsNotEqual(booking: CourtBooking) {
      courtBooking isNotEqualTo booking
    }
  }

  @Nested
  inner class ProbationBookingEquality {
    private val probationBooking = ProbationBooking(
      meetingType = ProbationMeetingType.PSR.name,
      location = UUID.fromString("00000000-0000-0000-0000-000000000000"),
      date = today(),
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      notesForStaff = "staff notes",
      notesForPrisoners = "prisoner notes",
      contactName = "fred",
      contactEmail = "email",
      contactPhoneNumber = "1234",
    )

    @Test
    fun `should be equal`() {
      probationBooking isEqualTo probationBooking.copy()
    }

    @Test
    fun `should not be equal`() {
      assertProbationBookingsNotEqual(probationBooking.copy(meetingType = ProbationMeetingType.OTHER.name))
      assertProbationBookingsNotEqual(probationBooking.copy(location = UUID.fromString("00000000-0000-0000-0000-000000000001")))
      assertProbationBookingsNotEqual(probationBooking.copy(date = yesterday()))
      assertProbationBookingsNotEqual(probationBooking.copy(startTime = probationBooking.startTime?.plusMinutes(1)))
      assertProbationBookingsNotEqual(probationBooking.copy(startTime = probationBooking.endTime?.plusMinutes(1)))
      assertProbationBookingsNotEqual(probationBooking.copy(notesForStaff = "updated"))
      assertProbationBookingsNotEqual(probationBooking.copy(notesForPrisoners = "updated"))
      assertProbationBookingsNotEqual(probationBooking.copy(contactName = "updated"))
      assertProbationBookingsNotEqual(probationBooking.copy(contactEmail = "updated"))
      assertProbationBookingsNotEqual(probationBooking.copy(contactEmail = "999"))
    }

    private fun assertProbationBookingsNotEqual(booking: ProbationBooking) {
      probationBooking isNotEqualTo booking
    }
  }
}
