package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository

/**
 * This service is responsible for the creation/updating/cancelling of appointments outside of BVLS to ensure
 * appointments are kept in sync with other areas e.g. Activities and Appointments and/or Prison API (NOMIS)
 *
 * Any errors raised by this service can be caught and logged but must be re-thrown up the stack, so they get propagated.
 */
@Service
class ManageExternalAppointmentsService(
  private val prisonAppointmentRepository: PrisonAppointmentRepository,
  private val activitiesAppointmentsClient: ActivitiesAppointmentsClient,
  private val prisonApiClient: PrisonApiClient,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun createAppointment(prisonAppointmentId: Long) {
    log.info("EXTERNAL APPOINTMENTS: creating appointment for prison appointment ID $prisonAppointmentId")

    prisonAppointmentRepository.findById(prisonAppointmentId).ifPresentOrElse(
      { appointment ->
        if (activitiesAppointmentsClient.isAppointmentsRolledOutAt(appointment.prisonCode)) {
          // TODO change to call proper create method when we can establish the internal location ID.
          activitiesAppointmentsClient.createAppointment()

          log.info("EXTERNAL APPOINTMENTS: created appointment for prison appointment ID $prisonAppointmentId in activities and appointments")
        } else {
          prisonApiClient.createAppointment()

          log.info("EXTERNAL APPOINTMENTS: created appointment for prison appointment ID $prisonAppointmentId in prison api")
        }
      },
      {
        // Ignore, there is nothing we can do if we do not find the prison appointment
        log.warn("Prison appointment with ID $prisonAppointmentId not found")
      },
    )
  }
}
