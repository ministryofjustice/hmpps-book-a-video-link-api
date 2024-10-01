package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.argumentCaptor
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvilleLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
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
        locationKey = wandsworthLocation.key,
      )

    service.createBookingHistory(HistoryType.CREATE, courtBooking)

    verify(bookingHistoryRepository).saveAndFlush(historyCaptor.capture())

    with(historyCaptor.firstValue) {
      historyType isEqualTo HistoryType.CREATE
      createdTime isCloseTo LocalDateTime.now()
      hearingType isEqualTo courtBooking.hearingType
      comments isEqualTo "Court hearing comments"
      videoUrl isEqualTo courtBooking.videoUrl
      createdBy isEqualTo COURT_USER.username
      createdTime isCloseTo LocalDateTime.now()
      appointments() hasSize 1
      with(appointments().first()) {
        prisonCode isEqualTo WANDSWORTH
        prisonerNumber isEqualTo "A1234AA"
        appointmentType isEqualTo AppointmentType.VLB_COURT_MAIN.name
        startTime isEqualTo LocalTime.of(9, 30)
        endTime isEqualTo LocalTime.of(10, 30)
        prisonLocKey isEqualTo wandsworthLocation.key
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
        locationKey = wandsworthLocation.key,
      )

    service.createBookingHistory(HistoryType.CREATE, probationBooking)

    verify(bookingHistoryRepository).saveAndFlush(historyCaptor.capture())

    with(historyCaptor.firstValue) {
      historyType isEqualTo HistoryType.CREATE
      createdTime isCloseTo LocalDateTime.now()
      probationMeetingType isEqualTo probationBooking.probationMeetingType
      comments isEqualTo "Probation meeting comments"
      videoUrl isEqualTo probationBooking.videoUrl
      createdBy isEqualTo "Probation team user"
      createdTime isCloseTo LocalDateTime.now()
      appointments() hasSize 1
      with(appointments().first()) {
        prisonCode isEqualTo WANDSWORTH
        prisonerNumber isEqualTo "A1234AA"
        appointmentType isEqualTo AppointmentType.VLB_PROBATION.name
        startTime isEqualTo LocalTime.of(9, 30)
        endTime isEqualTo LocalTime.of(10, 30)
        prisonLocKey isEqualTo wandsworthLocation.key
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
        locationKey = wandsworthLocation.key,
      )

    service.createBookingHistory(HistoryType.AMEND, courtBooking)

    verify(bookingHistoryRepository).saveAndFlush(historyCaptor.capture())

    with(historyCaptor.firstValue) {
      historyType isEqualTo HistoryType.AMEND
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
        locationKey = wandsworthLocation.key,
      )

    service.createBookingHistory(HistoryType.AMEND, probationBooking)

    verify(bookingHistoryRepository).saveAndFlush(historyCaptor.capture())

    with(historyCaptor.firstValue) {
      historyType isEqualTo HistoryType.AMEND
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
        locationKey = wandsworthLocation.key,
      )
      .addAppointment(
        prison = prison(prisonCode = PENTONVILLE),
        prisonerNumber = "A2222AA",
        appointmentType = AppointmentType.VLB_COURT_MAIN.name,
        date = tomorrow(),
        startTime = LocalTime.of(9, 30),
        endTime = LocalTime.of(10, 30),
        locationKey = pentonvilleLocation.key,
      )
      .addAppointment(
        prison = prison(prisonCode = BIRMINGHAM),
        prisonerNumber = "A3333AA",
        appointmentType = AppointmentType.VLB_COURT_MAIN.name,
        date = tomorrow(),
        startTime = LocalTime.of(9, 30),
        endTime = LocalTime.of(10, 30),
        locationKey = birminghamLocation.key,
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
        .extracting("prisonLocKey")
        .containsAll(listOf(wandsworthLocation.key, pentonvilleLocation.key, birminghamLocation.key))
    }
  }
}
