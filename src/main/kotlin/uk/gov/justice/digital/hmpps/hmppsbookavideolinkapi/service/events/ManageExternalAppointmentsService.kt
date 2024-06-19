package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
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
  private val prisonerSearchClient: PrisonerSearchClient,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun createAppointment(prisonAppointmentId: Long) {
    log.info("EXTERNAL APPOINTMENTS: creating appointment for prison appointment ID $prisonAppointmentId")

    prisonAppointmentRepository.findById(prisonAppointmentId).ifPresentOrElse(
      { appointment ->
        if (activitiesAppointmentsClient.isAppointmentsRolledOutAt(appointment.prisonCode)) {
          log.info("EXTERNAL APPOINTMENTS: appointments rolled out at ${appointment.prisonCode} creating via activities and appointments")

          activitiesAppointmentsClient.createAppointment(
            prisonCode = appointment.prisonCode,
            prisonerNumber = appointment.prisonerNumber,
            startDate = appointment.appointmentDate,
            startTime = appointment.startTime,
            endTime = appointment.endTime,
            internalLocationId = appointment.internalLocationId(),
            extraInformation = appointment.extraInformation(),
          )?.let { appointmentSeries ->
            log.info("EXTERNAL APPOINTMENTS: created activities and appointments series ${appointmentSeries.id} for prison appointment $prisonAppointmentId")
          }
        } else {
          log.info("EXTERNAL APPOINTMENTS: appointments not rolled out at ${appointment.prisonCode} creating via prison api")

          prisonApiClient.createAppointment(
            bookingId = appointment.bookingId(),
            locationId = appointment.internalLocationId(),
            appointmentDate = appointment.appointmentDate,
            startTime = appointment.startTime,
            endTime = appointment.endTime,
            comments = appointment.comments,
          )?.let { event ->
            log.info("EXTERNAL APPOINTMENTS: created prison api event ${event.eventId} for prison appointment $prisonAppointmentId")
          }
        }
      },
      {
        // Ignore, there is nothing we can do if we do not find the prison appointment
        log.warn("EXTERNAL APPOINTMENTS: Prison appointment with ID $prisonAppointmentId not found")
      },
    )
  }

  private fun PrisonAppointment.extraInformation() =
    if (videoBooking.isCourtBooking()) "Video booking for court ${videoBooking.court?.description}" else "Video booking for probation team ${videoBooking.probationTeam?.description}"

  // TODO question - this should never happen but what happens if we find no location? An exception here means the event will never clean up.
  private fun PrisonAppointment.internalLocationId() =
    prisonApiClient.getInternalLocationByKey(prisonLocKey)?.locationId
      ?: throw NullPointerException("EXTERNAL APPOINTMENTS: Internal location id for key $prisonLocKey not found for prison appointment $prisonAppointmentId")

  // TODO question - this should never happen but what happens if we find no booking id? An exception here means the event will never clean up.
  private fun PrisonAppointment.bookingId() =
    prisonerSearchClient.getPrisoner(prisonerNumber)?.bookingId?.toLong()
      ?: throw NullPointerException("EXTERNAL APPOINTMENTS: Booking id not found for prisoner $prisonerNumber for prison appointment $prisonAppointmentId")
}
