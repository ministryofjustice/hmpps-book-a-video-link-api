package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerMergedEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.PrisonerMergedTelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.TelemetryService

@Component
class PrisonerMergedEventHandler(
  private val videoBookingRepository: VideoBookingRepository,
  private val prisonAppRepository: PrisonAppointmentRepository,
  private val bookingHistoryAppRepository: BookingHistoryAppointmentRepository,
  private val telemetryService: TelemetryService,
) : DomainEventHandler<PrisonerMergedEvent> {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  override fun handle(event: PrisonerMergedEvent) {
    val removed = event.removedPrisonerNumber()
    val replacement = event.replacementPrisonerNumber()

    videoBookingRepository.countDistinctByPrisonerNumber(removed).takeIf { it > 0 }?.let { numberOfBookingsAffected ->
      mergeAffectedAppointments(removed, replacement)
      mergeAffectedAppointmentsHistory(removed, replacement)
      captureTelemetry(removed, replacement, numberOfBookingsAffected)
    } ?: log.info("PRISONER MERGED: nothing to merge for prisoner number '$removed'")
  }

  private fun mergeAffectedAppointments(removed: String, replacement: String) {
    prisonAppRepository.countByPrisonerNumber(removed).let { count ->
      prisonAppRepository.mergePrisonerNumber(removedNumber = removed, replacementNumber = replacement)
      log.info("PRISONER MERGED: merged $count prison appointment(s) - replaced number '$removed' with '$replacement'")
    }
  }

  private fun mergeAffectedAppointmentsHistory(removed: String, replacement: String) {
    bookingHistoryAppRepository.countByPrisonerNumber(removed).let { count ->
      bookingHistoryAppRepository.mergePrisonerNumber(removedNumber = removed, replacementNumber = replacement)
      log.info("PRISONER MERGED: merged $count booking history appointment(s) - replaced number '$removed' with '$replacement'")
    }
  }

  private fun captureTelemetry(removed: String, replacement: String, bookingsUpdated: Long) {
    telemetryService.track(
      PrisonerMergedTelemetryEvent(
        previousPrisonerNumber = removed,
        newPrisonerNumber = replacement,
        bookingsUpdated = bookingsUpdated,
      ),
    )
  }
}
