package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository

/**
 * This service is responsible for create/update/cancel of appointments relating to BVLS bookings in
 * downstream services, to ensure that appointments are kept in sync e.g. in Activities and Appointments
 * or Prison API (NOMIS)
 *
 * Any errors raised by this service can be caught and logged but must be re-thrown up the stack so that they
 * cause the queue listener to fail, and cause a retry.
 */
@Service
class ManageExternalAppointmentsService(
  private val prisonAppointmentRepository: PrisonAppointmentRepository,
  private val activitiesService: ActivitiesAndAppointmentsService,
  private val prisonService: PrisonService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun createAppointment(prisonAppointmentId: Long) {
    log.info("EXTERNAL APPOINTMENTS: creating appointment for prison appointment ID $prisonAppointmentId")

    prisonAppointmentRepository.findById(prisonAppointmentId).ifPresentOrElse(
      { appointment ->
        if (activitiesService.isAppointmentsRolledOutAt(appointment.prisonCode())) {
          log.info("EXTERNAL APPOINTMENTS: appointments rolled out at ${appointment.prisonCode()} creating via activities and appointments")

          // Attempt to check that an appointment does not already exist before creating.
          // This is here because we have seen network timeouts even though the transaction has completed in the external API.
          activitiesService.findMatchingAppointments(appointment).ifEmpty {
            // Only create if no existing matches found
            activitiesService.createAppointment(appointment)?.let { appointmentSeries ->
              log.info("EXTERNAL APPOINTMENTS: created activities and appointments series ${appointmentSeries.id} for prison appointment $prisonAppointmentId")
            }
          }
        } else {
          log.info("EXTERNAL APPOINTMENTS: appointments not rolled out at ${appointment.prisonCode()} creating via prison api")

          // Attempt to check if an appointment does not already exist before creating.
          // This is here because we have seen network timeouts even though the transaction has completed in the external API.
          prisonService.findMatchingAppointments(appointment).ifEmpty {
            // Only create if no existing matches found
            prisonService.createAppointment(appointment)?.let { event ->
              log.info("EXTERNAL APPOINTMENTS: created prison api event ${event.eventId} for prison appointment $prisonAppointmentId")
            }
          }
        }
      },
      {
        // Ignore, there is nothing we can do if we do not find the prison appointment
        log.warn("EXTERNAL APPOINTMENTS: Prison appointment with ID $prisonAppointmentId not found")
      },
    )
  }

  @Transactional
  fun cancelCurrentAppointment(prisonAppointmentId: Long) {
    log.info("EXTERNAL APPOINTMENTS: deleting appointment for prison appointment ID $prisonAppointmentId")

    prisonAppointmentRepository.findById(prisonAppointmentId).ifPresentOrElse(
      { appointment ->
        if (activitiesService.isAppointmentsRolledOutAt(appointment.prisonCode())) {
          activitiesService.findMatchingAppointments(appointment).forEach { matchingAppointment ->
            log.info("EXTERNAL APPOINTMENTS: soft deleting video booking appointment $appointment from activities and appointments")
            activitiesService.cancelAppointment(matchingAppointment)
            log.info("EXTERNAL APPOINTMENTS: soft deleted matching appointment $matchingAppointment from activities and appointments")
          }
        } else {
          prisonService.findMatchingAppointments(appointment).forEach { matchingAppointment ->
            log.info("EXTERNAL APPOINTMENTS: deleting video booking appointment $appointment from prison-api")
            prisonService.cancelAppointment(matchingAppointment)
            log.info("EXTERNAL APPOINTMENTS: deleted matching appointment $matchingAppointment from prison-api")
          }
        }
      },
      {
        // Ignore, there is nothing we can do if we do not find the prison appointment
        log.warn("EXTERNAL APPOINTMENTS: Prison appointment with ID $prisonAppointmentId not found")
      },
    )
  }

  @Transactional
  fun cancelPreviousAppointment(bha: BookingHistoryAppointment) {
    log.info("EXTERNAL APPOINTMENTS: deleting previous appointment for booking history appointment ID ${bha.bookingHistoryAppointmentId}")

    if (activitiesService.isAppointmentsRolledOutAt(bha.prisonCode)) {
      activitiesService.findMatchingAppointments(bha).forEach { matchingAppointmentId ->
        log.info("EXTERNAL APPOINTMENTS: hard deleting video booking appointment $bha from activities and appointments")
        activitiesService.cancelAppointment(matchingAppointmentId, deleteOnCancel = true)
        log.info("EXTERNAL APPOINTMENTS: hard deleted matching appointment $matchingAppointmentId from activities and appointments")
      }
    } else {
      prisonService.findMatchingAppointments(bha).forEach { matchingAppointmentId ->
        log.info("EXTERNAL APPOINTMENTS: deleting video booking appointment $bha from prison-api")
        prisonService.cancelAppointment(matchingAppointmentId)
        log.info("EXTERNAL APPOINTMENTS: deleted matching appointment $matchingAppointmentId from prison-api")
      }
    }
  }
}
