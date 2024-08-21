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
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationValidator
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerValidator
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toMinutePrecision
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ProbationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.CHESTERFIELD_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.EXTERNAL_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WERRINGTON
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendCourtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendProbationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.werringtonLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.Appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.VideoBookingAccessException
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

class AmendVideoBookingServiceTest {

  private val courtRepository: CourtRepository = mock()
  private val probationTeamRepository: ProbationTeamRepository = mock()
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val prisonRepository: PrisonRepository = mock()
  private val bookingHistoryService: BookingHistoryService = mock()

  private val persistedVideoBooking: VideoBooking = mock()
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val locationValidator: LocationValidator = mock()
  private val prisonerValidator: PrisonerValidator = mock()

  private val appointmentsService = AppointmentsService(prisonAppointmentRepository, locationValidator)

  private val service = AmendVideoBookingService(
    courtRepository,
    probationTeamRepository,
    videoBookingRepository,
    prisonRepository,
    appointmentsService,
    bookingHistoryService,
    prisonerValidator,
  )

  private var amendedBookingCaptor = argumentCaptor<VideoBooking>()

  @Test
  fun `should amend an existing court video booking for external user`() {
    val prisonerNumber = "123456"
    val courtBooking = courtBooking(court = court(DERBY_JUSTICE_CENTRE)).withMainCourtPrisonAppointment()
    val amendCourtBookingRequest = amendCourtBookingRequest(
      courtCode = CHESTERFIELD_JUSTICE_CENTRE,
      prisonCode = BIRMINGHAM,
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

    withBookingFixture(1, courtBooking)
    withCourtFixture(court(amendCourtBookingRequest.courtCode!!))
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    val (booking, prisoner) = service.amend(1, amendCourtBookingRequest, EXTERNAL_USER)

    booking isEqualTo persistedVideoBooking
    prisoner isEqualTo prisoner(prisonerNumber, BIRMINGHAM)

    verify(bookingHistoryService).createBookingHistory(HistoryType.AMEND, courtBooking)
    verify(videoBookingRepository).saveAndFlush(amendedBookingCaptor.capture())

    with(amendedBookingCaptor.firstValue) {
      bookingType isEqualTo "COURT"
      court isEqualTo court(CHESTERFIELD_JUSTICE_CENTRE)
      hearingType isEqualTo amendCourtBookingRequest.courtHearingType?.name
      comments isEqualTo "court booking comments"
      videoUrl isEqualTo amendCourtBookingRequest.videoLinkUrl
      amendedBy isEqualTo EXTERNAL_USER.username
      amendedTime isCloseTo LocalDateTime.now()

      appointments() hasSize 3

      with(appointments()) {
        assertThat(this).extracting("prisonCode").containsOnly(BIRMINGHAM)
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
  fun `should fail if booking is not found for external user`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.empty()

    val error = assertThrows<EntityNotFoundException> { service.amend(1, amendCourtBookingRequest(), EXTERNAL_USER) }

    error.message isEqualTo "Video booking with ID 1 not found."
  }

  @Test
  fun `should fail if the requested booking type does not match the booking type on the existing booking for external user`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(probationBooking().withProbationPrisonAppointment())

    val error = assertThrows<IllegalArgumentException> { service.amend(1, amendCourtBookingRequest(), EXTERNAL_USER) }

    error.message isEqualTo "The booking type PROBATION does not match the requested type COURT."
  }

  @Test
  fun `should fail to amend a court video booking when too many appointments for external user`() {
    val prisonerNumber = "123456"
    val amendCourtBookingRequest = amendCourtBookingRequest(
      prisonCode = BIRMINGHAM,
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

    withBookingFixture(1, courtBooking().withMainCourtPrisonAppointment())
    withCourtFixture(court(amendCourtBookingRequest.courtCode!!))
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    val error = assertThrows<IllegalArgumentException> { service.amend(1, amendCourtBookingRequest, EXTERNAL_USER) }

    error.message isEqualTo "Court bookings can only have one pre-conference, one hearing and one post-conference."
  }

  @Test
  fun `should fail to amend a court video booking when pre-hearing overlaps hearing for external user`() {
    val prisonerNumber = "678910"
    val amendCourtBookingRequest = amendCourtBookingRequest(
      prisonCode = WERRINGTON,
      prisonerNumber = prisonerNumber,
      appointments = listOf(
        Appointment(
          type = AppointmentType.VLB_COURT_PRE,
          locationKey = werringtonLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(9, 31),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_MAIN,
          locationKey = werringtonLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(9, 30),
          endTime = LocalTime.of(10, 0),
        ),
      ),
    )

    withBookingFixture(1, courtBooking().withMainCourtPrisonAppointment())
    withCourtFixture(court(amendCourtBookingRequest.courtCode!!))
    withPrisonPrisonerFixture(WERRINGTON, prisonerNumber)

    val error = assertThrows<IllegalArgumentException> { service.amend(1, amendCourtBookingRequest, EXTERNAL_USER) }

    error.message isEqualTo "Requested court booking appointments must not overlap."
  }

  @Test
  fun `should fail to amend a court video booking when post-hearing overlaps hearing for external user`() {
    val prisonerNumber = "123456"
    val amendCourtBookingRequest = amendCourtBookingRequest(
      prisonCode = BIRMINGHAM,
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

    withBookingFixture(1, courtBooking().withMainCourtPrisonAppointment())
    withCourtFixture(court(amendCourtBookingRequest.courtCode!!))
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    val error = assertThrows<IllegalArgumentException> { service.amend(1, amendCourtBookingRequest, EXTERNAL_USER) }

    error.message isEqualTo "Requested court booking appointments must not overlap."
  }

  @Test
  fun `should fail to amend a court video booking when no hearing appointment for external user`() {
    val prisonerNumber = "123456"
    val amendCourtBookingRequest = amendCourtBookingRequest(
      prisonCode = BIRMINGHAM,
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
          type = AppointmentType.VLB_COURT_POST,
          locationKey = birminghamLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(10, 30),
        ),
      ),
    )

    withBookingFixture(1, courtBooking().withMainCourtPrisonAppointment())
    withCourtFixture(court(amendCourtBookingRequest.courtCode!!))
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    val error = assertThrows<IllegalArgumentException> { service.amend(1, amendCourtBookingRequest, EXTERNAL_USER) }

    error.message isEqualTo "Court bookings can only have one pre-conference, one hearing and one post-conference."
  }

  @Test
  fun `should fail to amend a court video booking when too many pre-hearing appointments for external user`() {
    val prisonerNumber = "123456"
    val amendCourtBookingRequest = amendCourtBookingRequest(
      prisonCode = BIRMINGHAM,
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

    withBookingFixture(1, courtBooking().withMainCourtPrisonAppointment())
    withCourtFixture(court(amendCourtBookingRequest.courtCode!!))
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    val error = assertThrows<IllegalArgumentException> { service.amend(1, amendCourtBookingRequest, EXTERNAL_USER) }

    error.message isEqualTo "Court bookings can only have one pre-conference, one hearing and one post-conference."
  }

  @Test
  fun `should fail to amend a court video booking when too many post-hearing appointments for external user`() {
    val prisonerNumber = "123456"
    val amendCourtBookingRequest = amendCourtBookingRequest(
      prisonCode = BIRMINGHAM,
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

    withBookingFixture(1, courtBooking().withMainCourtPrisonAppointment())
    withCourtFixture(court(amendCourtBookingRequest.courtCode!!))
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    val error = assertThrows<IllegalArgumentException> { service.amend(1, amendCourtBookingRequest, EXTERNAL_USER) }

    error.message isEqualTo "Court bookings can only have one pre-conference, one hearing and one post-conference."
  }

  @Test
  fun `should fail to amend a court video booking when wrong appointment type for external user`() {
    val prisonerNumber = "123456"
    val amendCourtBookingRequest = amendCourtBookingRequest(
      prisonCode = BIRMINGHAM,
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

    withBookingFixture(1, courtBooking().withMainCourtPrisonAppointment())
    withCourtFixture(court(amendCourtBookingRequest.courtCode!!))
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    val error = assertThrows<IllegalArgumentException> { service.amend(1, amendCourtBookingRequest, EXTERNAL_USER) }

    error.message isEqualTo "Court bookings can only have one pre-conference, one hearing and one post-conference."
  }

  @Test
  fun `should fail to amend a court video booking when prison user changes the court for prison user`() {
    val courtBooking = courtBooking(court = court(DERBY_JUSTICE_CENTRE)).withMainCourtPrisonAppointment()
    val prisonerNumber = "123456"
    val amendCourtBookingRequest = amendCourtBookingRequest(
      courtCode = CHESTERFIELD_JUSTICE_CENTRE,
      prisonCode = BIRMINGHAM,
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

    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(courtBooking)
    withCourtFixture(court(amendCourtBookingRequest.courtCode!!))

    val error = assertThrows<IllegalArgumentException> { service.amend(1, amendCourtBookingRequest, PRISON_USER.copy(activeCaseLoadId = courtBooking.prisonCode())) }

    error.message isEqualTo "Prison users cannot change the court on a booking."

    verify(videoBookingRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `should fail to amend a Birmingham prison court video booking for Risley prison user`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(courtBooking().withMainCourtPrisonAppointment())

    assertThrows<CaseloadAccessException> { service.amend(1, mock<AmendVideoBookingRequest>(), PRISON_USER.copy(activeCaseLoadId = RISLEY)) }
  }

  @Test
  fun `should fail to amend a court video booking when new appointment overlaps existing for external user`() {
    val prisonerNumber = "123456"
    val amendCourtBookingRequest = amendCourtBookingRequest(
      prisonCode = BIRMINGHAM,
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

    withBookingFixture(1, courtBooking().withMainCourtPrisonAppointment())
    withCourtFixture(court(amendCourtBookingRequest.courtCode!!))
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)
    whenever(prisonAppointmentRepository.findActivePrisonAppointmentsAtLocationOnDate(BIRMINGHAM, birminghamLocation.key, tomorrow())) doReturn listOf(overlappingAppointment)

    val error = assertThrows<IllegalArgumentException> { service.amend(1, amendCourtBookingRequest, EXTERNAL_USER) }

    error.message isEqualTo "One or more requested court appointments overlaps with an existing appointment at location ${birminghamLocation.key}"
  }

  @Test
  fun `should succeed to amend a court video booking when new appointment overlaps existing for prison user`() {
    val prisonerNumber = "123456"
    val amendCourtBookingRequest = amendCourtBookingRequest(
      prisonCode = BIRMINGHAM,
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

    withBookingFixture(1, courtBooking().withMainCourtPrisonAppointment())
    withCourtFixture(court(amendCourtBookingRequest.courtCode!!))
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)
    whenever(prisonAppointmentRepository.findActivePrisonAppointmentsAtLocationOnDate(BIRMINGHAM, birminghamLocation.key, tomorrow())) doReturn listOf(overlappingAppointment)

    assertDoesNotThrow { service.amend(1, amendCourtBookingRequest, PRISON_USER.copy(activeCaseLoadId = BIRMINGHAM)) }
  }

  @Test
  fun `should fail to amend a court video booking when court not enabled for external user`() {
    val amendCourtBookingRequest = amendCourtBookingRequest()
    val disabledCourt = court(amendCourtBookingRequest.courtCode!!, enabled = false)

    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(courtBooking().withMainCourtPrisonAppointment())
    withCourtFixture(court(amendCourtBookingRequest.courtCode!!, enabled = false))

    val error = assertThrows<IllegalArgumentException> { service.amend(1, amendCourtBookingRequest, EXTERNAL_USER) }

    error.message isEqualTo "Court with code ${amendCourtBookingRequest.courtCode} is not enabled"
  }

  @Test
  fun `should fail to amend a court video booking when prison not found for external user`() {
    val amendCourtBookingRequest = amendCourtBookingRequest(prisonCode = MOORLAND)

    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(courtBooking().withMainCourtPrisonAppointment())
    withCourtFixture(court(amendCourtBookingRequest.courtCode!!))
    whenever(prisonRepository.findByCode(MOORLAND)) doReturn null

    val error = assertThrows<EntityNotFoundException> { service.amend(1, amendCourtBookingRequest, EXTERNAL_USER) }

    error.message isEqualTo "Prison with code $MOORLAND not found"
  }

  @Test
  fun `should fail to amend a court video booking when court not found for external user`() {
    val amendCourtBookingRequest = amendCourtBookingRequest()

    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(courtBooking().withMainCourtPrisonAppointment())
    whenever(courtRepository.findByCode(amendCourtBookingRequest.courtCode!!)) doReturn null

    val error = assertThrows<EntityNotFoundException> { service.amend(1, amendCourtBookingRequest, EXTERNAL_USER) }

    error.message isEqualTo "Court with code ${amendCourtBookingRequest.courtCode} not found"
  }

  @Test
  fun `should amend a probation video booking`() {
    val probationBooking = probationBooking().withProbationPrisonAppointment()
    val prisonerNumber = "123456"
    val probationBookingRequest = amendProbationBookingRequest(prisonCode = BIRMINGHAM, prisonerNumber = prisonerNumber, location = birminghamLocation)
    val requestedProbationTeam = probationTeam(probationBookingRequest.probationTeamCode!!)

    withBookingFixture(2, probationBooking)
    withProbationTeamFixture(requestedProbationTeam)
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    val (booking, prisoner) = service.amend(2, probationBookingRequest, EXTERNAL_USER)

    booking isEqualTo persistedVideoBooking
    prisoner isEqualTo prisoner(prisonerNumber, BIRMINGHAM)

    verify(bookingHistoryService).createBookingHistory(HistoryType.AMEND, probationBooking)
    verify(videoBookingRepository).saveAndFlush(amendedBookingCaptor.capture())

    with(amendedBookingCaptor.firstValue) {
      bookingType isEqualTo "PROBATION"
      probationTeam isEqualTo requestedProbationTeam
      probationMeetingType isEqualTo probationBookingRequest.probationMeetingType?.name
      comments isEqualTo "probation booking comments"
      videoUrl isEqualTo probationBookingRequest.videoLinkUrl
      amendedBy isEqualTo EXTERNAL_USER.username
      amendedTime isCloseTo LocalDateTime.now()

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
  fun `should fail to amend a probation video booking when new appointment overlaps existing for external user`() {
    val prisonerNumber = "123456"
    val probationBookingRequest = amendProbationBookingRequest(
      prisonCode = BIRMINGHAM,
      prisonerNumber = prisonerNumber,
      startTime = LocalTime.of(8, 30),
      endTime = LocalTime.of(9, 30),
      locationSuffix = "B-2-001",
    )

    val overlappingAppointment: PrisonAppointment = mock {
      on { startTime } doReturn LocalTime.of(9, 0)
      on { endTime } doReturn LocalTime.of(10, 0)
    }

    withBookingFixture(2, probationBooking().withProbationPrisonAppointment())
    withProbationTeamFixture(probationTeam(probationBookingRequest.probationTeamCode!!))
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)
    whenever(prisonAppointmentRepository.findActivePrisonAppointmentsAtLocationOnDate(BIRMINGHAM, "$BIRMINGHAM-B-2-001", tomorrow())) doReturn listOf(overlappingAppointment)

    val error = assertThrows<IllegalArgumentException> { service.amend(2, probationBookingRequest, EXTERNAL_USER) }

    error.message isEqualTo "Requested probation appointment overlaps with an existing appointment at location $BIRMINGHAM-B-2-001"
  }

  @Test
  fun `should succeed to amend a probation video booking when new appointment overlaps existing for prison user`() {
    val probationBooking = probationBooking().withProbationPrisonAppointment()
    val prisonerNumber = "123456"
    val probationBookingRequest = amendProbationBookingRequest(
      prisonCode = BIRMINGHAM,
      prisonerNumber = prisonerNumber,
      startTime = LocalTime.of(8, 30),
      endTime = LocalTime.of(9, 30),
      locationSuffix = "B-2-001",
    )

    val overlappingAppointment: PrisonAppointment = mock {
      on { startTime } doReturn LocalTime.of(9, 0)
      on { endTime } doReturn LocalTime.of(10, 0)
    }

    withBookingFixture(2, probationBooking)
    withProbationTeamFixture(probationTeam(probationBookingRequest.probationTeamCode!!))
    whenever(prisonAppointmentRepository.findActivePrisonAppointmentsAtLocationOnDate(BIRMINGHAM, "$BIRMINGHAM-B-2-001", tomorrow())) doReturn listOf(overlappingAppointment)
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    assertDoesNotThrow {
      service.amend(2, probationBookingRequest, PRISON_USER.copy(activeCaseLoadId = probationBooking.prisonCode()))
    }
  }

  @Test
  fun `should fail to amend a probation video booking when team not found for external user`() {
    val probationBookingRequest = amendProbationBookingRequest()

    withBookingFixture(2, probationBooking().withProbationPrisonAppointment())
    whenever(probationTeamRepository.findByCode(probationBookingRequest.probationTeamCode!!)) doReturn null

    val error = assertThrows<EntityNotFoundException> { service.amend(2, probationBookingRequest, EXTERNAL_USER) }

    error.message isEqualTo "Probation team with code ${probationBookingRequest.probationTeamCode} not found"
  }

  @Test
  fun `should fail to amend a probation video booking when prison not found for external user`() {
    val probationBookingRequest = amendProbationBookingRequest(prisonCode = BIRMINGHAM)

    withBookingFixture(2, probationBooking().withProbationPrisonAppointment())
    withProbationTeamFixture(probationTeam(probationBookingRequest.probationTeamCode!!))
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn null

    val error = assertThrows<EntityNotFoundException> { service.amend(2, probationBookingRequest, EXTERNAL_USER) }

    error.message isEqualTo "Prison with code $BIRMINGHAM not found"
  }

  @Test
  fun `should fail to amend a probation video booking when team not enabled for external user`() {
    val probationBookingRequest = amendProbationBookingRequest()
    val disabledProbationTeam = probationTeam(probationBookingRequest.probationTeamCode!!, false)

    withBookingFixture(2, probationBooking().withProbationPrisonAppointment())
    withProbationTeamFixture(disabledProbationTeam)

    val error = assertThrows<IllegalArgumentException> { service.amend(2, probationBookingRequest, EXTERNAL_USER) }

    error.message isEqualTo "Probation team with code ${probationBookingRequest.probationTeamCode} is not enabled"
  }

  @Test
  fun `should fail to amend a probation video booking when appointment type not probation specific for external user`() {
    val prisonerNumber = "123456"
    val amendRequest = amendProbationBookingRequest(prisonCode = BIRMINGHAM, prisonerNumber = prisonerNumber, appointmentType = AppointmentType.VLB_COURT_MAIN)

    withBookingFixture(1, probationBooking().withProbationPrisonAppointment())
    withProbationTeamFixture(probationTeam(amendRequest.probationTeamCode!!))
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    val error = assertThrows<IllegalArgumentException> { service.amend(1, amendRequest, EXTERNAL_USER) }

    error.message isEqualTo "Appointment type VLB_COURT_MAIN is not valid for probation appointments"
  }

  @Test
  fun `should fail to amend a court video booking when user is probation user`() {
    withBookingFixture(2, courtBooking())

    assertThrows<VideoBookingAccessException> { service.amend(2, amendCourtBookingRequest(), PROBATION_USER) }
  }

  @Test
  fun `should fail to amend a probation video booking when user is court user`() {
    withBookingFixture(2, probationBooking())

    assertThrows<VideoBookingAccessException> { service.amend(2, amendProbationBookingRequest(), COURT_USER) }
  }

  private fun withBookingFixture(bookingId: Long, booking: VideoBooking) {
    whenever(videoBookingRepository.findById(bookingId)) doReturn Optional.of(booking)
    whenever(videoBookingRepository.saveAndFlush(booking)) doReturn persistedVideoBooking
  }

  private fun withProbationTeamFixture(team: ProbationTeam) {
    whenever(probationTeamRepository.findByCode(team.code)) doReturn team
  }

  private fun withCourtFixture(court: Court) {
    whenever(courtRepository.findByCode(court.code)) doReturn court
  }

  private fun withPrisonPrisonerFixture(prisonCode: String, prisonerNumber: String) {
    whenever(prisonRepository.findByCode(prisonCode)) doReturn prison(prisonCode)
    whenever(prisonerValidator.validatePrisonerAtPrison(prisonerNumber, prisonCode)) doReturn prisonerSearchPrisoner(prisonerNumber, prisonCode)
  }
}
