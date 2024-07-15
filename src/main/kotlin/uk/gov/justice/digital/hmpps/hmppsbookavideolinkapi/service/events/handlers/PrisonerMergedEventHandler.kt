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
    val removed = event.removedPrisonerNumber()
    val replacement = event.replacementPrisonerNumber()

    prisonAppRepository.countByPrisonerNumber(removed).takeIf { it > 0 }?.let { count ->
      prisonAppRepository.mergePrisonerNumber(removedNumber = removed, replacementNumber = replacement)
      log.info("PRISONER MERGED: merged $count prison appointment(s) - replaced number '$removed' with '$replacement'")
    } ?: log.info("PRISONER MERGED: nothing to merge for prisoner number '$removed'")

    bookingHistoryAppRepository.countByPrisonerNumber(removed).takeIf { it > 0 }?.let { count ->
      bookingHistoryAppRepository.mergePrisonerNumber(removedNumber = removed, replacementNumber = replacement)
      log.info("PRISONER MERGED: merged $count booking history appointment(s) - replaced number '$removed' with '$replacement'")
    }
  }
}
