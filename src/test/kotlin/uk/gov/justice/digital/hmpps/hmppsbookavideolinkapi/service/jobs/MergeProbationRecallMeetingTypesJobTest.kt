package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TimeSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingHistoryService
import java.time.LocalDateTime

class MergeProbationRecallMeetingTypesJobTest {
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val bookingHistoryService: BookingHistoryService = mock()
  private val now = LocalDateTime.now()
  private val timeSource = TimeSource { now }
  private val job = MergeProbationRecallMeetingTypesJob(videoBookingRepository, bookingHistoryService, timeSource)
  private val captor = argumentCaptor<List<VideoBooking>>()

  @Test
  fun `should merge FTR56 meeting type to RECALL`() {
    val booking = probationBooking(meetingType = ProbationMeetingType.FTR56).withProbationPrisonAppointment()

    whenever { videoBookingRepository.findByProbationMeetingTypesOnOrAfterDate(listOf("FTR56", "RR"), now.toLocalDate()) } doReturn listOf(booking)

    job.runJob()

    booking.probationMeetingType isEqualTo "RECALL"
    booking.amendedBy isEqualTo "BOOK_A_VIDEO_LINK_SERVICE"
    booking.amendedTime isEqualTo now

    inOrder(videoBookingRepository, bookingHistoryService) {
      verify(videoBookingRepository).saveAllAndFlush(captor.capture())
      captor.firstValue containsExactlyInAnyOrder listOf(booking)
      verify(bookingHistoryService).createBookingHistory(HistoryType.AMEND, booking)
    }
  }

  @Test
  fun `should merge RR meeting type to RECALL`() {
    val booking = probationBooking(meetingType = ProbationMeetingType.RR).withProbationPrisonAppointment()

    whenever { videoBookingRepository.findByProbationMeetingTypesOnOrAfterDate(listOf("FTR56", "RR"), now.toLocalDate()) } doReturn listOf(booking)

    job.runJob()

    booking.probationMeetingType isEqualTo "RECALL"
    booking.amendedBy isEqualTo "BOOK_A_VIDEO_LINK_SERVICE"
    booking.amendedTime isEqualTo now

    inOrder(videoBookingRepository, bookingHistoryService) {
      verify(videoBookingRepository).saveAllAndFlush(captor.capture())
      captor.firstValue containsExactlyInAnyOrder listOf(booking)
      verify(bookingHistoryService).createBookingHistory(HistoryType.AMEND, booking)
    }
  }

  @Test
  fun `should merge multiple bookings`() {
    val ftr56Booking = probationBooking(meetingType = ProbationMeetingType.FTR56).withProbationPrisonAppointment()
    val rrBooking = probationBooking(meetingType = ProbationMeetingType.RR).withProbationPrisonAppointment()

    whenever { videoBookingRepository.findByProbationMeetingTypesOnOrAfterDate(listOf("FTR56", "RR"), now.toLocalDate()) } doReturn listOf(ftr56Booking, rrBooking)

    job.runJob()

    ftr56Booking.probationMeetingType isEqualTo "RECALL"
    ftr56Booking.amendedBy isEqualTo "BOOK_A_VIDEO_LINK_SERVICE"
    ftr56Booking.amendedTime isEqualTo now

    rrBooking.probationMeetingType isEqualTo "RECALL"
    rrBooking.amendedBy isEqualTo "BOOK_A_VIDEO_LINK_SERVICE"
    rrBooking.amendedTime isEqualTo now

    inOrder(videoBookingRepository, bookingHistoryService) {
      verify(videoBookingRepository).saveAllAndFlush(captor.capture())
      captor.firstValue containsExactlyInAnyOrder listOf(ftr56Booking, rrBooking)
      verify(bookingHistoryService, times(2)).createBookingHistory(eq(HistoryType.AMEND), any())
    }
  }

  @Test
  fun `should handle no bookings to merge`() {
    whenever { videoBookingRepository.findByProbationMeetingTypesOnOrAfterDate(listOf("FTR56", "RR"), now.toLocalDate()) } doReturn emptyList()

    job.runJob()

    verify(videoBookingRepository, never()).saveAllAndFlush(any<List<VideoBooking>>())
    verifyNoInteractions(bookingHistoryService)
  }
}
