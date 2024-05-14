package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toMinutePrecision
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import java.time.LocalDateTime
import java.util.*

class CreateVideoBookingServiceTest {

  private val courtRepository: CourtRepository = mock()
  private val prisonerSearchClient: PrisonerSearchClient = mock()
  private val probationTeamRepository: ProbationTeamRepository = mock()
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val persistedVideoBooking: VideoBooking = mock()
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()

  private val service = CreateVideoBookingService(
    courtRepository,
    prisonerSearchClient,
    probationTeamRepository,
    videoBookingRepository,
    prisonAppointmentRepository,
  )

  private var newBookingCaptor = argumentCaptor<VideoBooking>()

  private var appointmentsCaptor = argumentCaptor<PrisonAppointment>()

  @Test
  fun `should create a court video booking`() {
    val prisonCode = "MDI"
    val prisonerNumber = "123456"
    val courtBookingRequest = courtBookingRequest(prisonCode = prisonCode, prisonerNumber = prisonerNumber)
    val requestedCourt = court(courtBookingRequest.courtId!!)

    whenever(courtRepository.findById(courtBookingRequest.courtId!!)) doReturn Optional.of(requestedCourt)
    whenever(prisonerSearchClient.getPrisonerAtPrison(prisonCode = prisonCode, prisonerNumber = prisonerNumber)) doReturn Prisoner(prisonerNumber = prisonerNumber, prisonId = prisonCode)
    whenever(videoBookingRepository.saveAndFlush(any())) doReturn persistedVideoBooking

    service.create(courtBookingRequest)

    verify(videoBookingRepository).saveAndFlush(newBookingCaptor.capture())

    with(newBookingCaptor.firstValue) {
      bookingType isEqualTo "COURT"
      court isEqualTo requestedCourt
      createdTime isCloseTo LocalDateTime.now()
      hearingType isEqualTo courtBookingRequest.courtHearingType?.name
      videoUrl isEqualTo courtBookingRequest.videoLinkUrl
    }
  }

  @Test
  fun `should fail to create a court video booking when court not found`() {
    val courtBookingRequest = courtBookingRequest()

    whenever(courtRepository.findById(courtBookingRequest.courtId!!)) doReturn Optional.empty()

    val error = assertThrows<EntityNotFoundException> { service.create(courtBookingRequest) }

    error.message isEqualTo "Court with ID ${courtBookingRequest.courtId} not found"

    verifyNoInteractions(videoBookingRepository)
  }

  @Test
  fun `should create a probation video booking`() {
    val prisonCode = "MDI"
    val prisonerNumber = "123456"
    val probationBookingRequest = probationBookingRequest(prisonCode = prisonCode, prisonerNumber = prisonerNumber)
    val requestedProbationTeam = probationTeam(probationBookingRequest.probationTeamId!!)

    whenever(probationTeamRepository.findById(probationBookingRequest.probationTeamId!!)) doReturn Optional.of(requestedProbationTeam)
    whenever(prisonerSearchClient.getPrisonerAtPrison(prisonCode = prisonCode, prisonerNumber = prisonerNumber)) doReturn Prisoner(prisonerNumber = prisonerNumber, prisonId = prisonCode)
    whenever(videoBookingRepository.saveAndFlush(any())) doReturn persistedVideoBooking

    service.create(probationBookingRequest)

    verify(videoBookingRepository).saveAndFlush(newBookingCaptor.capture())

    with(newBookingCaptor.firstValue) {
      bookingType isEqualTo "PROBATION"
      probationTeam isEqualTo requestedProbationTeam
      createdTime isCloseTo LocalDateTime.now()
      probationMeetingType isEqualTo probationBookingRequest.probationMeetingType?.name
      videoUrl isEqualTo probationBookingRequest.videoLinkUrl
    }

    verify(prisonAppointmentRepository).saveAndFlush(appointmentsCaptor.capture())

    appointmentsCaptor.allValues.size isEqualTo 1

    with(appointmentsCaptor.firstValue) {
      val prisoner = probationBookingRequest.prisoners.single()

      videoBooking isEqualTo persistedVideoBooking
      prisonCode isEqualTo prisoner.prisonCode!!
      prisonerNumber isEqualTo prisoner.prisonerNumber!!
      appointmentType isEqualTo prisoner.appointments.single().type?.name
      appointmentDate isEqualTo prisoner.appointments.single().date!!
      startTime isEqualTo prisoner.appointments.single().startTime!!.toMinutePrecision()
      endTime isEqualTo prisoner.appointments.single().endTime!!.toMinutePrecision()
      prisonLocKey isEqualTo prisoner.appointments.single().locationKey!!
      createdBy isEqualTo "TBD"
    }
  }

  @Test
  fun `should fail to create a probation video booking when team not found`() {
    val probationBookingRequest = probationBookingRequest()

    whenever(probationTeamRepository.findById(probationBookingRequest.probationTeamId!!)) doReturn Optional.empty()

    val error = assertThrows<EntityNotFoundException> { service.create(probationBookingRequest) }

    error.message isEqualTo "Probation team with ID ${probationBookingRequest.probationTeamId} not found"

    verifyNoInteractions(videoBookingRepository)
  }

  @Test
  fun `should fail to create a probation video booking when appointment type not probation specific`() {
    val prisonCode = "MDI"
    val prisonerNumber = "123456"
    val probationBookingRequest = probationBookingRequest(prisonCode = prisonCode, prisonerNumber = prisonerNumber, appointmentType = AppointmentType.VLB_COURT_MAIN)
    val requestedProbationTeam = probationTeam(probationBookingRequest.probationTeamId!!)

    whenever(probationTeamRepository.findById(probationBookingRequest.probationTeamId!!)) doReturn Optional.of(requestedProbationTeam)
    whenever(prisonerSearchClient.getPrisonerAtPrison(prisonCode = prisonCode, prisonerNumber = prisonerNumber)) doReturn Prisoner(prisonerNumber = prisonerNumber, prisonId = prisonCode)
    whenever(videoBookingRepository.saveAndFlush(any())) doReturn persistedVideoBooking

    val error = assertThrows<IllegalArgumentException> { service.create(probationBookingRequest) }

    error.message isEqualTo "Appointment type VLB_COURT_MAIN is not valid for probation appointments"
  }
}