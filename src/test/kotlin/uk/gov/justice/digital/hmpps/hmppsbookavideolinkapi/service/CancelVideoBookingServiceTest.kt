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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.EXTERNAL_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.CaseloadAccessException
import java.util.Optional

class CancelVideoBookingServiceTest {
  private val booking = probationBooking().withProbationPrisonAppointment(prisonCode = BIRMINGHAM)
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val bookingHistoryService: BookingHistoryService = mock()

  private val service = CancelVideoBookingService(videoBookingRepository, bookingHistoryService)

  @Test
  fun `should cancel a video booking for external user`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(booking)
    whenever(videoBookingRepository.saveAndFlush(booking)) doReturn booking

    booking.statusCode isEqualTo StatusCode.ACTIVE

    val booking = service.cancel(1, EXTERNAL_USER)

    booking.statusCode isEqualTo StatusCode.CANCELLED

    verify(videoBookingRepository).saveAndFlush(booking)
    verify(bookingHistoryService).createBookingHistory(HistoryType.CANCEL, booking)
  }

  @Test
  fun `should fail if booking is not found for external user`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.empty()

    val error = assertThrows<EntityNotFoundException> { service.cancel(1, EXTERNAL_USER) }

    error.message isEqualTo "Video booking with ID 1 not found."

    verifyNoInteractions(bookingHistoryService)
    verify(videoBookingRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `should cancel a Birmingham video booking for Birmingham prison user`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(booking)
    whenever(videoBookingRepository.saveAndFlush(booking)) doReturn booking

    booking.statusCode isEqualTo StatusCode.ACTIVE

    val booking = service.cancel(1, PRISON_USER.copy(activeCaseLoadId = BIRMINGHAM))

    booking.statusCode isEqualTo StatusCode.CANCELLED

    verify(videoBookingRepository).saveAndFlush(booking)
    verify(bookingHistoryService).createBookingHistory(HistoryType.CANCEL, booking)
  }

  @Test
  fun `should fail to cancel a Birmingham video booking for Risley prison user`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(booking)
    whenever(videoBookingRepository.saveAndFlush(booking)) doReturn booking

    booking.statusCode isEqualTo StatusCode.ACTIVE

    assertThrows<CaseloadAccessException> { service.cancel(1, PRISON_USER.copy(activeCaseLoadId = RISLEY)) }

    verifyNoInteractions(bookingHistoryService)
    verify(videoBookingRepository, never()).saveAndFlush(any())
  }
}
