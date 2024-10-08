package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.StatusCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.VideoBookingAccessException
import java.util.Optional

class CancelVideoBookingServiceTest {
  private val probationBooking = probationBooking().withProbationPrisonAppointment(prisonCode = BIRMINGHAM)
  private val courtBooking = courtBooking().withMainCourtPrisonAppointment()
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val bookingHistoryService: BookingHistoryService = mock()

  private val service = CancelVideoBookingService(videoBookingRepository, bookingHistoryService)

  @Test
  fun `should cancel a probation video booking for probation user`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(probationBooking)
    whenever(videoBookingRepository.saveAndFlush(probationBooking)) doReturn probationBooking

    probationBooking.statusCode isEqualTo StatusCode.ACTIVE

    val booking = service.cancel(1, PROBATION_USER)

    booking.statusCode isEqualTo StatusCode.CANCELLED

    verify(videoBookingRepository).saveAndFlush(booking)
    verify(bookingHistoryService).createBookingHistory(HistoryType.CANCEL, booking)
  }

  @Test
  fun `should cancel a court video booking for court user`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(courtBooking)
    whenever(videoBookingRepository.saveAndFlush(probationBooking)) doReturn courtBooking

    courtBooking.statusCode isEqualTo StatusCode.ACTIVE

    val booking = service.cancel(1, COURT_USER)

    booking.statusCode isEqualTo StatusCode.CANCELLED

    verify(videoBookingRepository).saveAndFlush(booking)
    verify(bookingHistoryService).createBookingHistory(HistoryType.CANCEL, booking)
  }

  @Test
  fun `should fail if booking is not found for user`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.empty()

    val error = assertThrows<EntityNotFoundException> { service.cancel(1, PROBATION_USER) }

    error.message isEqualTo "Video booking with ID 1 not found."

    verifyNoInteractions(bookingHistoryService)
    verify(videoBookingRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `should cancel a Birmingham video booking for Birmingham prison user`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(probationBooking)
    whenever(videoBookingRepository.saveAndFlush(probationBooking)) doReturn probationBooking

    probationBooking.statusCode isEqualTo StatusCode.ACTIVE

    val booking = service.cancel(1, PRISON_USER_BIRMINGHAM)

    booking.statusCode isEqualTo StatusCode.CANCELLED

    verify(videoBookingRepository).saveAndFlush(booking)
    verify(bookingHistoryService).createBookingHistory(HistoryType.CANCEL, booking)
  }

  @Test
  fun `should fail to cancel a Birmingham video booking for Risley prison user`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(probationBooking)
    whenever(videoBookingRepository.saveAndFlush(probationBooking)) doReturn probationBooking

    probationBooking.statusCode isEqualTo StatusCode.ACTIVE

    assertThrows<CaseloadAccessException> { service.cancel(1, PRISON_USER_RISLEY) }

    verifyNoInteractions(bookingHistoryService)
    verify(videoBookingRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `should fail to cancel a court video booking when user is probation user`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(courtBooking())

    assertThrows<VideoBookingAccessException> { service.cancel(1, PROBATION_USER) }
  }

  @Test
  fun `should fail to cancel a probation video booking when user is court user`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(probationBooking())

    assertThrows<VideoBookingAccessException> { service.cancel(1, COURT_USER) }
  }
}
