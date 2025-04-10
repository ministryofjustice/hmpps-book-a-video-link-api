package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
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

  fun createAppointment(appointment: PrisonAppointment) {
    log.info("EXTERNAL APPOINTMENTS: creating appointment for prison appointment ID ${appointment.prisonAppointmentId}")

    if (activitiesService.isAppointmentsRolledOutAt(appointment.prisonCode())) {
      log.info("EXTERNAL APPOINTMENTS: appointments rolled out at ${appointment.prisonCode()} creating via activities and appointments")

      // Attempt to check that an appointment does not already exist before creating.
      // This is here because we have seen network timeouts even though the transaction has completed in the external API.
      activitiesService.findMatchingAppointments(appointment).ifEmpty {
        // Only create if no existing matches found
        activitiesService.createAppointment(appointment)?.let { appointmentSeries ->
          log.info("EXTERNAL APPOINTMENTS: created activities and appointments series ${appointmentSeries.id} for prison appointment ${appointment.prisonAppointmentId}")
        }
      }
    } else {
      log.info("EXTERNAL APPOINTMENTS: appointments not rolled out at ${appointment.prisonCode()} creating via prison api")

      // Attempt to check if an appointment does not already exist before creating.
      // This is here because we have seen network timeouts even though the transaction has completed in the external API.
      prisonService.findMatchingAppointments(appointment).ifEmpty {
        // Only create if no existing matches found
        prisonService.createAppointment(appointment)?.let { event ->
          log.info("EXTERNAL APPOINTMENTS: created prison api event ${event.eventId} for prison appointment ${appointment.prisonAppointmentId}")
        }
      }
    }
  }

  @Transactional
  fun cancelAppointment(appointment: PrisonAppointment) {
    log.info("EXTERNAL APPOINTMENTS: deleting appointment for prison appointment ID ${appointment.prisonAppointmentId}")

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
  }

  @Transactional
  fun patchAppointment(
    oldAppointment: BookingHistoryAppointment?,
    newAppointment: PrisonAppointment?,
    createAppointmentCallback: () -> Unit,
  ) {
    val prisonCode = oldAppointment?.prisonCode ?: requireNotNull(newAppointment) { "EXTERNAL APPOINTMENTS: newAppointment must not be null if oldAppointment is also null" }.prisonCode()

    if (activitiesService.isAppointmentsRolledOutAt(prisonCode)) {
      when {
        oldAppointment == null -> {
          createAppointmentCallback()
        }

        newAppointment == null -> {
          activitiesService.findMatchingAppointments(oldAppointment).forEach { matchingId ->
            log.info("EXTERNAL APPOINTMENTS: hard deleting $oldAppointment from activities and appointments")
            activitiesService.cancelAppointment(matchingId, deleteOnCancel = true)
            log.info("EXTERNAL APPOINTMENTS: hard deleted $matchingId from activities and appointments")
          }
        }

        else -> {
          activitiesService.findMatchingAppointments(oldAppointment).forEach { matchingId ->
            log.info("EXTERNAL APPOINTMENTS: patching $oldAppointment in activities and appointments")
            activitiesService.patchAppointment(matchingId, newAppointment)
            log.info("EXTERNAL APPOINTMENTS: patched $matchingId in activities and appointments")
          }
        }
      }
    } else {
      oldAppointment?.let {
        prisonService.findMatchingAppointments(it).forEach { matchingId ->
          log.info("EXTERNAL APPOINTMENTS: deleting $it from prison-api")
          prisonService.cancelAppointment(matchingId)
          log.info("EXTERNAL APPOINTMENTS: deleted $matchingId from prison-api")
        }
      }

      if (newAppointment != null) {
        createAppointmentCallback()
      }
    }
  }
}
