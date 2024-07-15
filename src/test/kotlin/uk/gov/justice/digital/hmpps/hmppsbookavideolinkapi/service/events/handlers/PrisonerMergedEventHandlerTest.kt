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

  private val mergeEvent = PrisonerMergedEvent(MergeInformation(removedNomsNumber = "old", nomsNumber = "new"))
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val bookingHistoryAppointmentRepository: BookingHistoryAppointmentRepository = mock()
  private val handler = PrisonerMergedEventHandler(prisonAppointmentRepository, bookingHistoryAppointmentRepository)

  @Test
  fun `should merge only prison appointments`() {
    whenever(prisonAppointmentRepository.countByPrisonerNumber("old")) doReturn 1

    handler.handle(mergeEvent)

    verify(prisonAppointmentRepository).mergeOldPrisonerNumberToNew(oldNumber = "old", newNumber = "new")
    verify(bookingHistoryAppointmentRepository, never()).mergeOldPrisonerNumberToNew(anyString(), anyString())
  }

  @Test
  fun `should merge only booking history appointments`() {
    whenever(bookingHistoryAppointmentRepository.countByPrisonerNumber("old")) doReturn 1

    handler.handle(mergeEvent)

    verify(bookingHistoryAppointmentRepository).mergeOldPrisonerNumberToNew(oldNumber = "old", newNumber = "new")
    verify(prisonAppointmentRepository, never()).mergeOldPrisonerNumberToNew(anyString(), anyString())
  }

  @Test
  fun `should merge prison appointments and booking history appointments`() {
    whenever(prisonAppointmentRepository.countByPrisonerNumber("old")) doReturn 1
    whenever(bookingHistoryAppointmentRepository.countByPrisonerNumber("old")) doReturn 1

    handler.handle(mergeEvent)

    verify(prisonAppointmentRepository).mergeOldPrisonerNumberToNew(oldNumber = "old", newNumber = "new")
    verify(bookingHistoryAppointmentRepository).mergeOldPrisonerNumberToNew(oldNumber = "old", newNumber = "new")
  }
}
