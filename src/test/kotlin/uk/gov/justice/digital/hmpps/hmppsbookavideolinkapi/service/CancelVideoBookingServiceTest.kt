package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.StatusCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import java.util.*

class CancelVideoBookingServiceTest {

  private val booking = probationBooking()
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val service = CancelVideoBookingService(videoBookingRepository)

  @Test
  fun `should cancel a video booking`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(booking)
    whenever(videoBookingRepository.saveAndFlush(booking)) doReturn booking

    booking.statusCode isEqualTo StatusCode.ACTIVE

    val booking = service.cancel(1, "Test User")

    booking.statusCode isEqualTo StatusCode.CANCELLED

    verify(videoBookingRepository).saveAndFlush(booking)
  }

  @Test
  fun `should fail if booking is not found`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.empty()

    val error = assertThrows<EntityNotFoundException> { service.cancel(1, "Test user") }

    error.message isEqualTo "Video booking with ID 1 not found."
  }
}
