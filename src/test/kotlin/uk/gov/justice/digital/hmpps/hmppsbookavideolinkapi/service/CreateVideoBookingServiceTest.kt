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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerValidator
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toMinutePrecision
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.RISLEY
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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.readOnlyCourt
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.Appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.CaseloadAccessException
import java.time.LocalDateTime
import java.time.LocalTime

class CreateVideoBookingServiceTest {
  private val courtRepository: CourtRepository = mock()
  private val probationTeamRepository: ProbationTeamRepository = mock()
  private val videoBookingRepository: VideoBookingRepository = mock()

  private val bookingHistoryService: BookingHistoryService = mock()
  private val prisonRepository: PrisonRepository = mock()
  private val prisonerValidator: PrisonerValidator = mock()

  private val persistedVideoBooking: VideoBooking = mock()
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val locationValidator: LocationValidator = mock()

  private val appointmentsService = AppointmentsService(prisonAppointmentRepository, prisonRepository, locationValidator)

  private val service = CreateVideoBookingService(
    courtRepository,
    probationTeamRepository,
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

    val (booking, prisoner) = service.create(courtBookingRequest, COURT_USER)

    booking isEqualTo persistedVideoBooking
    prisoner isEqualTo prisoner(prisonerNumber, prisonCode)

    verify(videoBookingRepository).saveAndFlush(newBookingCaptor.capture())

    with(newBookingCaptor.firstValue) {
      bookingType isEqualTo "COURT"
      court isEqualTo requestedCourt
      createdTime isCloseTo LocalDateTime.now()
      hearingType isEqualTo courtBookingRequest.courtHearingType?.name
      comments isEqualTo "court booking comments"
      videoUrl isEqualTo courtBookingRequest.videoLinkUrl
      createdBy isEqualTo COURT_USER.username
      createdTime isCloseTo LocalDateTime.now()

      appointments() hasSize 3

      with(appointments()) {
        assertThat(this).extracting("prison").extracting("code").containsOnly(prisonCode)
        assertThat(this).extracting("prisonerNumber").containsOnly(prisonerNumber)
        assertThat(this).extracting("appointmentDate").containsOnly(tomorrow())
        assertThat(this).extracting("prisonLocKey").containsOnly("$BIRMINGHAM-ABCEDFG")
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
  fun `should create a Birmingham prison court video booking for Risley prison user`() {
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

    assertThrows<CaseloadAccessException> { service.create(courtBookingRequest, PRISON_USER.copy(activeCaseLoadId = RISLEY)) }
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
          locationKey = "$prisonCode-A-1-001",
          date = tomorrow(),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(9, 30),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_MAIN,
          locationKey = "$prisonCode-A-1-001",
          date = tomorrow(),
          startTime = LocalTime.of(9, 30),
          endTime = LocalTime.of(10, 0),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_POST,
          locationKey = "$prisonCode-A-1-001",
          date = tomorrow(),
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(10, 30),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_POST,
          locationKey = "$prisonCode-A-1-001",
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
          locationKey = "$prisonCode-A-1-001",
          date = tomorrow(),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(9, 31),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_MAIN,
          locationKey = "$prisonCode-A-1-001",
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
          locationKey = "$prisonCode-A-1-001",
          date = tomorrow(),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(9, 31),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_POST,
          locationKey = "$prisonCode-A-1-001",
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
          locationKey = "$prisonCode-A-1-001",
          date = tomorrow(),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(9, 30),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_POST,
          locationKey = "$prisonCode-A-1-001",
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
          locationKey = "$prisonCode-A-1-001",
          date = tomorrow(),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(9, 30),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_PRE,
          locationKey = "$prisonCode-A-1-001",
          date = tomorrow(),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(9, 30),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_MAIN,
          locationKey = "$prisonCode-A-1-001",
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
          locationKey = "$prisonCode-A-1-001",
          date = tomorrow(),
          startTime = LocalTime.of(9, 30),
          endTime = LocalTime.of(10, 0),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_POST,
          locationKey = "$prisonCode-A-1-001",
          date = tomorrow(),
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(10, 30),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_POST,
          locationKey = "$prisonCode-A-1-001",
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
          locationKey = "$prisonCode-A-1-001",
          date = tomorrow(),
          startTime = LocalTime.of(9, 30),
          endTime = LocalTime.of(10, 0),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_POST,
          locationKey = "$prisonCode-A-1-001",
          date = tomorrow(),
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(10, 30),
        ),
        Appointment(
          type = AppointmentType.VLB_PROBATION,
          locationKey = "$prisonCode-A-1-001",
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
  fun `should fail to create a court video booking when new appointment overlaps existing for court user`() {
    val prisonCode = BIRMINGHAM
    val prisonerNumber = "123456"
    val courtBookingRequest = courtBookingRequest(
      prisonCode = prisonCode,
      prisonerNumber = prisonerNumber,
      appointments = listOf(
        Appointment(
          type = AppointmentType.VLB_COURT_MAIN,
          locationKey = "$prisonCode-A-1-001",
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
    whenever(prisonAppointmentRepository.findActivePrisonAppointmentsAtLocationOnDate(BIRMINGHAM, "$BIRMINGHAM-A-1-001", tomorrow())) doReturn listOf(overlappingAppointment)
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn prison(BIRMINGHAM)
    whenever(prisonerValidator.validatePrisonerAtPrison(prisonerNumber, BIRMINGHAM)) doReturn prisonerSearchPrisoner(prisonerNumber, BIRMINGHAM)

    val error = assertThrows<IllegalArgumentException> { service.create(courtBookingRequest, COURT_USER) }

    error.message isEqualTo "One or more requested court appointments overlaps with an existing appointment at location $prisonCode-A-1-001"
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
          locationKey = "$prisonCode-A-1-001",
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
    whenever(prisonAppointmentRepository.findActivePrisonAppointmentsAtLocationOnDate(BIRMINGHAM, "$BIRMINGHAM-A-1-001", tomorrow())) doReturn listOf(overlappingAppointment)
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn prison(BIRMINGHAM)
    whenever(prisonerValidator.validatePrisonerAtPrison(prisonerNumber, BIRMINGHAM)) doReturn prisonerSearchPrisoner(prisonerNumber, BIRMINGHAM)

    assertDoesNotThrow {
      service.create(courtBookingRequest, PRISON_USER.copy(activeCaseLoadId = BIRMINGHAM))
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

    val error = assertThrows<IllegalArgumentException> { service.create(courtBookingRequest, PRISON_USER.copy(activeCaseLoadId = BIRMINGHAM)) }

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
          locationKey = "$prisonCode-A-1-001",
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
    whenever(prisonAppointmentRepository.findActivePrisonAppointmentsAtLocationOnDate(BIRMINGHAM, "$BIRMINGHAM-A-1-001", tomorrow())) doReturn listOf(overlappingAppointment)
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn prison(BIRMINGHAM)
    whenever(prisonerValidator.validatePrisonerAtPrison(prisonerNumber, BIRMINGHAM)) doReturn prisonerSearchPrisoner(prisonerNumber, BIRMINGHAM)

    assertDoesNotThrow {
      service.create(courtBookingRequest, PRISON_USER.copy(activeCaseLoadId = BIRMINGHAM))
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
  fun `should create a probation video booking for probation user`() {
    val prisonCode = BIRMINGHAM
    val prisonerNumber = "123456"
    val probationBookingRequest = probationBookingRequest(prisonCode = prisonCode, prisonerNumber = prisonerNumber, location = birminghamLocation)
    val requestedProbationTeam = probationTeam(probationBookingRequest.probationTeamCode!!)

    whenever(probationTeamRepository.findByCode(probationBookingRequest.probationTeamCode!!)) doReturn requestedProbationTeam
    whenever(videoBookingRepository.saveAndFlush(any())) doReturn persistedVideoBooking
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn prison(BIRMINGHAM)
    whenever(prisonerValidator.validatePrisonerAtPrison(prisonerNumber, prisonCode)) doReturn prisonerSearchPrisoner(prisonerNumber, prisonCode)

    val (booking, prisoner) = service.create(probationBookingRequest, PROBATION_USER)

    booking isEqualTo persistedVideoBooking
    prisoner isEqualTo prisoner(prisonerNumber, prisonCode)

    verify(videoBookingRepository).saveAndFlush(newBookingCaptor.capture())

    with(newBookingCaptor.firstValue) {
      bookingType isEqualTo "PROBATION"
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
        prisonLocKey isEqualTo onePrisoner.appointments.single().locationKey!!
      }
    }

    verify(locationValidator).validatePrisonLocation(BIRMINGHAM, birminghamLocation.key)
    verify(prisonerValidator).validatePrisonerAtPrison(prisonerNumber, BIRMINGHAM)
    verify(bookingHistoryService).createBookingHistory(any(), any())
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
      locationSuffix = "B-2-001",
    )
    val requestedProbationTeam = probationTeam(probationBookingRequest.probationTeamCode!!)

    val overlappingAppointment: PrisonAppointment = mock {
      on { startTime } doReturn LocalTime.of(9, 0)
      on { endTime } doReturn LocalTime.of(10, 0)
    }

    whenever(probationTeamRepository.findByCode(probationBookingRequest.probationTeamCode!!)) doReturn requestedProbationTeam
    whenever(videoBookingRepository.saveAndFlush(any())) doReturn persistedVideoBooking
    whenever(prisonAppointmentRepository.findActivePrisonAppointmentsAtLocationOnDate(BIRMINGHAM, "$BIRMINGHAM-B-2-001", tomorrow())) doReturn listOf(overlappingAppointment)
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn prison(BIRMINGHAM)
    whenever(prisonerValidator.validatePrisonerAtPrison(prisonerNumber, BIRMINGHAM)) doReturn prisonerSearchPrisoner(prisonerNumber, BIRMINGHAM)

    val error = assertThrows<IllegalArgumentException> { service.create(probationBookingRequest, PROBATION_USER) }

    error.message isEqualTo "Requested probation appointment overlaps with an existing appointment at location $BIRMINGHAM-B-2-001"
  }

  @Test
  fun `should fail to create a probation video booking when not probation user`() {
    assertThrows<IllegalArgumentException> { service.create(probationBookingRequest(), PRISON_USER) }.message isEqualTo "Only probation users can create probation bookings."
    assertThrows<IllegalArgumentException> { service.create(probationBookingRequest(), COURT_USER) }.message isEqualTo "Only probation users can create probation bookings."
    assertThrows<IllegalArgumentException> { service.create(probationBookingRequest(), SERVICE_USER) }.message isEqualTo "Only probation users can create probation bookings."

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

    val error = assertThrows<IllegalArgumentException> { service.create(probationBookingRequest, PROBATION_USER) }

    error.message isEqualTo "Appointment type VLB_COURT_MAIN is not valid for probation appointments"
  }
}
