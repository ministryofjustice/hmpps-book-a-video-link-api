package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationValidator
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerValidator
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toMinutePrecision
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AdditionalBookingDetail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.SERVICE_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AdditionalBookingDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.AdditionalBookingDetailRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import java.time.LocalDateTime
import java.time.LocalTime

class CreateProbationBookingServiceTest {
  private val probationTeamRepository: ProbationTeamRepository = mock()
  private val videoBookingRepository: VideoBookingRepository = mock()

  private val bookingHistoryService: BookingHistoryService = mock()
  private val prisonRepository: PrisonRepository = mock()
  private val prisonerValidator: PrisonerValidator = mock()

  private val persistedVideoBooking: VideoBooking = mock()
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient = mock()
  private val locationValidator: LocationValidator = mock()
  private val additionalBookingDetailRepository: AdditionalBookingDetailRepository = mock()
  private val persistedAdditionalBookingDetail: AdditionalBookingDetail = mock()

  private val appointmentsService = AppointmentsService(prisonAppointmentRepository, prisonRepository, locationsInsidePrisonClient, locationValidator)

  private val service = CreateProbationBookingService(
    probationTeamRepository,
    videoBookingRepository,
    appointmentsService,
    bookingHistoryService,
    prisonRepository,
    prisonerValidator,
    additionalBookingDetailRepository,
  )

  private val newBookingCaptor = argumentCaptor<VideoBooking>()
  private val additionalBookingDetailCaptor = argumentCaptor<AdditionalBookingDetail>()

  @Test
  fun `should create a probation video booking for probation user`() {
    val prisonCode = BIRMINGHAM
    val prisonerNumber = "123456"
    val probationBookingRequest = probationBookingRequest(
      prisonCode = prisonCode,
      prisonerNumber = prisonerNumber,
      location = birminghamLocation,
    ).copy(additionalBookingDetails = AdditionalBookingDetails("Fred", "fred@email.com", "0173 361 6789", "some extra information for Fred"))

    val requestedProbationTeam = probationTeam(probationBookingRequest.probationTeamCode!!)

    whenever(probationTeamRepository.findByCode(probationBookingRequest.probationTeamCode!!)) doReturn requestedProbationTeam
    whenever(videoBookingRepository.saveAndFlush(any())) doReturn persistedVideoBooking
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn prison(BIRMINGHAM)
    whenever(prisonerValidator.validatePrisonerAtPrison(prisonerNumber, prisonCode)) doReturn prisonerSearchPrisoner(prisonerNumber, prisonCode)
    whenever(locationValidator.validatePrisonLocation(BIRMINGHAM, birminghamLocation.key)) doReturn birminghamLocation
    whenever(locationsInsidePrisonClient.getLocationByKey(birminghamLocation.key)) doReturn birminghamLocation
    whenever(additionalBookingDetailRepository.saveAndFlush(any())) doReturn persistedAdditionalBookingDetail
    val (booking, prisoner) = service.create(probationBookingRequest, PROBATION_USER)

    booking isEqualTo persistedVideoBooking
    prisoner isEqualTo prisoner(prisonerNumber, prisonCode)

    verify(videoBookingRepository).saveAndFlush(newBookingCaptor.capture())
    verify(additionalBookingDetailRepository).saveAndFlush(additionalBookingDetailCaptor.capture())

    with(newBookingCaptor.firstValue) {
      bookingType isEqualTo BookingType.PROBATION
      probationTeam isEqualTo requestedProbationTeam
      createdTime isCloseTo LocalDateTime.now()
      probationMeetingType isEqualTo probationBookingRequest.probationMeetingType?.name
      comments isEqualTo "probation booking comments"
      videoUrl isEqualTo probationBookingRequest.videoLinkUrl
      createdBy isEqualTo PROBATION_USER.username
      createdTime isCloseTo LocalDateTime.now()

      appointments() hasSize 1

      with(appointments().single()) {
        val onePrisoner = probationBookingRequest.prisoners.single()

        prisonCode isEqualTo onePrisoner.prisonCode
        prisonerNumber isEqualTo onePrisoner.prisonerNumber
        appointmentType isEqualTo onePrisoner.appointments.single().type?.name
        appointmentDate isEqualTo onePrisoner.appointments.single().date!!
        startTime isEqualTo onePrisoner.appointments.single().startTime!!.toMinutePrecision()
        endTime isEqualTo onePrisoner.appointments.single().endTime!!.toMinutePrecision()
        prisonLocationId isEqualTo birminghamLocation.id
      }
    }

    with(additionalBookingDetailCaptor.firstValue) {
      contactName isEqualTo "Fred"
      contactEmail isEqualTo "fred@email.com"
      contactNumber isEqualTo "0173 361 6789"
      extraInformation isEqualTo "some extra information for Fred"
    }

    verify(locationValidator).validatePrisonLocation(BIRMINGHAM, birminghamLocation.key)
    verify(prisonerValidator).validatePrisonerAtPrison(prisonerNumber, BIRMINGHAM)
    verify(bookingHistoryService).createBookingHistory(any(), any())
  }

  @Test
  fun `should create a probation video booking for prison user`() {
    val prisonCode = BIRMINGHAM
    val prisonerNumber = "123456"
    val probationBookingRequest = probationBookingRequest(
      prisonCode = prisonCode,
      prisonerNumber = prisonerNumber,
      location = birminghamLocation,
    ).copy(additionalBookingDetails = AdditionalBookingDetails("Jane", "jane@email.com", "0114 561 6789", "some extra information for Jane"))

    val requestedProbationTeam = probationTeam(probationBookingRequest.probationTeamCode!!)

    whenever(probationTeamRepository.findByCode(probationBookingRequest.probationTeamCode!!)) doReturn requestedProbationTeam
    whenever(videoBookingRepository.saveAndFlush(any())) doReturn persistedVideoBooking
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn prison(BIRMINGHAM)
    whenever(prisonerValidator.validatePrisonerAtPrison(prisonerNumber, prisonCode)) doReturn prisonerSearchPrisoner(prisonerNumber, prisonCode)
    whenever(locationValidator.validatePrisonLocation(BIRMINGHAM, birminghamLocation.key)) doReturn birminghamLocation
    whenever(locationsInsidePrisonClient.getLocationByKey(birminghamLocation.key)) doReturn birminghamLocation
    whenever(additionalBookingDetailRepository.saveAndFlush(any())) doReturn persistedAdditionalBookingDetail
    val (booking, prisoner) = service.create(probationBookingRequest, PRISON_USER_BIRMINGHAM)

    booking isEqualTo persistedVideoBooking
    prisoner isEqualTo prisoner(prisonerNumber, prisonCode)

    verify(videoBookingRepository).saveAndFlush(newBookingCaptor.capture())
    verify(additionalBookingDetailRepository).saveAndFlush(additionalBookingDetailCaptor.capture())

    with(newBookingCaptor.firstValue) {
      bookingType isEqualTo BookingType.PROBATION
      probationTeam isEqualTo requestedProbationTeam
      createdTime isCloseTo LocalDateTime.now()
      probationMeetingType isEqualTo probationBookingRequest.probationMeetingType?.name
      comments isEqualTo "probation booking comments"
      videoUrl isEqualTo probationBookingRequest.videoLinkUrl
      createdBy isEqualTo PRISON_USER_BIRMINGHAM.username
      createdTime isCloseTo LocalDateTime.now()

      appointments() hasSize 1

      with(appointments().single()) {
        val onePrisoner = probationBookingRequest.prisoners.single()

        prisonCode isEqualTo onePrisoner.prisonCode
        prisonerNumber isEqualTo onePrisoner.prisonerNumber
        appointmentType isEqualTo onePrisoner.appointments.single().type?.name
        appointmentDate isEqualTo onePrisoner.appointments.single().date!!
        startTime isEqualTo onePrisoner.appointments.single().startTime!!.toMinutePrecision()
        endTime isEqualTo onePrisoner.appointments.single().endTime!!.toMinutePrecision()
        prisonLocationId isEqualTo birminghamLocation.id
      }
    }

    with(additionalBookingDetailCaptor.firstValue) {
      contactName isEqualTo "Jane"
      contactEmail isEqualTo "jane@email.com"
      contactNumber isEqualTo "0114 561 6789"
      extraInformation isEqualTo "some extra information for Jane"
    }

    verify(locationValidator).validatePrisonLocation(BIRMINGHAM, birminghamLocation.key)
    verify(prisonerValidator).validatePrisonerAtPrison(prisonerNumber, BIRMINGHAM)
    verify(bookingHistoryService).createBookingHistory(any(), any())
  }

  @Test
  fun `should fail to create a probation video booking for with missing extra details for meeting type OTHER`() {
    val prisonCode = BIRMINGHAM
    val prisonerNumber = "123456"
    val probationBookingRequest = probationBookingRequest(
      prisonCode = prisonCode,
      prisonerNumber = prisonerNumber,
      location = birminghamLocation,
      probationMeetingType = ProbationMeetingType.OTHER,
    ).copy(additionalBookingDetails = AdditionalBookingDetails("Fred", "fred@email.com", "0173 361 6789", extraInformation = null))

    val requestedProbationTeam = probationTeam(probationBookingRequest.probationTeamCode!!)

    whenever(probationTeamRepository.findByCode(probationBookingRequest.probationTeamCode!!)) doReturn requestedProbationTeam
    whenever(videoBookingRepository.saveAndFlush(any())) doReturn persistedVideoBooking
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn prison(BIRMINGHAM)
    whenever(prisonerValidator.validatePrisonerAtPrison(prisonerNumber, prisonCode)) doReturn prisonerSearchPrisoner(prisonerNumber, prisonCode)
    whenever(locationValidator.validatePrisonLocation(BIRMINGHAM, birminghamLocation.key)) doReturn birminghamLocation
    whenever(locationsInsidePrisonClient.getLocationByKey(birminghamLocation.key)) doReturn birminghamLocation
    whenever(additionalBookingDetailRepository.saveAndFlush(any())) doReturn persistedAdditionalBookingDetail

    val error = assertThrows<IllegalArgumentException> { service.create(probationBookingRequest, PROBATION_USER) }
    error.message isEqualTo "Probation meeting extra information is required for meeting type Other"
  }

  @Test
  fun `should fail to create a probation video booking when new appointment overlaps existing for probation user`() {
    val prisonCode = BIRMINGHAM
    val prisonerNumber = "123456"
    val probationBookingRequest = probationBookingRequest(
      prisonCode = prisonCode,
      prisonerNumber = prisonerNumber,
      startTime = LocalTime.of(8, 30),
      endTime = LocalTime.of(9, 30),
      location = birminghamLocation,
    )
    val requestedProbationTeam = probationTeam(probationBookingRequest.probationTeamCode!!)

    val overlappingAppointment: PrisonAppointment = mock {
      on { startTime } doReturn LocalTime.of(9, 0)
      on { endTime } doReturn LocalTime.of(10, 0)
    }

    whenever(probationTeamRepository.findByCode(probationBookingRequest.probationTeamCode!!)) doReturn requestedProbationTeam
    whenever(videoBookingRepository.saveAndFlush(any())) doReturn persistedVideoBooking
    whenever(prisonAppointmentRepository.findActivePrisonAppointmentsAtLocationOnDate(BIRMINGHAM, birminghamLocation.id, tomorrow())) doReturn listOf(overlappingAppointment)
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn prison(BIRMINGHAM)
    whenever(prisonerValidator.validatePrisonerAtPrison(prisonerNumber, BIRMINGHAM)) doReturn prisonerSearchPrisoner(prisonerNumber, BIRMINGHAM)
    whenever(locationValidator.validatePrisonLocation(BIRMINGHAM, birminghamLocation.key)) doReturn birminghamLocation
    whenever(locationsInsidePrisonClient.getLocationsByKeys(setOf(birminghamLocation.key))) doReturn listOf(birminghamLocation)

    val error = assertThrows<IllegalArgumentException> { service.create(probationBookingRequest, PROBATION_USER) }

    error.message isEqualTo "Requested probation appointment overlaps with an existing appointment at location ${birminghamLocation.key}"

    verifyNoInteractions(bookingHistoryService)
  }

  @Test
  fun `should fail to create a probation video booking when not probation user`() {
    assertThrows<IllegalArgumentException> { service.create(probationBookingRequest(), COURT_USER) }.message isEqualTo "Only probation users and prison users can create probation bookings."
    assertThrows<IllegalArgumentException> { service.create(probationBookingRequest(), SERVICE_USER) }.message isEqualTo "Only probation users and prison users can create probation bookings."

    verify(videoBookingRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `should fail to create a probation video booking when team not found for probation user`() {
    val probationBookingRequest = probationBookingRequest()

    whenever(probationTeamRepository.findByCode(probationBookingRequest.probationTeamCode!!)) doReturn null

    val error = assertThrows<EntityNotFoundException> { service.create(probationBookingRequest, PROBATION_USER) }

    error.message isEqualTo "Probation team with code ${probationBookingRequest.probationTeamCode} not found"

    verifyNoInteractions(videoBookingRepository)
  }

  @Test
  fun `should fail to create a probation video booking when prison not found for probation user`() {
    val probationBookingRequest = probationBookingRequest(prisonCode = BIRMINGHAM)

    whenever(probationTeamRepository.findByCode(probationBookingRequest.probationTeamCode!!)) doReturn probationTeam()
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn null

    val error = assertThrows<EntityNotFoundException> { service.create(probationBookingRequest, PROBATION_USER) }

    error.message isEqualTo "Prison with code $BIRMINGHAM not found"

    verifyNoInteractions(videoBookingRepository)
  }

  @Test
  fun `should fail to create a probation video booking when team not enabled for probation user`() {
    val probationBookingRequest = probationBookingRequest()
    val disabledProbationTeam = probationTeam(probationBookingRequest.probationTeamCode!!, false)

    whenever(probationTeamRepository.findByCode(probationBookingRequest.probationTeamCode!!)) doReturn disabledProbationTeam

    val error = assertThrows<IllegalArgumentException> { service.create(probationBookingRequest, PROBATION_USER) }

    error.message isEqualTo "Probation team with code ${probationBookingRequest.probationTeamCode} is not enabled"

    verifyNoInteractions(videoBookingRepository)
  }

  @Test
  fun `should fail to create a probation video booking when appointment type not probation specific for probation user`() {
    val prisonCode = BIRMINGHAM
    val prisonerNumber = "123456"
    val probationBookingRequest = probationBookingRequest(prisonCode = prisonCode, prisonerNumber = prisonerNumber, appointmentType = AppointmentType.VLB_COURT_MAIN)
    val requestedProbationTeam = probationTeam(probationBookingRequest.probationTeamCode!!)

    whenever(probationTeamRepository.findByCode(probationBookingRequest.probationTeamCode!!)) doReturn requestedProbationTeam
    whenever(videoBookingRepository.saveAndFlush(any())) doReturn persistedVideoBooking
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn prison(BIRMINGHAM)
    whenever(prisonerValidator.validatePrisonerAtPrison(prisonerNumber, BIRMINGHAM)) doReturn prisonerSearchPrisoner(prisonerNumber, BIRMINGHAM)
    whenever(locationValidator.validatePrisonLocations(BIRMINGHAM, setOf(birminghamLocation.key))) doReturn listOf(birminghamLocation)
    whenever(locationsInsidePrisonClient.getLocationByKey(birminghamLocation.key)) doReturn birminghamLocation

    val error = assertThrows<IllegalArgumentException> { service.create(probationBookingRequest, PROBATION_USER) }

    error.message isEqualTo "Appointment type VLB_COURT_MAIN is not valid for probation appointments"
  }
}
