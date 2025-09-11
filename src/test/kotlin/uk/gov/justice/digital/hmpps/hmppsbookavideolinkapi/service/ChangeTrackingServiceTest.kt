package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.CvpLinkDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.AmendCourtBookingRequestBuilder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.SERVICE_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendProbationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation2
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthPrisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withPreMainPostCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AdditionalBookingDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.AdditionalBookingDetailRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.time.LocalTime
import java.util.Optional

class ChangeTrackingServiceTest {

  private val videoBookingRepository: VideoBookingRepository = mock()
  private val locationsService: LocationsService = mock()
  private val additionalBookingDetailRepository: AdditionalBookingDetailRepository = mock()
  private val service = ChangeTrackingService(videoBookingRepository, locationsService, additionalBookingDetailRepository)

  @Nested
  inner class DetermineChangeTypeForCourtBooking {

    private val existingCourtBooking = courtBooking(
      hearingType = CourtHearingType.TRIBUNAL,
      cvpLinkDetails = CvpLinkDetails.url("https://video.link.com"),
      notesForStaff = "staff notes",
      notesForPrisoners = "prisoner notes",
    ).withPreMainPostCourtPrisonAppointment(
      prisonCode = WANDSWORTH,
      location = wandsworthLocation,
      date = today(),
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
    )

    private val amendCourtBookingRequest = AmendCourtBookingRequestBuilder.builder {
      prisoner(wandsworthPrisoner)
      pre(wandsworthLocation)
      main(wandsworthLocation, today(), LocalTime.of(10, 0), LocalTime.of(11, 0))
      post(wandsworthLocation)
      cvpLinkDetails(CvpLinkDetails.url("https://video.link.com"))
      hearingType(CourtHearingType.TRIBUNAL)
      staffNotes("staff notes")
      prisonerNotes("prisoner notes")
    }.build()

    @BeforeEach
    fun before() {
      whenever(videoBookingRepository.findById(1)) doReturn Optional.of(existingCourtBooking)
      whenever(locationsService.getLocationByKey(wandsworthLocation.key)) doReturn wandsworthLocation.toModel()
      whenever(locationsService.getLocationByKey(wandsworthLocation2.key)) doReturn wandsworthLocation2.toModel()
    }

    @Test
    fun `should be no change as court user`() {
      service.determineChangeType(1, amendCourtBookingRequest, COURT_USER) isEqualTo ChangeType.NONE
    }

    @Test
    fun `should fail when booking not found as court user`() {
      whenever(videoBookingRepository.findById(99)) doReturn Optional.empty()

      val error = assertThrows<EntityNotFoundException> { service.determineChangeType(99, amendCourtBookingRequest, COURT_USER) }
      error.message isEqualTo "Video booking with ID 99 not found."
    }

    @Test
    fun `should fail for unsupported user type`() {
      val error = assertThrows<IllegalArgumentException> { service.determineChangeType(1, amendCourtBookingRequest, SERVICE_USER) }
      error.message isEqualTo "Only prison users and external users are supported. ServiceUser is not supported."
    }

    @Test
    fun `should fail when booking types do not match as court user`() {
      whenever(videoBookingRepository.findById(1)) doReturn Optional.of(probationBooking())

      val error = assertThrows<IllegalArgumentException> { service.determineChangeType(1, amendCourtBookingRequest, COURT_USER) }
      error.message isEqualTo "Request type and existing booking type must be the same. Request type is COURT and booking type is PROBATION."
    }

    @Test
    fun `should be global change for hearing type as court user`() {
      val differentHearingType = amendCourtBookingRequest.copy(courtHearingType = CourtHearingType.OTHER)

      service.determineChangeType(1, differentHearingType, COURT_USER) isEqualTo ChangeType.GLOBAL
    }

    @Test
    fun `should be global change for date as court user`() {
      val differentDate = AmendCourtBookingRequestBuilder.builder {
        prisoner(wandsworthPrisoner)
        pre(wandsworthLocation)
        main(wandsworthLocation, tomorrow(), LocalTime.of(10, 0), LocalTime.of(11, 0))
        post(wandsworthLocation)
        cvpLinkDetails(CvpLinkDetails.url("https://video.link.com"))
        hearingType(CourtHearingType.TRIBUNAL)
        staffNotes("staff notes")
        prisonerNotes("prisoner notes")
      }.build()

      service.determineChangeType(1, differentDate, COURT_USER) isEqualTo ChangeType.GLOBAL
    }

    @Test
    fun `should be global change for main start time as court user`() {
      val differentStart = AmendCourtBookingRequestBuilder.builder {
        prisoner(wandsworthPrisoner)
        pre(wandsworthLocation)
        main(wandsworthLocation, today(), LocalTime.of(10, 1), LocalTime.of(11, 0))
        post(wandsworthLocation)
        cvpLinkDetails(CvpLinkDetails.url("https://video.link.com"))
        hearingType(CourtHearingType.TRIBUNAL)
        staffNotes("staff notes")
        prisonerNotes("prisoner notes")
      }.build()

      service.determineChangeType(1, differentStart, COURT_USER) isEqualTo ChangeType.GLOBAL
    }

    @Test
    fun `should be global change for main end time as court user`() {
      val differentEnd = AmendCourtBookingRequestBuilder.builder {
        prisoner(wandsworthPrisoner)
        pre(wandsworthLocation)
        main(wandsworthLocation, today(), LocalTime.of(10, 0), LocalTime.of(11, 1))
        post(wandsworthLocation)
        cvpLinkDetails(CvpLinkDetails.url("https://video.link.com"))
        hearingType(CourtHearingType.TRIBUNAL)
        staffNotes("staff notes")
        prisonerNotes("prisoner notes")
      }.build()

      service.determineChangeType(1, differentEnd, COURT_USER) isEqualTo ChangeType.GLOBAL
    }

    @Test
    fun `should be global change for pre location as court user`() {
      val differentLocation = AmendCourtBookingRequestBuilder.builder {
        prisoner(wandsworthPrisoner)
        pre(wandsworthLocation2)
        main(wandsworthLocation, today(), LocalTime.of(10, 0), LocalTime.of(11, 0))
        post(wandsworthLocation)
        cvpLinkDetails(CvpLinkDetails.url("https://video.link.com"))
        hearingType(CourtHearingType.TRIBUNAL)
        staffNotes("staff notes")
        prisonerNotes("prisoner notes")
      }.build()

      service.determineChangeType(1, differentLocation, COURT_USER) isEqualTo ChangeType.GLOBAL
    }

    @Test
    fun `should be global change for main location as court user`() {
      val differentLocation = AmendCourtBookingRequestBuilder.builder {
        prisoner(wandsworthPrisoner)
        pre(wandsworthLocation)
        main(wandsworthLocation2, today(), LocalTime.of(10, 0), LocalTime.of(11, 0))
        post(wandsworthLocation)
        cvpLinkDetails(CvpLinkDetails.url("https://video.link.com"))
        hearingType(CourtHearingType.TRIBUNAL)
        staffNotes("staff notes")
        prisonerNotes("prisoner notes")
      }.build()

      service.determineChangeType(1, differentLocation, COURT_USER) isEqualTo ChangeType.GLOBAL
    }

    @Test
    fun `should be global change for post location as court user`() {
      val differentLocation = AmendCourtBookingRequestBuilder.builder {
        prisoner(wandsworthPrisoner)
        pre(wandsworthLocation)
        main(wandsworthLocation, today(), LocalTime.of(10, 0), LocalTime.of(11, 0))
        post(wandsworthLocation2)
        cvpLinkDetails(CvpLinkDetails.url("https://video.link.com"))
        hearingType(CourtHearingType.TRIBUNAL)
        staffNotes("staff notes")
        prisonerNotes("prisoner notes")
      }.build()

      service.determineChangeType(1, differentLocation, COURT_USER) isEqualTo ChangeType.GLOBAL
    }

    @Test
    fun `should be global change for video URL as court user`() {
      val differentVideoUrl = amendCourtBookingRequest.copy(videoLinkUrl = "updated")

      service.determineChangeType(1, differentVideoUrl, COURT_USER) isEqualTo ChangeType.GLOBAL
    }

    @Test
    fun `should be global change for HMCTS number as court user`() {
      val differentHmctsNumber = amendCourtBookingRequest.copy(hmctsNumber = "updated")

      service.determineChangeType(1, differentHmctsNumber, COURT_USER) isEqualTo ChangeType.GLOBAL
    }

    @Test
    fun `should be global change for guest PIN as court user`() {
      val differentGuestPin = amendCourtBookingRequest.copy(guestPin = "updated")

      service.determineChangeType(1, differentGuestPin, COURT_USER) isEqualTo ChangeType.GLOBAL
    }

    @Test
    fun `should be global change for video link url as court user`() {
      val differentHearingType = amendCourtBookingRequest.copy(videoLinkUrl = "updated")

      service.determineChangeType(1, differentHearingType, COURT_USER) isEqualTo ChangeType.GLOBAL
    }

    @Test
    fun `should be global change for staff notes as court user`() {
      val differentStaffNotes = amendCourtBookingRequest.copy(notesForStaff = "updated")

      service.determineChangeType(1, differentStaffNotes, COURT_USER) isEqualTo ChangeType.GLOBAL
    }

    @Test
    fun `should be no change for prisoner notes as court user`() {
      val differentNotesForPrisoner = amendCourtBookingRequest.copy(notesForPrisoners = "updated")

      service.determineChangeType(1, differentNotesForPrisoner, COURT_USER) isEqualTo ChangeType.NONE
    }

    @Test
    fun `should be prison change for prisoner notes only as prison user`() {
      val differentNotesForPrisoner = amendCourtBookingRequest.copy(notesForPrisoners = "updated")

      service.determineChangeType(1, differentNotesForPrisoner, PRISON_USER_WANDSWORTH) isEqualTo ChangeType.PRISON
    }

    @Test
    fun `should be global change for prisoner and staff notes as prison user`() {
      val differentNotes = amendCourtBookingRequest.copy(notesForPrisoners = "updated", notesForStaff = "updated")

      service.determineChangeType(1, differentNotes, PRISON_USER_WANDSWORTH) isEqualTo ChangeType.GLOBAL
    }
  }

  @Nested
  inner class DetermineChangeTypeForProbationBooking {

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

    @BeforeEach
    fun before() {
      whenever(videoBookingRepository.findById(1)) doReturn Optional.of(existingProbationBooking)
      whenever(locationsService.getLocationByKey(wandsworthLocation.key)) doReturn wandsworthLocation.toModel()
      whenever(locationsService.getLocationByKey(wandsworthLocation2.key)) doReturn wandsworthLocation2.toModel()
    }

    @Test
    fun `should be no change as probation user`() {
      service.determineChangeType(1, amendProbationBookingRequest, PROBATION_USER) isEqualTo ChangeType.NONE
    }

    @Test
    fun `should be global change for meeting type as probation user`() {
      val differentMeetingType = amendProbationBookingRequest.copy(probationMeetingType = ProbationMeetingType.OTHER)

      service.determineChangeType(1, differentMeetingType, PROBATION_USER) isEqualTo ChangeType.GLOBAL
    }

    @Test
    fun `should be global change for date as probation user`() {
      val details = amendProbationBookingRequest.prisoners.single()
      val appointment = details.appointments.single().copy(date = tomorrow())
      val differentDate = amendProbationBookingRequest.copy(prisoners = listOf(details.copy(appointments = listOf(appointment))))

      service.determineChangeType(1, differentDate, PROBATION_USER) isEqualTo ChangeType.GLOBAL
    }

    @Test
    fun `should be global change for start time as probation user`() {
      val details = amendProbationBookingRequest.prisoners.single()
      val appointment = details.appointments.single().copy(startTime = LocalTime.of(10, 1))
      val differentStart = amendProbationBookingRequest.copy(prisoners = listOf(details.copy(appointments = listOf(appointment))))

      service.determineChangeType(1, differentStart, PROBATION_USER) isEqualTo ChangeType.GLOBAL
    }

    @Test
    fun `should be global change for end time as probation user`() {
      val details = amendProbationBookingRequest.prisoners.single()
      val appointment = details.appointments.single().copy(endTime = LocalTime.of(11, 1))
      val differentStart = amendProbationBookingRequest.copy(prisoners = listOf(details.copy(appointments = listOf(appointment))))

      service.determineChangeType(1, differentStart, PROBATION_USER) isEqualTo ChangeType.GLOBAL
    }

    @Test
    fun `should be global change for location as probation user`() {
      val details = amendProbationBookingRequest.prisoners.single()
      val appointment = details.appointments.single().copy(locationKey = wandsworthLocation2.key)
      val differentLocation = amendProbationBookingRequest.copy(prisoners = listOf(details.copy(appointments = listOf(appointment))))

      service.determineChangeType(1, differentLocation, PROBATION_USER) isEqualTo ChangeType.GLOBAL
    }

    @Test
    fun `should be global change for notes for staff as probation user`() {
      val differentNotesForStaff = amendProbationBookingRequest.copy(notesForStaff = "updated")

      service.determineChangeType(1, differentNotesForStaff, PROBATION_USER) isEqualTo ChangeType.GLOBAL
    }

    @Test
    fun `should be global change for additional details as probation user`() {
      val differentAdditionalDetails = amendProbationBookingRequest.copy(
        notesForStaff = "updated",
        additionalBookingDetails = AdditionalBookingDetails(
          contactName = "name",
          contactEmail = "email",
          contactNumber = "0123456789",
        ),
      )

      service.determineChangeType(1, differentAdditionalDetails, PROBATION_USER) isEqualTo ChangeType.GLOBAL
    }

    @Test
    fun `should be no change for prisoner notes as probation user`() {
      val differentNotesForPrisoner = amendProbationBookingRequest.copy(notesForPrisoners = "updated")

      service.determineChangeType(1, differentNotesForPrisoner, PROBATION_USER) isEqualTo ChangeType.NONE
    }

    @Test
    fun `should be prison change for prisoner notes as prison user`() {
      val differentNotesForPrisoner = amendProbationBookingRequest.copy(notesForPrisoners = "updated")

      service.determineChangeType(1, differentNotesForPrisoner, PRISON_USER_WANDSWORTH) isEqualTo ChangeType.PRISON
    }

    @Test
    fun `should be global change for prisoner and staff notes as prison user`() {
      val differentNotes = amendProbationBookingRequest.copy(notesForPrisoners = "updated", notesForStaff = "updated")

      service.determineChangeType(1, differentNotes, PRISON_USER_WANDSWORTH) isEqualTo ChangeType.GLOBAL
    }
  }
}
