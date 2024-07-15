package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerMergedEvent

@Component
class PrisonerMergedEventHandler(
  private val prisonAppRepository: PrisonAppointmentRepository,
  private val bookingHistoryAppRepository: BookingHistoryAppointmentRepository,
) : DomainEventHandler<PrisonerMergedEvent> {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  override fun handle(event: PrisonerMergedEvent) {
    val old = event.oldPrisonerNumber()
    val new = event.newPrisonerNumber()

    prisonAppRepository.countByPrisonerNumber(old).takeIf { it > 0 }?.let { count ->
      prisonAppRepository.mergeOldPrisonerNumberToNew(oldNumber = old, newNumber = new)
      log.info("PRISONER MERGED: merged $count prison appointment(s) for old number '$old' to new number '$new'")
    } ?: log.info("PRISONER MERGED: nothing to merge for old prisoner number '$old' to new number '$new'")

    bookingHistoryAppRepository.countByPrisonerNumber(old).takeIf { it > 0 }?.let { count ->
      bookingHistoryAppRepository.mergeOldPrisonerNumberToNew(oldNumber = old, newNumber = new)
      log.info("PRISONER MERGED: merged $count booking history appointment(s) for old number '$old' to new number '$new'")
    }
  }
}
