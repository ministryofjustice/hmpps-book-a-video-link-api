package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment

/**
 * This service is responsible for create/update/cancel of appointments relating to BVLS bookings in
 * downstream services, to ensure that appointments are kept in sync e.g. in Activities and Appointments.
 *
 * Any errors raised by this service can be caught and logged but must be re-thrown up the stack so that they
 * cause the queue listener to fail and cause a retry.
 */
@Service
class ManageExternalAppointmentsService(private val activitiesService: ActivitiesAndAppointmentsService) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun createAppointment(appointment: PrisonAppointment) {
    log.info("EXTERNAL APPOINTMENTS: creating appointment for prison appointment ID ${appointment.prisonAppointmentId}")

    // Attempt to check that an appointment does not already exist before creating.
    // This is here because we have seen network timeouts even though the transaction has completed in the external API.
    activitiesService.findMatchingAppointments(appointment).ifEmpty {
      // Only create if no existing matches found
      activitiesService.createAppointment(appointment)?.let { appointmentSeries ->
        log.info("EXTERNAL APPOINTMENTS: created activities and appointments series ${appointmentSeries.id} for prison appointment ${appointment.prisonAppointmentId}")
      }
    }
  }

  @Transactional
  fun cancelCurrentAppointment(appointment: PrisonAppointment) {
    log.info("EXTERNAL APPOINTMENTS: deleting appointment for prison appointment ID ${appointment.prisonAppointmentId}")

    activitiesService.findMatchingAppointments(appointment).forEach { matchingAppointment ->
      log.info("EXTERNAL APPOINTMENTS: soft deleting video booking appointment $appointment from activities and appointments")
      activitiesService.cancelAppointment(matchingAppointment)
      log.info("EXTERNAL APPOINTMENTS: soft deleted matching appointment $matchingAppointment from activities and appointments")
    }
  }

  @Transactional
  fun cancelPreviousAppointment(bha: BookingHistoryAppointment) {
    log.info("EXTERNAL APPOINTMENTS: deleting previous appointment for booking history appointment ID ${bha.bookingHistoryAppointmentId}")

    activitiesService.findMatchingAppointments(bha).forEach { matchingAppointmentId ->
      log.info("EXTERNAL APPOINTMENTS: hard deleting video booking appointment $bha from activities and appointments")
      activitiesService.cancelAppointment(matchingAppointmentId, deleteOnCancel = true)
      log.info("EXTERNAL APPOINTMENTS: hard deleted matching appointment $matchingAppointmentId from activities and appointments")
    }
  }

  @Transactional
  fun amendAppointment(oldAppointment: BookingHistoryAppointment, newAppointment: PrisonAppointment) {
    activitiesService.findMatchingAppointments(oldAppointment).forEach { matchingId ->
      log.info("EXTERNAL APPOINTMENTS: patching $oldAppointment in activities and appointments")
      activitiesService.patchAppointment(matchingId, newAppointment)
      log.info("EXTERNAL APPOINTMENTS: patched $matchingId in activities and appointments")
    }
  }
}
