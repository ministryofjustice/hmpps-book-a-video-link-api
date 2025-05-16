package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.SERVICE_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.readOnlyCourt
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.Appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.CaseloadAccessException
import java.time.LocalDateTime
import java.time.LocalTime

class CreateCourtBookingServiceTest {
  private val courtRepository: CourtRepository = mock()
  private val videoBookingRepository: VideoBookingRepository = mock()

  private val bookingHistoryService: BookingHistoryService = mock()
  private val prisonRepository: PrisonRepository = mock()
  private val prisonerValidator: PrisonerValidator = mock()

  private val persistedVideoBooking: VideoBooking = mock()
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient = mock()
  private val locationValidator: LocationValidator = mock()

  private val appointmentsService = AppointmentsService(prisonAppointmentRepository, prisonRepository, locationsInsidePrisonClient, locationValidator)

  private val service = CreateCourtBookingService(
    courtRepository,
    videoBookingRepository,
    appointmentsService,
    bookingHistoryService,
    prisonRepository,
    prisonerValidator,
  )

  private var newBookingCaptor = argumentCaptor<VideoBooking>()

  @Test
  fun `should create a court video booking for court user`() {
    val prisonCode = BIRMINGHAM
    val prisonerNumber = "123456"
    val courtBookingRequest = courtBookingRequest(
      prisonCode = prisonCode,
      prisonerNumber = prisonerNumber,
      appointments = listOf(
        Appointment(
          type = AppointmentType.VLB_COURT_PRE,
          locationKey = birminghamLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(9, 30),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_MAIN,
          locationKey = birminghamLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(9, 30),
          endTime = LocalTime.of(10, 0),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_POST,
          locationKey = birminghamLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(10, 30),
        ),
      ),
    )

    val requestedCourt = court(courtBookingRequest.courtCode!!)

    whenever(courtRepository.findByCode(courtBookingRequest.courtCode!!)) doReturn requestedCourt
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn prison(BIRMINGHAM)
    whenever(videoBookingRepository.saveAndFlush(any())) doReturn persistedVideoBooking
    whenever(prisonerValidator.validatePrisonerAtPrison(prisonerNumber, prisonCode)) doReturn prisonerSearchPrisoner(prisonerNumber, prisonCode)
    whenever(locationValidator.validatePrisonLocations(BIRMINGHAM, setOf(birminghamLocation.key))) doReturn listOf(birminghamLocation)
    whenever(locationsInsidePrisonClient.getLocationsByKeys(setOf(birminghamLocation.key))) doReturn listOf(birminghamLocation)

    val (booking, prisoner) = service.create(courtBookingRequest, COURT_USER)

    booking isEqualTo persistedVideoBooking
    prisoner isEqualTo prisoner(prisonerNumber, prisonCode)

    verify(videoBookingRepository).saveAndFlush(newBookingCaptor.capture())

    with(newBookingCaptor.firstValue) {
      bookingType isEqualTo BookingType.COURT
      court isEqualTo requestedCourt
      createdTime isCloseTo LocalDateTime.now()
      hearingType isEqualTo courtBookingRequest.courtHearingType?.name
      comments isEqualTo "court booking comments"
      videoUrl isEqualTo courtBookingRequest.videoLinkUrl
      createdBy isEqualTo COURT_USER.username
      createdTime isCloseTo LocalDateTime.now()
      notesForStaff isEqualTo "Some private staff notes"
      notesForPrisoners isEqualTo null

      appointments() hasSize 3

      with(appointments()) {
        assertThat(this).extracting("prison").extracting("code").containsOnly(prisonCode)
        assertThat(this).extracting("prisonerNumber").containsOnly(prisonerNumber)
        assertThat(this).extracting("appointmentDate").containsOnly(tomorrow())
        assertThat(this).extracting("prisonLocationId").containsOnly(birminghamLocation.id)
        assertThat(this).extracting("startTime").containsAll(
          listOf(
            LocalTime.of(9, 0),
            LocalTime.of(9, 30),
            LocalTime.of(10, 0),
          ),
        )
        assertThat(this).extracting("endTime").containsAll(
          listOf(
            LocalTime.of(9, 30),
            LocalTime.of(10, 0),
            LocalTime.of(10, 30),
          ),
        )
        assertThat(this).extracting("appointmentType").containsAll(
          listOf(
            AppointmentType.VLB_COURT_PRE.name,
            AppointmentType.VLB_COURT_MAIN.name,
            AppointmentType.VLB_COURT_POST.name,
          ),
        )
      }
    }

    verify(locationValidator).validatePrisonLocations(BIRMINGHAM, setOf(birminghamLocation.key))
    verify(prisonerValidator).validatePrisonerAtPrison(prisonerNumber, BIRMINGHAM)
    verify(bookingHistoryService).createBookingHistory(any(), any())
  }

  @Test
  fun `should fail to create a Birmingham prison court video booking for Risley prison user`() {
    val courtBookingRequest = courtBookingRequest(
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      appointments = listOf(
        Appointment(
          type = AppointmentType.VLB_COURT_MAIN,
          locationKey = birminghamLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(9, 30),
          endTime = LocalTime.of(10, 0),
        ),
      ),
    )

    assertThrows<CaseloadAccessException> { service.create(courtBookingRequest, PRISON_USER_RISLEY) }
  }

  @Test
  fun `should fail to create a court video booking when too many appointments for court user`() {
    val prisonCode = BIRMINGHAM
    val prisonerNumber = "123456"
    val courtBookingRequest = courtBookingRequest(
      prisonCode = prisonCode,
      prisonerNumber = prisonerNumber,
      appointments = listOf(
        Appointment(
          type = AppointmentType.VLB_COURT_PRE,
          locationKey = birminghamLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(9, 30),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_MAIN,
          locationKey = birminghamLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(9, 30),
          endTime = LocalTime.of(10, 0),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_POST,
          locationKey = birminghamLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(10, 30),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_POST,
          locationKey = birminghamLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(10, 30),
          endTime = LocalTime.of(11, 0),
        ),
      ),
    )
    val requestedCourt = court(courtBookingRequest.courtCode!!)

    whenever(courtRepository.findByCode(courtBookingRequest.courtCode!!)) doReturn requestedCourt
    whenever(videoBookingRepository.saveAndFlush(any())) doReturn persistedVideoBooking
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn prison(BIRMINGHAM)
    whenever(prisonerValidator.validatePrisonerAtPrison(prisonerNumber, BIRMINGHAM)) doReturn prisonerSearchPrisoner(prisonerNumber, BIRMINGHAM)

    val error = assertThrows<IllegalArgumentException> { service.create(courtBookingRequest, COURT_USER) }

    error.message isEqualTo "Court bookings can only have one pre hearing, one hearing and one post hearing."
  }

  @Test
  fun `should fail to create a court video booking when pre-hearing overlaps hearing for court user`() {
    val prisonCode = BIRMINGHAM
    val prisonerNumber = "123456"
    val courtBookingRequest = courtBookingRequest(
      prisonCode = prisonCode,
      prisonerNumber = prisonerNumber,
      appointments = listOf(
        Appointment(
          type = AppointmentType.VLB_COURT_PRE,
          locationKey = birminghamLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(9, 31),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_MAIN,
          locationKey = birminghamLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(9, 30),
          endTime = LocalTime.of(10, 0),
        ),
      ),
    )
    val requestedCourt = court(courtBookingRequest.courtCode!!)

    whenever(courtRepository.findByCode(courtBookingRequest.courtCode!!)) doReturn requestedCourt
    whenever(videoBookingRepository.saveAndFlush(any())) doReturn persistedVideoBooking
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn prison(BIRMINGHAM)
    whenever(prisonerValidator.validatePrisonerAtPrison(prisonerNumber, BIRMINGHAM)) doReturn prisonerSearchPrisoner(prisonerNumber, BIRMINGHAM)

    val error = assertThrows<IllegalArgumentException> { service.create(courtBookingRequest, COURT_USER) }

    error.message isEqualTo "Requested court booking appointments must not overlap."
  }

  @Test
  fun `should fail to create a court video booking when post-hearing overlaps hearing for court user`() {
    val prisonCode = BIRMINGHAM
    val prisonerNumber = "123456"
    val courtBookingRequest = courtBookingRequest(
      prisonCode = prisonCode,
      prisonerNumber = prisonerNumber,
      appointments = listOf(
        Appointment(
          type = AppointmentType.VLB_COURT_MAIN,
          locationKey = birminghamLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(9, 31),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_POST,
          locationKey = birminghamLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(9, 30),
          endTime = LocalTime.of(10, 0),
        ),
      ),
    )
    val requestedCourt = court(courtBookingRequest.courtCode!!)

    whenever(courtRepository.findByCode(courtBookingRequest.courtCode!!)) doReturn requestedCourt
    whenever(videoBookingRepository.saveAndFlush(any())) doReturn persistedVideoBooking
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn prison(BIRMINGHAM)
    whenever(prisonerValidator.validatePrisonerAtPrison(prisonerNumber, BIRMINGHAM)) doReturn prisonerSearchPrisoner(prisonerNumber, BIRMINGHAM)

    val error = assertThrows<IllegalArgumentException> { service.create(courtBookingRequest, COURT_USER) }

    error.message isEqualTo "Requested court booking appointments must not overlap."
  }

  @Test
  fun `should fail to create a court video booking when no hearing appointment for court user`() {
    val prisonCode = WANDSWORTH
    val prisonerNumber = "123456"
    val courtBookingRequest = courtBookingRequest(
      prisonCode = prisonCode,
      prisonerNumber = prisonerNumber,
      appointments = listOf(
        Appointment(
          type = AppointmentType.VLB_COURT_PRE,
          locationKey = wandsworthLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(9, 30),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_POST,
          locationKey = wandsworthLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(10, 30),
        ),
      ),
    )
    val requestedCourt = court(courtBookingRequest.courtCode!!)

    whenever(courtRepository.findByCode(courtBookingRequest.courtCode!!)) doReturn requestedCourt
    whenever(videoBookingRepository.saveAndFlush(any())) doReturn persistedVideoBooking
    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn prison(WANDSWORTH)
    whenever(prisonerValidator.validatePrisonerAtPrison(prisonerNumber, WANDSWORTH)) doReturn prisonerSearchPrisoner(prisonerNumber, WANDSWORTH)

    val error = assertThrows<IllegalArgumentException> { service.create(courtBookingRequest, COURT_USER) }

    error.message isEqualTo "Court bookings can only have one pre hearing, one hearing and one post hearing."
  }

  @Test
  fun `should fail to create a court video booking when too many pre-hearing appointments for court user`() {
    val prisonCode = BIRMINGHAM
    val prisonerNumber = "123456"
    val courtBookingRequest = courtBookingRequest(
      prisonCode = prisonCode,
      prisonerNumber = prisonerNumber,
      appointments = listOf(
        Appointment(
          type = AppointmentType.VLB_COURT_PRE,
          locationKey = birminghamLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(9, 30),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_PRE,
          locationKey = birminghamLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(9, 30),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_MAIN,
          locationKey = birminghamLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(9, 30),
          endTime = LocalTime.of(10, 0),
        ),
      ),
    )
    val requestedCourt = court(courtBookingRequest.courtCode!!)

    whenever(courtRepository.findByCode(courtBookingRequest.courtCode!!)) doReturn requestedCourt
    whenever(videoBookingRepository.saveAndFlush(any())) doReturn persistedVideoBooking
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn prison(BIRMINGHAM)
    whenever(prisonerValidator.validatePrisonerAtPrison(prisonerNumber, BIRMINGHAM)) doReturn prisonerSearchPrisoner(prisonerNumber, BIRMINGHAM)

    val error = assertThrows<IllegalArgumentException> { service.create(courtBookingRequest, COURT_USER) }

    error.message isEqualTo "Court bookings can only have one pre hearing, one hearing and one post hearing."
  }

  @Test
  fun `should fail to create a court video booking when too many post-hearing appointments for court user`() {
    val prisonCode = BIRMINGHAM
    val prisonerNumber = "123456"
    val courtBookingRequest = courtBookingRequest(
      prisonCode = prisonCode,
      prisonerNumber = prisonerNumber,
      appointments = listOf(
        Appointment(
          type = AppointmentType.VLB_COURT_MAIN,
          locationKey = birminghamLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(9, 30),
          endTime = LocalTime.of(10, 0),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_POST,
          locationKey = birminghamLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(10, 30),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_POST,
          locationKey = birminghamLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(10, 30),
          endTime = LocalTime.of(11, 0),
        ),
      ),
    )
    val requestedCourt = court(courtBookingRequest.courtCode!!)

    whenever(courtRepository.findByCode(courtBookingRequest.courtCode!!)) doReturn requestedCourt
    whenever(videoBookingRepository.saveAndFlush(any())) doReturn persistedVideoBooking
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn prison(BIRMINGHAM)
    whenever(prisonerValidator.validatePrisonerAtPrison(prisonerNumber, BIRMINGHAM)) doReturn prisonerSearchPrisoner(prisonerNumber, BIRMINGHAM)

    val error = assertThrows<IllegalArgumentException> { service.create(courtBookingRequest, COURT_USER) }

    error.message isEqualTo "Court bookings can only have one pre hearing, one hearing and one post hearing."
  }

  @Test
  fun `should fail to create a court video booking when wrong appointment type for court user`() {
    val prisonCode = BIRMINGHAM
    val prisonerNumber = "123456"
    val courtBookingRequest = courtBookingRequest(
      prisonCode = prisonCode,
      prisonerNumber = prisonerNumber,
      appointments = listOf(
        Appointment(
          type = AppointmentType.VLB_COURT_MAIN,
          locationKey = birminghamLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(9, 30),
          endTime = LocalTime.of(10, 0),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_POST,
          locationKey = birminghamLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(10, 30),
        ),
        Appointment(
          type = AppointmentType.VLB_PROBATION,
          locationKey = birminghamLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(10, 30),
          endTime = LocalTime.of(11, 0),
        ),
      ),
    )
    val requestedCourt = court(courtBookingRequest.courtCode!!)

    whenever(courtRepository.findByCode(courtBookingRequest.courtCode!!)) doReturn requestedCourt
    whenever(videoBookingRepository.saveAndFlush(any())) doReturn persistedVideoBooking
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn prison(BIRMINGHAM)
    whenever(prisonerValidator.validatePrisonerAtPrison(prisonerNumber, BIRMINGHAM)) doReturn prisonerSearchPrisoner(prisonerNumber, BIRMINGHAM)

    val error = assertThrows<IllegalArgumentException> { service.create(courtBookingRequest, COURT_USER) }

    error.message isEqualTo "Court bookings can only have one pre hearing, one hearing and one post hearing."
  }

  @Test
  fun `should succeed to create a court video booking when new appointment overlaps existing for prison user`() {
    val prisonCode = BIRMINGHAM
    val prisonerNumber = "123456"
    val courtBookingRequest = courtBookingRequest(
      prisonCode = prisonCode,
      prisonerNumber = prisonerNumber,
      appointments = listOf(
        Appointment(
          type = AppointmentType.VLB_COURT_MAIN,
          locationKey = birminghamLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(9, 30),
          endTime = LocalTime.of(10, 0),
        ),
      ),
    )

    val overlappingAppointment: PrisonAppointment = mock {
      on { startTime } doReturn LocalTime.of(9, 0)
      on { endTime } doReturn LocalTime.of(10, 0)
    }

    val requestedCourt = court(courtBookingRequest.courtCode!!)

    whenever(courtRepository.findByCode(courtBookingRequest.courtCode!!)) doReturn requestedCourt
    whenever(videoBookingRepository.saveAndFlush(any())) doReturn persistedVideoBooking
    whenever(prisonAppointmentRepository.findActivePrisonAppointmentsAtLocationOnDate(BIRMINGHAM, birminghamLocation.id, tomorrow())) doReturn listOf(overlappingAppointment)
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn prison(BIRMINGHAM)
    whenever(prisonerValidator.validatePrisonerAtPrison(prisonerNumber, BIRMINGHAM)) doReturn prisonerSearchPrisoner(prisonerNumber, BIRMINGHAM)
    whenever(locationValidator.validatePrisonLocations(BIRMINGHAM, setOf(birminghamLocation.key))) doReturn listOf(birminghamLocation)
    whenever(locationsInsidePrisonClient.getLocationsByKeys(setOf(birminghamLocation.key))) doReturn listOf(birminghamLocation)
    whenever(locationsInsidePrisonClient.getLocationByKey(birminghamLocation.key)) doReturn birminghamLocation

    assertDoesNotThrow {
      service.create(courtBookingRequest, PRISON_USER_BIRMINGHAM)
    }
  }

  @Test
  fun `should fail to create a court video booking when court not enabled for court user`() {
    val courtBookingRequest = courtBookingRequest()
    val disabledCourt = court(courtBookingRequest.courtCode!!, enabled = false)

    whenever(courtRepository.findByCode(courtBookingRequest.courtCode!!)) doReturn disabledCourt

    val error = assertThrows<IllegalArgumentException> { service.create(courtBookingRequest, COURT_USER) }

    error.message isEqualTo "Court with code ${courtBookingRequest.courtCode} is not enabled"

    verifyNoInteractions(videoBookingRepository)
  }

  @Test
  fun `should fail to create a court video booking when court is read only`() {
    val courtBookingRequest = courtBookingRequest(courtCode = "UNKNOWN", prisonCode = BIRMINGHAM)

    whenever(courtRepository.findByCode(courtBookingRequest.courtCode!!)) doReturn readOnlyCourt()

    val error = assertThrows<IllegalArgumentException> { service.create(courtBookingRequest, PRISON_USER_BIRMINGHAM) }

    error.message isEqualTo "Court with code ${courtBookingRequest.courtCode} is read-only"

    verifyNoInteractions(videoBookingRepository)
  }

  @Test
  fun `should create a court video booking when court not enabled for prison user`() {
    val prisonCode = BIRMINGHAM
    val prisonerNumber = "123456"
    val courtBookingRequest = courtBookingRequest(
      prisonCode = prisonCode,
      prisonerNumber = prisonerNumber,
      appointments = listOf(
        Appointment(
          type = AppointmentType.VLB_COURT_MAIN,
          locationKey = birminghamLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(9, 30),
          endTime = LocalTime.of(10, 0),
        ),
      ),
    )

    val overlappingAppointment: PrisonAppointment = mock {
      on { startTime } doReturn LocalTime.of(9, 0)
      on { endTime } doReturn LocalTime.of(10, 0)
    }

    val disabledCourt = court(courtBookingRequest.courtCode!!, enabled = false)

    whenever(courtRepository.findByCode(courtBookingRequest.courtCode!!)) doReturn disabledCourt
    whenever(videoBookingRepository.saveAndFlush(any())) doReturn persistedVideoBooking
    whenever(prisonAppointmentRepository.findActivePrisonAppointmentsAtLocationOnDate(BIRMINGHAM, birminghamLocation.id, tomorrow())) doReturn listOf(overlappingAppointment)
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn prison(BIRMINGHAM)
    whenever(prisonerValidator.validatePrisonerAtPrison(prisonerNumber, BIRMINGHAM)) doReturn prisonerSearchPrisoner(prisonerNumber, BIRMINGHAM)
    whenever(locationValidator.validatePrisonLocations(BIRMINGHAM, setOf(birminghamLocation.key))) doReturn listOf(birminghamLocation)
    whenever(locationsInsidePrisonClient.getLocationsByKeys(setOf(birminghamLocation.key))) doReturn listOf(birminghamLocation)
    whenever(locationsInsidePrisonClient.getLocationByKey(birminghamLocation.key)) doReturn birminghamLocation

    assertDoesNotThrow {
      service.create(courtBookingRequest, PRISON_USER_BIRMINGHAM)
    }
  }

  @Test
  fun `should fail to create a court video booking when prison not found for court user`() {
    val courtBookingRequest = courtBookingRequest(prisonCode = WANDSWORTH)

    whenever(courtRepository.findByCode(courtBookingRequest.courtCode!!)) doReturn court(courtBookingRequest.courtCode!!)
    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn null

    val error = assertThrows<EntityNotFoundException> { service.create(courtBookingRequest, COURT_USER) }

    error.message isEqualTo "Prison with code $WANDSWORTH not found"

    verifyNoInteractions(videoBookingRepository)
  }

  @Test
  fun `should fail to create a court video booking when court not found for court user`() {
    val courtBookingRequest = courtBookingRequest()

    whenever(courtRepository.findByCode(courtBookingRequest.courtCode!!)) doReturn null

    val error = assertThrows<EntityNotFoundException> { service.create(courtBookingRequest, COURT_USER) }

    error.message isEqualTo "Court with code ${courtBookingRequest.courtCode} not found"

    verifyNoInteractions(videoBookingRepository)
  }

  @Test
  fun `should fail to create a court video booking when not court or prison user`() {
    assertThrows<IllegalArgumentException> { service.create(courtBookingRequest(), PROBATION_USER) }.message isEqualTo "Only court and prison users can create court bookings."
    assertThrows<IllegalArgumentException> { service.create(courtBookingRequest(), SERVICE_USER) }.message isEqualTo "Only court and prison users can create court bookings."

    verify(videoBookingRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `should fail to create when not a court booking`() {
    val error = assertThrows<IllegalArgumentException> { service.create(probationBookingRequest(), COURT_USER) }

    error.message isEqualTo "CREATE COURT BOOKING: booking type is not court"
  }
}
