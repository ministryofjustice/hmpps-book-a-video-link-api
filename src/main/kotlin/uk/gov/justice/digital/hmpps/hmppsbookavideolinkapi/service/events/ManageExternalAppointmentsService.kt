package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toHourMinuteStyle
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
            comments = appointment.detailedComments(),
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
            comments = appointment.detailedComments(),
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

  @Transactional
  fun cancelAppointment(prisonAppointmentId: Long) {
    log.info("EXTERNAL APPOINTMENTS: deleting appointment for prison appointment ID $prisonAppointmentId")

    prisonAppointmentRepository.findById(prisonAppointmentId).ifPresentOrElse(
      { appointment ->
        if (activitiesAppointmentsClient.isAppointmentsRolledOutAt(appointment.prisonCode)) {
          activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
            appointment.prisonCode,
            appointment.prisonerNumber,
            appointment.appointmentDate,
            appointment.internalLocationId(),
          ).findMatching(appointment)?.let { matchingAppointment ->
            log.info("EXTERNAL APPOINTMENTS: deleting video booking appointment $appointment from activities and appointments")
            activitiesAppointmentsClient.cancelAppointment(matchingAppointment.appointmentId)
            log.info("EXTERNAL APPOINTMENTS: deleted matching appointment ${matchingAppointment.appointmentId} from activities and appointments")
          } ?: log.info("EXTERNAL APPOINTMENTS: matching activities api appointment not found for prison appointment $prisonAppointmentId")
        } else {
          prisonApiClient.getPrisonersAppointmentsAtLocations(
            appointment.prisonCode,
            appointment.prisonerNumber,
            appointment.appointmentDate,
            appointment.internalLocationId(),
          ).findMatching(appointment)?.let { matchingAppointment ->
            log.info("EXTERNAL APPOINTMENTS: deleting video booking appointment $appointment from prison-api")
            prisonApiClient.cancelAppointment(matchingAppointment.eventId)
            log.info("EXTERNAL APPOINTMENTS: deleted matching appointment ${matchingAppointment.eventId} from prison-api")
          } ?: log.info("EXTERNAL APPOINTMENTS: matching prison-api appointment not found for prison appointment $prisonAppointmentId")
        }
      },
      {
        // Ignore, there is nothing we can do if we do not find the prison appointment
        log.warn("EXTERNAL APPOINTMENTS: Prison appointment with ID $prisonAppointmentId not found")
      },
    )
  }

  private fun Collection<AppointmentSearchResult>.findMatching(appointment: PrisonAppointment) =
    singleOrNull {
      appointment.startTime.toHourMinuteStyle() == it.startTime && appointment.endTime.toHourMinuteStyle() == it.endTime
    }

  private fun Collection<PrisonerSchedule>.findMatching(appointment: PrisonAppointment) =
    singleOrNull {
      it.startTime == appointment.appointmentDate.atTime(appointment.startTime) &&
        it.endTime == appointment.appointmentDate.atTime(appointment.endTime)
    }

  private fun PrisonAppointment.detailedComments() =
    if (videoBooking.isCourtBooking()) {
      "Video booking for a ${videoBooking.hearingType?.lowercase()} court hearing at ${videoBooking.court?.description}\n\n$comments"
    } else {
      "Video booking for a ${videoBooking.probationMeetingType?.lowercase()} probation meeting at ${videoBooking.probationTeam?.description}\n\n$comments"
    }

  // This should never happen but if it ever happens we are throwing NPE with a bit more context to it!
  private fun PrisonAppointment.internalLocationId() =
    prisonApiClient.getInternalLocationByKey(prisonLocKey)?.locationId
      ?: throw NullPointerException("EXTERNAL APPOINTMENTS: Internal location id for key $prisonLocKey not found for prison appointment $prisonAppointmentId")

  // This should never happen but if it ever happens we are throwing NPE with a bit more context to it!
  private fun PrisonAppointment.bookingId() =
    prisonerSearchClient.getPrisoner(prisonerNumber)?.bookingId?.toLong()
      ?: throw NullPointerException("EXTERNAL APPOINTMENTS: Booking id not found for prisoner $prisonerNumber for prison appointment $prisonAppointmentId")
}
