package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingFacade
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingHistoryService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerVideoAppointmentCancelledEvent

/**
 * The nature of this event handler is to do our best to cancel (or delete an appointment) providing we can match the
 * details in the event to a single appointment.
 *
 * A booking is cancelled if the appointment removed in NOMIS is a main court hearing or a probation meeting. If the
 * cancelled appointment is a pre-meeting or post-meeting then appointment is removed from the court booking.
 *
 * If zero or more than one matching appointment is found then we log and ignore this event entirely.
 *
 * For A&A rolled out prisons we can safely ignore this event. Appointments cannot be changed in NOMIS for rolled out
 * prisons, the screens are disabled. However the event still gets raised on the back of syncing from A&A when we delete
 * appointments in BVLS and then in A&A.
 */
@Component
class PrisonerVideoAppointmentCancelledEventHandler(
  private val activitiesAppointmentsClient: ActivitiesAppointmentsClient,
  private val videoAppointmentRepository: VideoAppointmentRepository,
  private val bookingFacade: BookingFacade,
  private val videoBookingRepository: VideoBookingRepository,
  private val prisonAppointmentRepository: PrisonAppointmentRepository,
  private val bookingHistoryService: BookingHistoryService,
) : DomainEventHandler<PrisonerVideoAppointmentCancelledEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  override fun handle(event: PrisonerVideoAppointmentCancelledEvent) {
    if (event.isVideoLinkBooking() && appointmentsNotManagedExternallyAt(event.prisonCode())) {
      val activeAppointments = videoAppointmentRepository.findActiveVideoAppointments(
        prisonCode = event.prisonCode(),
        prisonerNumber = event.prisonerNumber(),
        appointmentDate = event.date(),
        startTime = event.startTime(),
      )

      if (activeAppointments.size == 1) {
        log.info("PRISONER_APPOINTMENT_CANCELLATION: processing $event")

        val appointment = activeAppointments.single()

        when {
          appointment.isForCourtBooking() -> {
            if (appointment.isMainAppointment()) {
              cancelTheBookingForThe(appointment)
            } else {
              removeFromBookingThe(appointment)
            }
          }

          appointment.isForProbationBooking() -> cancelTheBookingForThe(appointment)

          // This should never occur. Will see on the DLQ if it ever does happen!
          else -> throw IllegalArgumentException("Video booking ${appointment.videoBookingId} appointment type ${appointment.appointmentType} not recognised")
        }

        return
      }

      log.info("PRISONER_APPOINTMENT_CANCELLATION: ignoring event ${event.additionalInformation}, could not find a unique match.")
    }
  }

  private fun appointmentsNotManagedExternallyAt(prisonCode: String) = !activitiesAppointmentsClient.isAppointmentsRolledOutAt(prisonCode)

  private fun VideoAppointment.isForCourtBooking() =
    listOf("VLB_COURT_PRE", "VLB_COURT_MAIN", "VLB_COURT_POST").contains(appointmentType)

  private fun VideoAppointment.isForProbationBooking() = appointmentType == "VLB_PROBATION"

  private fun VideoAppointment.isMainAppointment() = listOf("VLB_COURT_MAIN").contains(appointmentType)

  private fun cancelTheBookingForThe(appointment: VideoAppointment) {
    bookingFacade.cancel(appointment.videoBookingId, UserService.getServiceAsUser())
  }
  private fun removeFromBookingThe(appointment: VideoAppointment) {
    prisonAppointmentRepository.deleteById(appointment.prisonAppointmentId)
    prisonAppointmentRepository.flush()

    createBookingHistoryForTheRemoved(appointment)
  }

  private fun createBookingHistoryForTheRemoved(appointment: VideoAppointment) {
    videoBookingRepository.findById(appointment.videoBookingId)
      .ifPresentOrElse(
        { bookingHistoryService.createBookingHistory(HistoryType.AMEND, it) },
        { throw NullPointerException("Video booking with ID ${appointment.videoBookingId} not found.") },
      )
  }
}
