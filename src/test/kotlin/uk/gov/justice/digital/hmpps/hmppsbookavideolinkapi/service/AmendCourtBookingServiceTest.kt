package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationValidator
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Toggles
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendCourtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendProbationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasAmendedBy
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasAmendedTimeCloseTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasAppointmentDate
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasAppointmentTypeMain
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasBookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasComments
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasEndTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasPrisonCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasPrisonersNotes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasStaffNotes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasStartTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasVideoUrl
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvilleLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.Appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.VideoBookingAccessException
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

class AmendCourtBookingServiceTest {

  private val videoBookingRepository: VideoBookingRepository = mock()
  private val prisonRepository: PrisonRepository = mock()
  private val bookingHistoryService: BookingHistoryService = mock()

  private val persistedVideoBooking: VideoBooking = mock()
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient = mock()
  private val locationValidator: LocationValidator = mock()
  private val prisonerSearchClient: PrisonerSearchClient = mock()
  private val toggles: Toggles = mock()

  private val appointmentsService = AppointmentsService(prisonAppointmentRepository, prisonRepository, locationsInsidePrisonClient, locationValidator)

  private val service = AmendCourtBookingService(
    videoBookingRepository,
    prisonRepository,
    appointmentsService,
    bookingHistoryService,
    prisonerSearchClient,
    toggles,
  )

  private var amendedBookingCaptor = argumentCaptor<VideoBooking>()

  @Test
  fun `should amend an existing court video booking for court user`() {
    val prisonerNumber = "123456"
    val courtBooking = courtBooking(court = court(DERBY_JUSTICE_CENTRE)).withMainCourtPrisonAppointment()
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
      ),
    )

    withBookingFixture(1, courtBooking)
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    whenever(locationValidator.validatePrisonLocations(BIRMINGHAM, setOf(birminghamLocation.key))) doReturn listOf(birminghamLocation)
    whenever(locationsInsidePrisonClient.getLocationsByKeys(setOf(birminghamLocation.key))) doReturn listOf(birminghamLocation)

    val (booking, prisoner) = service.amend(1, amendCourtBookingRequest, COURT_USER)

    booking isEqualTo persistedVideoBooking
    prisoner isEqualTo prisoner(prisonerNumber, BIRMINGHAM)

    verify(bookingHistoryService).createBookingHistory(HistoryType.AMEND, courtBooking)
    verify(videoBookingRepository).saveAndFlush(amendedBookingCaptor.capture())

    with(amendedBookingCaptor.firstValue) {
      bookingType isEqualTo BookingType.COURT
      hearingType isEqualTo amendCourtBookingRequest.courtHearingType?.name
      comments isEqualTo "court booking comments"
      videoUrl isEqualTo amendCourtBookingRequest.videoLinkUrl
      amendedBy isEqualTo COURT_USER.username
      amendedTime isCloseTo LocalDateTime.now()

      appointments() hasSize 3

      with(appointments()) {
        assertThat(this).extracting("prison").extracting("code").containsOnly(BIRMINGHAM)
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
    verify(prisonerSearchClient).getPrisoner(prisonerNumber)
    verify(bookingHistoryService).createBookingHistory(any(), any())
  }

  @Test
  fun `should fail if booking is not found for court user`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.empty()

    val error = assertThrows<EntityNotFoundException> { service.amend(1, amendCourtBookingRequest(), COURT_USER) }

    error.message isEqualTo "Video booking with ID 1 not found."
  }

  @Test
  fun `should fail to amend a court video booking when too many appointments for court user`() {
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

    val courtBooking = courtBooking().withMainCourtPrisonAppointment()
    withBookingFixture(1, courtBooking)
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    val error = assertThrows<IllegalArgumentException> { service.amend(1, amendCourtBookingRequest, COURT_USER) }

    error.message isEqualTo "Court bookings can only have one pre hearing, one hearing and one post hearing."
  }

  @Test
  fun `should fail to amend a court video booking when pre-hearing overlaps hearing for court user`() {
    val prisonerNumber = "678910"
    val amendCourtBookingRequest = amendCourtBookingRequest(
      prisonCode = PENTONVILLE,
      prisonerNumber = prisonerNumber,
      appointments = listOf(
        Appointment(
          type = AppointmentType.VLB_COURT_PRE,
          locationKey = pentonvilleLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(9, 31),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_MAIN,
          locationKey = pentonvilleLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(9, 30),
          endTime = LocalTime.of(10, 0),
        ),
      ),
    )

    val courtBooking = courtBooking().withMainCourtPrisonAppointment()
    withBookingFixture(1, courtBooking)
    withPrisonPrisonerFixture(PENTONVILLE, prisonerNumber)

    val error = assertThrows<IllegalArgumentException> { service.amend(1, amendCourtBookingRequest, COURT_USER) }

    error.message isEqualTo "Requested court booking appointments must not overlap."
  }

  @Test
  fun `should fail to amend a court video booking when post-hearing overlaps hearing for court user`() {
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

    val courtBooking = courtBooking().withMainCourtPrisonAppointment()
    withBookingFixture(1, courtBooking)
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    val error = assertThrows<IllegalArgumentException> { service.amend(1, amendCourtBookingRequest, COURT_USER) }

    error.message isEqualTo "Requested court booking appointments must not overlap."
  }

  @Test
  fun `should fail to amend a court video booking when no hearing appointment for court user`() {
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

    val courtBooking = courtBooking().withMainCourtPrisonAppointment()
    withBookingFixture(1, courtBooking)
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    val error = assertThrows<IllegalArgumentException> { service.amend(1, amendCourtBookingRequest, COURT_USER) }

    error.message isEqualTo "Court bookings can only have one pre hearing, one hearing and one post hearing."
  }

  @Test
  fun `should fail to amend a court video booking when too many pre-hearing appointments for court user`() {
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

    val courtBooking = courtBooking().withMainCourtPrisonAppointment()
    withBookingFixture(1, courtBooking)
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    val error = assertThrows<IllegalArgumentException> { service.amend(1, amendCourtBookingRequest, COURT_USER) }

    error.message isEqualTo "Court bookings can only have one pre hearing, one hearing and one post hearing."
  }

  @Test
  fun `should fail to amend a court video booking when too many post-hearing appointments for court user`() {
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

    val courtBooking = courtBooking().withMainCourtPrisonAppointment()
    withBookingFixture(1, courtBooking)
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    val error = assertThrows<IllegalArgumentException> { service.amend(1, amendCourtBookingRequest, COURT_USER) }

    error.message isEqualTo "Court bookings can only have one pre hearing, one hearing and one post hearing."
  }

  @Test
  fun `should fail to amend a court video booking when wrong appointment type for court user`() {
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

    val courtBooking = courtBooking().withMainCourtPrisonAppointment()
    withBookingFixture(1, courtBooking)
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    val error = assertThrows<IllegalArgumentException> { service.amend(1, amendCourtBookingRequest, COURT_USER) }

    error.message isEqualTo "Court bookings can only have one pre hearing, one hearing and one post hearing."
  }

  @Test
  fun `should fail to amend a Birmingham prison court video booking for Risley prison user`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(courtBooking().withMainCourtPrisonAppointment())

    val booking = mock<AmendVideoBookingRequest>()
    whenever(booking.bookingType) doReturn uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType.COURT

    assertThrows<CaseloadAccessException> { service.amend(1, booking, PRISON_USER_RISLEY) }
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

    val courtBooking = courtBooking().withMainCourtPrisonAppointment()
    withBookingFixture(1, courtBooking)
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)
    whenever(locationsInsidePrisonClient.getLocationByKey(birminghamLocation.key)) doReturn birminghamLocation
    whenever(locationsInsidePrisonClient.getLocationsByKeys(setOf(birminghamLocation.key))) doReturn listOf(birminghamLocation)
    whenever(prisonAppointmentRepository.findActivePrisonAppointmentsAtLocationOnDate(BIRMINGHAM, birminghamLocation.id, tomorrow())) doReturn listOf(overlappingAppointment)

    assertDoesNotThrow { service.amend(1, amendCourtBookingRequest, PRISON_USER_BIRMINGHAM) }
  }

  @Test
  fun `should fail to amend a court video booking when prison not found for court user`() {
    val amendCourtBookingRequest = amendCourtBookingRequest(prisonCode = WANDSWORTH)
    val courtBooking = courtBooking().withMainCourtPrisonAppointment()
    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(courtBooking)
    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn null

    val error = assertThrows<EntityNotFoundException> { service.amend(1, amendCourtBookingRequest, COURT_USER) }

    error.message isEqualTo "Prison with code $WANDSWORTH not found"
  }

  @Test
  fun `should fail to amend a cancelled video booking`() {
    val prisonerNumber = "123456"
    val amendRequest = amendCourtBookingRequest()

    val cancelledBooking = courtBooking().withMainCourtPrisonAppointment().cancel(COURT_USER)

    withBookingFixture(2, cancelledBooking)
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    val error = assertThrows<IllegalArgumentException> { service.amend(2, amendRequest, COURT_USER) }

    error.message isEqualTo "Video booking 2 is already cancelled, and so cannot be amended"
  }

  @Test
  fun `should fail to amend a video booking which has already started`() {
    val prisonerNumber = "123456"
    val amendRequest = amendCourtBookingRequest()

    val booking = courtBooking().withMainCourtPrisonAppointment(date = yesterday())

    withBookingFixture(2, booking)
    withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

    val error = assertThrows<IllegalArgumentException> { service.amend(2, amendRequest, COURT_USER) }

    error.message isEqualTo "Video booking 2 has already started, and so cannot be amended"
  }

  @Test
  fun `should fail to amend a court video booking when user is probation user`() {
    withBookingFixture(2, courtBooking())

    assertThrows<VideoBookingAccessException> { service.amend(2, amendCourtBookingRequest(), PROBATION_USER) }
  }

  @Test
  fun `should fail to amend when not a court booking`() {
    val error = assertThrows<IllegalArgumentException> { service.amend(1, amendProbationBookingRequest(), COURT_USER) }

    error.message isEqualTo "AMEND COURT BOOKING: booking type is not court"
  }

  @Nested
  inner class FeatureToggleForNotesComments {
    @Test
    fun `should amend existing comments (and not notes) when toggle off`() {
      val prisonerNumber = "123456"
      val courtBooking = courtBooking(court = court(DERBY_JUSTICE_CENTRE), comments = "create comments").withMainCourtPrisonAppointment()
      val amendCourtBookingRequest = amendCourtBookingRequest(
        prisonCode = BIRMINGHAM,
        prisonerNumber = prisonerNumber,
        comments = "amend comments",
        notesForStaff = "amend notes for staff",
        notesForPrisoners = "amend notes for prisoners",
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

      withBookingFixture(1, courtBooking)
      withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

      whenever(toggles.isMasterPublicAndPrivateNotes()) doReturn false
      whenever(locationValidator.validatePrisonLocations(BIRMINGHAM, setOf(birminghamLocation.key))) doReturn listOf(birminghamLocation)
      whenever(locationsInsidePrisonClient.getLocationsByKeys(setOf(birminghamLocation.key))) doReturn listOf(birminghamLocation)

      service.amend(1, amendCourtBookingRequest, COURT_USER).also { (booking, prisoner) ->
        booking isEqualTo persistedVideoBooking
        prisoner isEqualTo prisoner(prisonerNumber, BIRMINGHAM)
      }

      inOrder(videoBookingRepository, prisonRepository, prisonerSearchClient, locationValidator, bookingHistoryService) {
        verify(videoBookingRepository).findById(any())
        verify(prisonRepository).findByCode(BIRMINGHAM)
        verify(prisonerSearchClient).getPrisoner(prisonerNumber)
        verify(locationValidator).validatePrisonLocations(BIRMINGHAM, setOf(birminghamLocation.key))
        verify(videoBookingRepository).saveAndFlush(amendedBookingCaptor.capture())
        verify(bookingHistoryService).createBookingHistory(HistoryType.AMEND, courtBooking)
      }

      amendedBookingCaptor
        .firstValue
        .hasBookingType(BookingType.COURT)
        .hasHearingType(amendCourtBookingRequest.courtHearingType!!)
        .hasVideoUrl(amendCourtBookingRequest.videoLinkUrl!!)
        .hasComments("amend comments")
        .hasStaffNotes(null)
        .hasPrisonersNotes(null)
        .hasAmendedBy(COURT_USER)
        .hasAmendedTimeCloseTo(LocalDateTime.now())
        .mainHearing()!!
        .hasPrisonCode(BIRMINGHAM)
        .hasPrisonerNumber(prisonerNumber)
        .hasLocation(birminghamLocation)
        .hasAppointmentDate(tomorrow())
        .hasStartTime(LocalTime.of(9, 30))
        .hasEndTime(LocalTime.of(10, 0))
        .hasAppointmentTypeMain()
    }

    @Test
    fun `should amend existing notes (and not comments) when toggle is on`() {
      val prisonerNumber = "123456"
      val courtBooking = courtBooking(court = court(DERBY_JUSTICE_CENTRE), comments = "create comments").withMainCourtPrisonAppointment()
      val amendCourtBookingRequest = amendCourtBookingRequest(
        prisonCode = BIRMINGHAM,
        prisonerNumber = prisonerNumber,
        comments = "amend comments",
        notesForStaff = "some amend notes for staff",
        notesForPrisoners = "some amend notes for prisoners",
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

      withBookingFixture(1, courtBooking)
      withPrisonPrisonerFixture(BIRMINGHAM, prisonerNumber)

      whenever(toggles.isMasterPublicAndPrivateNotes()) doReturn true
      whenever(locationValidator.validatePrisonLocations(BIRMINGHAM, setOf(birminghamLocation.key))) doReturn listOf(birminghamLocation)
      whenever(locationsInsidePrisonClient.getLocationsByKeys(setOf(birminghamLocation.key))) doReturn listOf(birminghamLocation)
      whenever(locationsInsidePrisonClient.getLocationByKey(birminghamLocation.key)) doReturn birminghamLocation

      service.amend(1, amendCourtBookingRequest, PRISON_USER_BIRMINGHAM).also { (booking, prisoner) ->
        booking isEqualTo persistedVideoBooking
        prisoner isEqualTo prisoner(prisonerNumber, BIRMINGHAM)
      }

      inOrder(videoBookingRepository, prisonRepository, prisonerSearchClient, locationValidator, bookingHistoryService) {
        verify(videoBookingRepository).findById(any())
        verify(prisonRepository).findByCode(BIRMINGHAM)
        verify(prisonerSearchClient).getPrisoner(prisonerNumber)
        verify(locationValidator).validatePrisonLocations(BIRMINGHAM, setOf(birminghamLocation.key))
        verify(videoBookingRepository).saveAndFlush(amendedBookingCaptor.capture())
        verify(bookingHistoryService).createBookingHistory(HistoryType.AMEND, courtBooking)
      }

      amendedBookingCaptor
        .firstValue
        .hasBookingType(BookingType.COURT)
        .hasHearingType(amendCourtBookingRequest.courtHearingType!!)
        .hasVideoUrl(amendCourtBookingRequest.videoLinkUrl!!)
        // note old comments are not lost/overwritten
        .hasComments("create comments")
        .hasStaffNotes("some amend notes for staff")
        .hasPrisonersNotes("some amend notes for prisoners")
        .hasAmendedBy(PRISON_USER_BIRMINGHAM)
        .hasAmendedTimeCloseTo(LocalDateTime.now())
        .mainHearing()!!
        .hasPrisonCode(BIRMINGHAM)
        .hasPrisonerNumber(prisonerNumber)
        .hasLocation(birminghamLocation)
        .hasAppointmentDate(tomorrow())
        .hasStartTime(LocalTime.of(9, 30))
        .hasEndTime(LocalTime.of(10, 0))
        .hasAppointmentTypeMain()
    }
  }

  private fun withBookingFixture(bookingId: Long, booking: VideoBooking) {
    whenever(videoBookingRepository.findById(bookingId)) doReturn Optional.of(booking)
    whenever(videoBookingRepository.saveAndFlush(booking)) doReturn persistedVideoBooking
  }

  private fun withPrisonPrisonerFixture(prisonCode: String, prisonerNumber: String) {
    whenever(prisonRepository.findByCode(prisonCode)) doReturn prison(prisonCode)
    whenever(prisonerSearchClient.getPrisoner(prisonerNumber)) doReturn prisonerSearchPrisoner(prisonerNumber, prisonCode)
  }
}
