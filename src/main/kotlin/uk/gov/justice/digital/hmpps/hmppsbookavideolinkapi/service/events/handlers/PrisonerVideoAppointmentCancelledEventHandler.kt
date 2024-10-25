package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingFacade
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerVideoAppointmentCancelledEvent

@Component
class PrisonerVideoAppointmentCancelledEventHandler(
  private val videoAppointmentRepository: VideoAppointmentRepository,
  private val bookingFacade: BookingFacade,
) : DomainEventHandler<PrisonerVideoAppointmentCancelledEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: PrisonerVideoAppointmentCancelledEvent) {
    if (event.isVideoLinkBooking()) {
      val activeAppointments = videoAppointmentRepository.findActiveVideoAppointments(
        prisonCode = event.prisonCode(),
        prisonerNumber = event.prisonerNumber(),
        appointmentDate = event.date(),
        startTime = event.startTime(),
      )

      if (activeAppointments.size == 1) {
        val appointment = activeAppointments.single()

        if (appointment.isMainAppointment()) {
          bookingFacade.cancel(appointment.videoBookingId, UserService.getServiceAsUser())
        } else {
          // TODO - not yet implemented
          // if single matching appointment and is a pre or post,
          //   remove the appointment, do not propagate to other services
          //   no email necessary
          //   record a history of the removal
          log.info("Not yet handling removal of pre/post appointment here.")
        }

        return
      }

      log.info("Ignoring event for cancellation of appointment, could not find a unique match.")
    }
  }

  private fun VideoAppointment.isMainAppointment() = listOf("VLB_COURT_MAIN", "VLB_PROBATION").contains(appointmentType)
}
