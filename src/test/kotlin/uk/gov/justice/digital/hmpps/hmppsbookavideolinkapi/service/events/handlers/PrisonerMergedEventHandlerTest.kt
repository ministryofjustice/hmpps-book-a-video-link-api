package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.MergeInformation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerMergedEvent

class PrisonerMergedEventHandlerTest {

  private val mergeEvent = PrisonerMergedEvent(MergeInformation(removedNomsNumber = "removed", nomsNumber = "replacement"))
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val bookingHistoryAppointmentRepository: BookingHistoryAppointmentRepository = mock()
  private val handler = PrisonerMergedEventHandler(prisonAppointmentRepository, bookingHistoryAppointmentRepository)

  @Test
  fun `should merge only prison appointments`() {
    whenever(prisonAppointmentRepository.countByPrisonerNumber("removed")) doReturn 1

    handler.handle(mergeEvent)

    verify(prisonAppointmentRepository).mergePrisonerNumber(removedNumber = "removed", replacementNumber = "replacement")
    verify(bookingHistoryAppointmentRepository, never()).mergePrisonerNumber(anyString(), anyString())
  }

  @Test
  fun `should merge only booking history appointments`() {
    whenever(bookingHistoryAppointmentRepository.countByPrisonerNumber("removed")) doReturn 1

    handler.handle(mergeEvent)

    verify(bookingHistoryAppointmentRepository).mergePrisonerNumber(removedNumber = "removed", replacementNumber = "replacement")
    verify(prisonAppointmentRepository, never()).mergePrisonerNumber(anyString(), anyString())
  }

  @Test
  fun `should merge prison appointments and booking history appointments`() {
    whenever(prisonAppointmentRepository.countByPrisonerNumber("removed")) doReturn 1
    whenever(bookingHistoryAppointmentRepository.countByPrisonerNumber("removed")) doReturn 1

    handler.handle(mergeEvent)

    verify(prisonAppointmentRepository).mergePrisonerNumber(removedNumber = "removed", replacementNumber = "replacement")
    verify(bookingHistoryAppointmentRepository).mergePrisonerNumber(removedNumber = "removed", replacementNumber = "replacement")
  }

  @Test
  fun `should be no-op when nothing to merge`() {
    whenever(prisonAppointmentRepository.countByPrisonerNumber("removed")) doReturn 0
    whenever(bookingHistoryAppointmentRepository.countByPrisonerNumber("removed")) doReturn 0

    handler.handle(mergeEvent)

    verify(prisonAppointmentRepository, never()).mergePrisonerNumber(anyString(), anyString())
    verify(bookingHistoryAppointmentRepository, never()).mergePrisonerNumber(anyString(), anyString())
  }
}
