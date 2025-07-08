package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationValidator
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AdditionalBookingDetail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendProbationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasAmendedBy
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasAmendedTimeCloseTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasAppointmentDate
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasAppointmentTypeProbation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasBookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasContactName
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasEmailAddress
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasEndTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasPhoneNumber
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasPrisonCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasProbationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasNotesForStaff
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasStartTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AdditionalBookingDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.AdditionalBookingDetailRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.VideoBookingAccessException
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

class AmendProbationBookingServiceTest {

  private val videoBookingRepository: VideoBookingRepository = mock()
  private val prisonRepository: PrisonRepository = mock()
  private val bookingHistoryService: BookingHistoryService = mock()
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient = mock()
  private val locationValidator: LocationValidator = mock()
  private val prisonerSearchClient: PrisonerSearchClient = mock()
  private val additionalBookingDetailRepository: AdditionalBookingDetailRepository = mock()
  private val appointmentsService = spy(AppointmentsService(prisonAppointmentRepository, prisonRepository, locationsInsidePrisonClient, locationValidator))

  private val service = AmendProbationBookingService(
    videoBookingRepository,
    prisonRepository,
    appointmentsService,
    bookingHistoryService,
    prisonerSearchClient,
    additionalBookingDetailRepository,
  )

  private val additionalBookingDetail: AdditionalBookingDetail = mock()
  private var amendedBookingCaptor = argumentCaptor<VideoBooking>()
  private var additionalBookingDetailCaptor = argumentCaptor<AdditionalBookingDetail>()

  @Test
  fun `should amend a PSR probation video booking for probation user`() {
    val probationBooking = probationBooking(meetingType = ProbationMeetingType.PSR, notesForStaff = "notes for staff").withProbationPrisonAppointment()
    val prisonerNumber = "123456"
    val probationBookingRequest = amendProbationBookingRequest(
      prisonCode = BIRMINGHAM,
      prisonerNumber = prisonerNumber,
      location = birminghamLocation,
      appointmentDate = tomorrow(),
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(13, 0),
      additionalBookingDetails = AdditionalBookingDetails(
        contactName = "contact name",
        contactEmail = "contact@email.com",
        contactNumber = "07928 660553",
      ),
      notesForStaff = "amended notes for staff",
    )

    withBookingFixture(2, probationBooking)
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    whenever(locationValidator.validatePrisonLocation(BIRMINGHAM, birminghamLocation.key)) doReturn birminghamLocation
    whenever(locationsInsidePrisonClient.getLocationByKey(birminghamLocation.key)) doReturn birminghamLocation

    val (booking, prisoner) = service.amend(2, probationBookingRequest, PROBATION_USER)

    booking isEqualTo probationBooking
    prisoner isEqualTo prisoner(prisonerNumber, BIRMINGHAM)

    verify(bookingHistoryService).createBookingHistory(HistoryType.AMEND, probationBooking)
    verify(videoBookingRepository).saveAndFlush(amendedBookingCaptor.capture())

    amendedBookingCaptor
      .firstValue
      .hasBookingType(BookingType.PROBATION)
      .hasProbationTeam(probationBooking.probationTeam!!)
      .hasMeetingType(ProbationMeetingType.PSR)
      .hasNotesForStaff("amended notes for staff")
      .hasAmendedBy(PROBATION_USER)
      .hasAmendedTimeCloseTo(LocalDateTime.now())
      .appointments()
      .single()
      .hasPrisonCode(BIRMINGHAM)
      .hasPrisonerNumber("123456")
      .hasAppointmentTypeProbation()
      .hasAppointmentDate(tomorrow())
      .hasStartTime(LocalTime.of(12, 0))
      .hasEndTime(LocalTime.of(13, 0))
      .hasLocation(birminghamLocation)

    inOrder(
      videoBookingRepository,
      prisonerSearchClient,
      locationValidator,
      appointmentsService,
      videoBookingRepository,
      additionalBookingDetailRepository,
      additionalBookingDetailRepository,
      bookingHistoryService,
    ) {
      verify(videoBookingRepository).findById(2)
      verify(prisonerSearchClient).getPrisoner(prisonerNumber)
      verify(locationValidator).validatePrisonLocation(BIRMINGHAM, birminghamLocation.key)
      verify(videoBookingRepository).saveAndFlush(probationBooking)
      verify(additionalBookingDetailRepository).findByVideoBooking(probationBooking)
      verify(additionalBookingDetailRepository).saveAndFlush(additionalBookingDetailCaptor.capture())
      verify(bookingHistoryService).createBookingHistory(HistoryType.AMEND, probationBooking)
    }

    additionalBookingDetailCaptor
      .firstValue
      .hasContactName("contact name")
      .hasEmailAddress("contact@email.com")
      .hasPhoneNumber("07928 660553")
  }

  @Test
  fun `should remove additional details from probation video booking for probation user`() {
    val probationBooking = probationBooking(meetingType = ProbationMeetingType.PSR, notesForStaff = "notes for staff").withProbationPrisonAppointment()
    val prisonerNumber = "123456"
    val probationBookingRequest = amendProbationBookingRequest(
      prisonCode = BIRMINGHAM,
      prisonerNumber = prisonerNumber,
      location = birminghamLocation,
      appointmentDate = tomorrow(),
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(13, 0),
      notesForStaff = "amended notes for staff",
    )

    withBookingFixture(2, probationBooking)
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    whenever(locationValidator.validatePrisonLocation(BIRMINGHAM, birminghamLocation.key)) doReturn birminghamLocation
    whenever(locationsInsidePrisonClient.getLocationByKey(birminghamLocation.key)) doReturn birminghamLocation
    whenever(additionalBookingDetailRepository.findByVideoBooking(probationBooking)) doReturn additionalBookingDetail

    val (booking, prisoner) = service.amend(2, probationBookingRequest, PROBATION_USER)

    booking isEqualTo probationBooking
    prisoner isEqualTo prisoner(prisonerNumber, BIRMINGHAM)

    verify(bookingHistoryService).createBookingHistory(HistoryType.AMEND, probationBooking)
    verify(videoBookingRepository).saveAndFlush(amendedBookingCaptor.capture())

    amendedBookingCaptor
      .firstValue
      .hasBookingType(BookingType.PROBATION)
      .hasProbationTeam(probationBooking.probationTeam!!)
      .hasMeetingType(ProbationMeetingType.PSR)
      .hasNotesForStaff("amended notes for staff")
      .hasAmendedBy(PROBATION_USER)
      .hasAmendedTimeCloseTo(LocalDateTime.now())
      .appointments()
      .single()
      .hasPrisonCode(BIRMINGHAM)
      .hasPrisonerNumber("123456")
      .hasAppointmentTypeProbation()
      .hasAppointmentDate(tomorrow())
      .hasStartTime(LocalTime.of(12, 0))
      .hasEndTime(LocalTime.of(13, 0))
      .hasLocation(birminghamLocation)

    inOrder(
      videoBookingRepository,
      prisonerSearchClient,
      locationValidator,
      appointmentsService,
      videoBookingRepository,
      additionalBookingDetailRepository,
      additionalBookingDetailRepository,
      additionalBookingDetailRepository,
      bookingHistoryService,
    ) {
      verify(videoBookingRepository).findById(2)
      verify(prisonerSearchClient).getPrisoner(prisonerNumber)
      verify(locationValidator).validatePrisonLocation(BIRMINGHAM, birminghamLocation.key)
      verify(videoBookingRepository).saveAndFlush(probationBooking)
      verify(additionalBookingDetailRepository).findByVideoBooking(probationBooking)
      verify(additionalBookingDetailRepository).delete(additionalBookingDetail)
      verify(additionalBookingDetailRepository).flush()
      verify(bookingHistoryService).createBookingHistory(HistoryType.AMEND, probationBooking)
    }
  }

  @Test
  fun `should fail to amend a probation video booking when new appointment overlaps existing for probation user`() {
    val prisonerNumber = "123456"
    val probationBookingRequest = amendProbationBookingRequest(
      prisonCode = BIRMINGHAM,
      prisonerNumber = prisonerNumber,
      startTime = LocalTime.of(8, 30),
      endTime = LocalTime.of(9, 30),
      location = birminghamLocation,
    )

    val overlappingAppointment: PrisonAppointment = mock {
      on { startTime } doReturn LocalTime.of(9, 0)
      on { endTime } doReturn LocalTime.of(10, 0)
    }

    val probationBooking = probationBooking().withProbationPrisonAppointment()
    withBookingFixture(2, probationBooking)
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)
    whenever(locationValidator.validatePrisonLocation(BIRMINGHAM, birminghamLocation.key)) doReturn birminghamLocation
    whenever(locationsInsidePrisonClient.getLocationByKey(birminghamLocation.key)) doReturn birminghamLocation
    whenever(prisonAppointmentRepository.findActivePrisonAppointmentsAtLocationOnDate(BIRMINGHAM, birminghamLocation.id, tomorrow())) doReturn listOf(overlappingAppointment)

    val error = assertThrows<IllegalArgumentException> { service.amend(2, probationBookingRequest, PROBATION_USER) }

    error.message isEqualTo "Requested probation appointment overlaps with an existing appointment at location ${birminghamLocation.key}"
  }

  @Test
  fun `should succeed to amend a PSR probation video booking when new appointment overlaps existing for prison user`() {
    val probationBooking = probationBooking(meetingType = ProbationMeetingType.PSR).withProbationPrisonAppointment()
    val prisonerNumber = "123456"
    val probationBookingRequest = amendProbationBookingRequest(
      prisonCode = BIRMINGHAM,
      prisonerNumber = prisonerNumber,
      startTime = LocalTime.of(8, 30),
      endTime = LocalTime.of(9, 30),
      location = birminghamLocation,
      probationMeetingType = ProbationMeetingType.PSR,
      additionalBookingDetails = AdditionalBookingDetails(
        contactName = "contact name",
        contactEmail = "contact@email.com",
        contactNumber = null,
      ),
    )

    val overlappingAppointment: PrisonAppointment = mock {
      on { startTime } doReturn LocalTime.of(9, 0)
      on { endTime } doReturn LocalTime.of(10, 0)
    }

    withBookingFixture(2, probationBooking)
    whenever(locationValidator.validatePrisonLocations(BIRMINGHAM, setOf(birminghamLocation.key))) doReturn listOf(birminghamLocation)
    whenever(locationsInsidePrisonClient.getLocationByKey(birminghamLocation.key)) doReturn birminghamLocation

    whenever(prisonAppointmentRepository.findActivePrisonAppointmentsAtLocationOnDate(BIRMINGHAM, birminghamLocation.id, tomorrow())) doReturn listOf(overlappingAppointment)
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    assertDoesNotThrow {
      service.amend(2, probationBookingRequest, prisonUser(activeCaseLoadId = probationBooking.prisonCode()))
    }
  }

  @Test
  fun `should fail to amend a probation video booking when prison not found for probation user`() {
    val probationBookingRequest = amendProbationBookingRequest(prisonCode = BIRMINGHAM)
    val probationBooking = probationBooking().withProbationPrisonAppointment()
    withBookingFixture(2, probationBooking)
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn null

    val error = assertThrows<EntityNotFoundException> { service.amend(2, probationBookingRequest, PROBATION_USER) }

    error.message isEqualTo "Prison with code $BIRMINGHAM not found"
  }

  @Test
  fun `should fail to amend a probation video booking when appointment type not probation specific for probation user`() {
    val prisonerNumber = "123456"
    val amendRequest = amendProbationBookingRequest(prisonCode = BIRMINGHAM, prisonerNumber = prisonerNumber, appointmentType = AppointmentType.VLB_COURT_MAIN)

    val probationBooking = probationBooking().withProbationPrisonAppointment()
    withBookingFixture(1, probationBooking)
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    val error = assertThrows<IllegalArgumentException> { service.amend(1, amendRequest, PROBATION_USER) }

    error.message isEqualTo "Appointment type VLB_COURT_MAIN is not valid for probation appointments"
  }

  @Test
  fun `should fail to amend a cancelled video booking`() {
    val prisonerNumber = "123456"
    val amendRequest = amendProbationBookingRequest()

    val cancelledBooking = probationBooking().withMainCourtPrisonAppointment().cancel(PROBATION_USER)

    withBookingFixture(2, cancelledBooking)
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    val error = assertThrows<IllegalArgumentException> { service.amend(2, amendRequest, PROBATION_USER) }

    error.message isEqualTo "Video booking 2 is already cancelled, and so cannot be amended"
  }

  @Test
  fun `should fail to amend a video booking which has already started`() {
    val prisonerNumber = "123456"
    val amendRequest = amendProbationBookingRequest()

    val booking = probationBooking().withProbationPrisonAppointment(date = yesterday())

    withBookingFixture(2, booking)
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    val error = assertThrows<IllegalArgumentException> { service.amend(2, amendRequest, PROBATION_USER) }

    error.message isEqualTo "Video booking 2 has already started, and so cannot be amended"
  }

  @Test
  fun `should fail to amend a probation video booking when user is court user`() {
    withBookingFixture(2, probationBooking())

    assertThrows<VideoBookingAccessException> { service.amend(2, amendProbationBookingRequest(), COURT_USER) }
  }

  private fun withBookingFixture(bookingId: Long, booking: VideoBooking) {
    whenever(videoBookingRepository.findById(bookingId)) doReturn Optional.of(booking)
    whenever(videoBookingRepository.saveAndFlush(booking)) doReturn booking
  }

  private fun withPrisonPrisonerFixture(prisonCode: String, prisonerNumber: String) {
    whenever(prisonRepository.findByCode(prisonCode)) doReturn prison(prisonCode)
    whenever(prisonerSearchClient.getPrisoner(prisonerNumber)) doReturn prisonerSearchPrisoner(prisonerNumber, prisonCode)
  }
}
