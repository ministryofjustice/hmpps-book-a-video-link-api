package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.argumentCaptor
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvilleLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryRepository
import java.time.LocalDateTime
import java.time.LocalTime

class BookingHistoryServiceTest {
  private val bookingHistoryRepository: BookingHistoryRepository = mock()

  private val service = BookingHistoryService(bookingHistoryRepository)

  private var historyCaptor = argumentCaptor<BookingHistory>()

  @BeforeEach
  fun setUp() {
    openMocks(this)
  }

  @Test
  fun `Should create history for a court booking`() {
    val courtBooking = courtBooking(COURT_USER.username)
      .addAppointment(
        prison = prison(prisonCode = WANDSWORTH),
        prisonerNumber = "A1234AA",
        appointmentType = AppointmentType.VLB_COURT_MAIN.name,
        date = tomorrow(),
        startTime = LocalTime.of(9, 30),
        endTime = LocalTime.of(10, 30),
        locationId = wandsworthLocation.id,
      )

    service.createBookingHistory(HistoryType.CREATE, courtBooking)

    verify(bookingHistoryRepository).saveAndFlush(historyCaptor.capture())

    with(historyCaptor.firstValue) {
      historyType isEqualTo HistoryType.CREATE
      createdTime isCloseTo LocalDateTime.now()
      hearingType isEqualTo courtBooking.hearingType
      videoUrl isEqualTo courtBooking.videoUrl
      notesForStaff isEqualTo courtBooking.notesForStaff
      notesForPrisoners isEqualTo courtBooking.notesForPrisoners
      createdBy isEqualTo COURT_USER.username
      createdTime isCloseTo LocalDateTime.now()
      appointments() hasSize 1
      with(appointments().first()) {
        prisonCode isEqualTo WANDSWORTH
        prisonerNumber isEqualTo "A1234AA"
        appointmentType isEqualTo AppointmentType.VLB_COURT_MAIN.name
        startTime isEqualTo LocalTime.of(9, 30)
        endTime isEqualTo LocalTime.of(10, 30)
        prisonLocationId isEqualTo wandsworthLocation.id
      }
    }
  }

  @Test
  fun `Should create history for a probation booking`() {
    val probationBooking = probationBooking()
      .addAppointment(
        prison = prison(prisonCode = WANDSWORTH),
        prisonerNumber = "A1234AA",
        appointmentType = AppointmentType.VLB_PROBATION.name,
        date = tomorrow(),
        startTime = LocalTime.of(9, 30),
        endTime = LocalTime.of(10, 30),
        locationId = wandsworthLocation.id,
      )

    service.createBookingHistory(HistoryType.CREATE, probationBooking)

    verify(bookingHistoryRepository).saveAndFlush(historyCaptor.capture())

    with(historyCaptor.firstValue) {
      historyType isEqualTo HistoryType.CREATE
      createdTime isCloseTo LocalDateTime.now()
      probationMeetingType isEqualTo probationBooking.probationMeetingType
      notesForPrisoners isEqualTo probationBooking.notesForPrisoners
      notesForStaff isEqualTo probationBooking.notesForStaff
      videoUrl isEqualTo probationBooking.videoUrl
      createdBy isEqualTo PROBATION_USER.username
      createdTime isCloseTo LocalDateTime.now()
      appointments() hasSize 1
      with(appointments().first()) {
        prisonCode isEqualTo WANDSWORTH
        prisonerNumber isEqualTo "A1234AA"
        appointmentType isEqualTo AppointmentType.VLB_PROBATION.name
        startTime isEqualTo LocalTime.of(9, 30)
        endTime isEqualTo LocalTime.of(10, 30)
        prisonLocationId isEqualTo wandsworthLocation.id
      }
    }
  }

  @Test
  fun `Should create history for a court booking amendment`() {
    val courtBooking = courtBooking(COURT_USER.username)
      .addAppointment(
        prison = prison(prisonCode = WANDSWORTH),
        prisonerNumber = "A1234AA",
        appointmentType = AppointmentType.VLB_COURT_MAIN.name,
        date = tomorrow(),
        startTime = LocalTime.of(9, 30),
        endTime = LocalTime.of(10, 30),
        locationId = wandsworthLocation.id,
      ).apply {
        amendedBy = "amended by someone else"
        amendedTime = today().atStartOfDay()
      }

    service.createBookingHistory(HistoryType.AMEND, courtBooking)

    verify(bookingHistoryRepository).saveAndFlush(historyCaptor.capture())

    with(historyCaptor.firstValue) {
      historyType isEqualTo HistoryType.AMEND
      comments isEqualTo courtBooking.comments
      notesForPrisoners isEqualTo courtBooking.notesForPrisoners
      notesForStaff isEqualTo courtBooking.notesForStaff
      createdBy isEqualTo "amended by someone else"
      createdTime isEqualTo today().atStartOfDay()
      appointments() hasSize 1
    }
  }

  @Test
  fun `Should create history for a court booking cancellation`() {
    val courtBooking = courtBooking(COURT_USER.username)
      .addAppointment(
        prison = prison(prisonCode = WANDSWORTH),
        prisonerNumber = "A1234AA",
        appointmentType = AppointmentType.VLB_COURT_MAIN.name,
        date = tomorrow(),
        startTime = LocalTime.of(9, 30),
        endTime = LocalTime.of(10, 30),
        locationId = wandsworthLocation.id,
      ).cancel(courtUser(username = "court cancellation user"))

    service.createBookingHistory(HistoryType.CANCEL, courtBooking)

    verify(bookingHistoryRepository).saveAndFlush(historyCaptor.capture())

    with(historyCaptor.firstValue) {
      historyType isEqualTo HistoryType.CANCEL
      comments isEqualTo courtBooking.comments
      notesForPrisoners isEqualTo courtBooking.notesForPrisoners
      notesForStaff isEqualTo courtBooking.notesForStaff
      createdBy isEqualTo "court cancellation user"
      createdTime isCloseTo LocalDateTime.now()
      appointments() hasSize 1
    }
  }

  @Test
  fun `Should create a booking history for a probation booking amendment`() {
    val probationBooking = probationBooking()
      .addAppointment(
        prison = prison(prisonCode = WANDSWORTH),
        prisonerNumber = "A1234AA",
        appointmentType = AppointmentType.VLB_PROBATION.name,
        date = tomorrow(),
        startTime = LocalTime.of(9, 30),
        endTime = LocalTime.of(10, 30),
        locationId = wandsworthLocation.id,
      ).apply {
        amendedBy = "amended by someone else"
        amendedTime = today().atStartOfDay()
      }

    service.createBookingHistory(HistoryType.AMEND, probationBooking)

    verify(bookingHistoryRepository).saveAndFlush(historyCaptor.capture())

    with(historyCaptor.firstValue) {
      historyType isEqualTo HistoryType.AMEND
      comments isEqualTo probationBooking.comments
      notesForPrisoners isEqualTo probationBooking.notesForPrisoners
      notesForStaff isEqualTo probationBooking.notesForStaff
      createdBy isEqualTo "amended by someone else"
      createdTime isEqualTo today().atStartOfDay()
      appointments() hasSize 1
    }
  }

  @Test
  fun `Should create a booking history for a probation booking cancellation`() {
    val probationBooking = probationBooking()
      .addAppointment(
        prison = prison(prisonCode = WANDSWORTH),
        prisonerNumber = "A1234AA",
        appointmentType = AppointmentType.VLB_PROBATION.name,
        date = tomorrow(),
        startTime = LocalTime.of(9, 30),
        endTime = LocalTime.of(10, 30),
        locationId = wandsworthLocation.id,
      ).cancel(probationUser(username = "probation cancellation user"))

    service.createBookingHistory(HistoryType.CANCEL, probationBooking)

    verify(bookingHistoryRepository).saveAndFlush(historyCaptor.capture())

    with(historyCaptor.firstValue) {
      historyType isEqualTo HistoryType.CANCEL
      comments isEqualTo probationBooking.comments
      notesForPrisoners isEqualTo probationBooking.notesForPrisoners
      notesForStaff isEqualTo probationBooking.notesForStaff
      createdBy isEqualTo "probation cancellation user"
      createdTime isCloseTo LocalDateTime.now()
      appointments() hasSize 1
    }
  }

  @Test
  fun `Should cater for multiple prisoners at different prisons on the same booking`() {
    val courtBooking = courtBooking(COURT_USER.username)
      .addAppointment(
        prison = prison(prisonCode = WANDSWORTH),
        prisonerNumber = "A1111AA",
        appointmentType = AppointmentType.VLB_COURT_MAIN.name,
        date = tomorrow(),
        startTime = LocalTime.of(9, 30),
        endTime = LocalTime.of(10, 30),
        locationId = wandsworthLocation.id,
      )
      .addAppointment(
        prison = prison(prisonCode = PENTONVILLE),
        prisonerNumber = "A2222AA",
        appointmentType = AppointmentType.VLB_COURT_MAIN.name,
        date = tomorrow(),
        startTime = LocalTime.of(9, 30),
        endTime = LocalTime.of(10, 30),
        locationId = pentonvilleLocation.id,
      )
      .addAppointment(
        prison = prison(prisonCode = BIRMINGHAM),
        prisonerNumber = "A3333AA",
        appointmentType = AppointmentType.VLB_COURT_MAIN.name,
        date = tomorrow(),
        startTime = LocalTime.of(9, 30),
        endTime = LocalTime.of(10, 30),
        locationId = birminghamLocation.id,
      )

    service.createBookingHistory(HistoryType.CREATE, courtBooking)

    verify(bookingHistoryRepository).saveAndFlush(historyCaptor.capture())

    with(historyCaptor.firstValue) {
      historyType isEqualTo HistoryType.CREATE
      hearingType isEqualTo courtBooking.hearingType
      appointments() hasSize 3
      assertThat(appointments())
        .extracting("prisonCode")
        .containsAll(listOf(WANDSWORTH, PENTONVILLE, BIRMINGHAM))

      assertThat(appointments())
        .extracting("prisonerNumber")
        .containsAll(listOf("A1111AA", "A2222AA", "A3333AA"))

      assertThat(appointments())
        .extracting("prisonLocationId")
        .containsAll(listOf(wandsworthLocation.id, pentonvilleLocation.id, birminghamLocation.id))
    }
  }

  @Test
  fun `Should fail to create new booking history when booking is not new`() {
    with(assertThrows<IllegalArgumentException> { service.createBookingHistory(HistoryType.CREATE, courtBooking().apply { amendedBy = "amend user" }) }) {
      message isEqualTo "Booking 0 must be new for CREATE booking history"
    }

    with(assertThrows<IllegalArgumentException> { service.createBookingHistory(HistoryType.CREATE, courtBooking().cancel(courtUser())) }) {
      message isEqualTo "Booking 0 must be new for CREATE booking history"
    }
  }

  @Test
  fun `Should fail to create amend booking history when booking is not amended`() {
    with(assertThrows<IllegalArgumentException> { service.createBookingHistory(HistoryType.AMEND, courtBooking()) }) {
      message isEqualTo "Booking 0 must be amended for AMEND booking history"
    }

    with(assertThrows<IllegalArgumentException> { service.createBookingHistory(HistoryType.AMEND, courtBooking().cancel(courtUser())) }) {
      message isEqualTo "Booking 0 must be amended for AMEND booking history"
    }
  }

  @Test
  fun `Should fail to create cancel booking history when booking is not cancelled`() {
    with(assertThrows<IllegalArgumentException> { service.createBookingHistory(HistoryType.CANCEL, courtBooking()) }) {
      message isEqualTo "Booking 0 must be cancelled for CANCEL booking history"
    }

    with(assertThrows<IllegalArgumentException> { service.createBookingHistory(HistoryType.CANCEL, courtBooking().apply { amendedBy = "amend user" }) }) {
      message isEqualTo "Booking 0 must be cancelled for CANCEL booking history"
    }
  }
}
