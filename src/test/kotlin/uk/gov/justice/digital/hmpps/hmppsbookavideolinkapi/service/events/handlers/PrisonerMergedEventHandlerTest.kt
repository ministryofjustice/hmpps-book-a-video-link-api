package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsEntriesExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.MergeInformation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerMergedEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.PrisonerMergedTelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.TelemetryService

class PrisonerMergedEventHandlerTest {

  private val mergeEvent = PrisonerMergedEvent(MergeInformation(removedNomsNumber = "removed", nomsNumber = "replacement"))
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val bookingHistoryAppointmentRepository: BookingHistoryAppointmentRepository = mock()
  private val telemetryService: TelemetryService = mock()
  private val telemetryCaptor = argumentCaptor<PrisonerMergedTelemetryEvent>()
  private val handler = PrisonerMergedEventHandler(videoBookingRepository, prisonAppointmentRepository, bookingHistoryAppointmentRepository, telemetryService)

  @Test
  fun `should merge prison appointments and booking history appointments`() {
    whenever(videoBookingRepository.countDistinctByPrisonerNumber("removed")) doReturn 1
    whenever(prisonAppointmentRepository.countByPrisonerNumber("removed")) doReturn 1
    whenever(bookingHistoryAppointmentRepository.countByPrisonerNumber("removed")) doReturn 1

    handler.handle(mergeEvent)

    inOrder(prisonAppointmentRepository, bookingHistoryAppointmentRepository, telemetryService) {
      verify(prisonAppointmentRepository).mergePrisonerNumber(removedNumber = "removed", replacementNumber = "replacement")
      verify(bookingHistoryAppointmentRepository).mergePrisonerNumber(removedNumber = "removed", replacementNumber = "replacement")
      verify(telemetryService).track(telemetryCaptor.capture())
    }

    with(telemetryCaptor.firstValue) {
      properties() containsEntriesExactlyInAnyOrder mapOf(
        "previous_prisoner_number" to "removed",
        "new_prisoner_number" to "replacement",
      )

      metrics() containsEntriesExactlyInAnyOrder mapOf("bookings_updated" to 1.0)
    }
  }

  @Test
  fun `should be no-op when nothing to merge`() {
    whenever(videoBookingRepository.countDistinctByPrisonerNumber("removed")) doReturn 0

    handler.handle(mergeEvent)

    verify(prisonAppointmentRepository, never()).mergePrisonerNumber(anyString(), anyString())
    verify(bookingHistoryAppointmentRepository, never()).mergePrisonerNumber(anyString(), anyString())
    verify(telemetryService, never()).track(any())
  }
}
