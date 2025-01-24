package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping.NomisMappingClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import java.time.LocalTime

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
  private val activitiesAppointmentsClient: ActivitiesAppointmentsClient,
  private val prisonApiClient: PrisonApiClient,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val nomisMappingClient: NomisMappingClient,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun createAppointment(prisonAppointmentId: Long) {
    log.info("EXTERNAL APPOINTMENTS: creating appointment for prison appointment ID $prisonAppointmentId")

    prisonAppointmentRepository.findById(prisonAppointmentId).ifPresentOrElse(
      { appointment ->
        if (activitiesAppointmentsClient.isAppointmentsRolledOutAt(appointment.prisonCode())) {
          log.info("EXTERNAL APPOINTMENTS: appointments rolled out at ${appointment.prisonCode()} creating via activities and appointments")

          val internalLocationId = appointment.internalLocationId()

          // Attempt to check that an appointment does not already exist before creating.
          // This is here because we have seen network timeouts even though the transaction has completed in the external API.
          activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
            prisonCode = appointment.prisonCode(),
            prisonerNumber = appointment.prisonerNumber,
            onDate = appointment.appointmentDate,
            internalLocationId,
          ).findMatchingAppointments(appointment).ifEmpty {
            // Only create if no existing matches found
            activitiesAppointmentsClient.createAppointment(
              prisonCode = appointment.prisonCode(),
              prisonerNumber = appointment.prisonerNumber,
              startDate = appointment.appointmentDate,
              startTime = appointment.startTime,
              endTime = appointment.endTime,
              internalLocationId = internalLocationId,
              comments = appointment.comments,
            )?.let { appointmentSeries ->
              log.info("EXTERNAL APPOINTMENTS: created activities and appointments series ${appointmentSeries.id} for prison appointment $prisonAppointmentId")
            }
          }
        } else {
          log.info("EXTERNAL APPOINTMENTS: appointments not rolled out at ${appointment.prisonCode()} creating via prison api")

          val internalLocationId = appointment.internalLocationId()

          // Attempt to check if an appointment does not already exist before creating.
          // This is here because we have seen network timeouts even though the transaction has completed in the external API.
          prisonApiClient.getPrisonersAppointmentsAtLocations(
            prisonCode = appointment.prisonCode(),
            prisonerNumber = appointment.prisonerNumber,
            onDate = appointment.appointmentDate,
            internalLocationId,
          ).findMatchingPrisonApi(appointment).ifEmpty {
            // Only create if no existing matches found
            prisonApiClient.createAppointment(
              bookingId = appointment.bookingId(),
              locationId = internalLocationId,
              appointmentDate = appointment.appointmentDate,
              startTime = appointment.startTime,
              endTime = appointment.endTime,
              comments = appointment.comments,
            )?.let { event ->
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
        if (activitiesAppointmentsClient.isAppointmentsRolledOutAt(appointment.prisonCode())) {
          activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
            prisonCode = appointment.prisonCode(),
            prisonerNumber = appointment.prisonerNumber,
            onDate = appointment.appointmentDate,
            appointment.internalLocationId(),
          ).findMatchingAppointments(appointment)
            .forEach { matchingAppointment ->
              log.info("EXTERNAL APPOINTMENTS: deleting video booking appointment $appointment from activities and appointments")
              activitiesAppointmentsClient.cancelAppointment(matchingAppointment.appointmentId)
              log.info("EXTERNAL APPOINTMENTS: deleted matching appointment ${matchingAppointment.appointmentId} from activities and appointments")
            }
        } else {
          prisonApiClient.getPrisonersAppointmentsAtLocations(
            prisonCode = appointment.prisonCode(),
            prisonerNumber = appointment.prisonerNumber,
            onDate = appointment.appointmentDate,
            appointment.internalLocationId(),
          ).findMatchingPrisonApi(appointment)
            .forEach { matchingAppointment ->
              log.info("EXTERNAL APPOINTMENTS: deleting video booking appointment $appointment from prison-api")
              prisonApiClient.cancelAppointment(matchingAppointment.eventId)
              log.info("EXTERNAL APPOINTMENTS: deleted matching appointment ${matchingAppointment.eventId} from prison-api")
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

    if (activitiesAppointmentsClient.isAppointmentsRolledOutAt(bha.prisonCode)) {
      activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
        prisonCode = bha.prisonCode,
        prisonerNumber = bha.prisonerNumber,
        onDate = bha.appointmentDate,
        bha.internalLocationId(),
      ).findMatchingAppointments(bha).forEach { matchingAppointment ->
        log.info("EXTERNAL APPOINTMENTS: deleting video booking appointment $bha from activities and appointments")
        activitiesAppointmentsClient.cancelAppointment(matchingAppointment.appointmentId)
        log.info("EXTERNAL APPOINTMENTS: deleted matching appointment ${matchingAppointment.appointmentId} from activities and appointments")
      }
    } else {
      prisonApiClient.getPrisonersAppointmentsAtLocations(
        prisonCode = bha.prisonCode,
        prisonerNumber = bha.prisonerNumber,
        onDate = bha.appointmentDate,
        bha.internalLocationId(),
      ).findMatchingPrisonApi(bha).forEach { matchingAppointment ->
        log.info("EXTERNAL APPOINTMENTS: deleting video booking appointment $bha from prison-api")
        prisonApiClient.cancelAppointment(matchingAppointment.eventId)
        log.info("EXTERNAL APPOINTMENTS: deleted matching appointment ${matchingAppointment.eventId} from prison-api")
      }
    }
  }

  private fun Collection<AppointmentSearchResult>.findMatchingAppointments(appointment: PrisonAppointment): List<AppointmentSearchResult> =
    filter {
      appointment.startTime == LocalTime.parse(it.startTime) && appointment.endTime == LocalTime.parse(it.endTime)
    }.ifEmpty {
      emptyList<AppointmentSearchResult>()
        .also { log.info("EXTERNAL APPOINTMENTS: no matching appointments found in A&A for prison appointment ${appointment.prisonAppointmentId}") }
    }

  private fun Collection<AppointmentSearchResult>.findMatchingAppointments(bha: BookingHistoryAppointment): List<AppointmentSearchResult> =
    filter {
      bha.startTime == LocalTime.parse(it.startTime) && bha.endTime == LocalTime.parse(it.endTime)
    }.ifEmpty {
      emptyList<AppointmentSearchResult>()
        .also { log.info("EXTERNAL APPOINTMENTS: no matching appointments found in A&A for booking history appointment ${bha.bookingHistoryAppointmentId}") }
    }

  private fun Collection<PrisonerSchedule>.findMatchingPrisonApi(appointment: PrisonAppointment): List<PrisonerSchedule> =
    filter {
      it.startTime == appointment.appointmentDate.atTime(appointment.startTime) &&
        appointment.appointmentDate.atTime(appointment.endTime) == it.endTime
    }.ifEmpty {
      emptyList<PrisonerSchedule>()
        .also { log.info("EXTERNAL APPOINTMENTS: no matching appointments found in prison-api for prison appointment ${appointment.prisonAppointmentId}") }
    }

  private fun Collection<PrisonerSchedule>.findMatchingPrisonApi(bha: BookingHistoryAppointment): List<PrisonerSchedule> =
    filter {
      it.startTime == bha.appointmentDate.atTime(bha.startTime) &&
        bha.appointmentDate.atTime(bha.endTime) == it.endTime
    }.ifEmpty {
      emptyList<PrisonerSchedule>()
        .also { log.info("EXTERNAL APPOINTMENTS: no matching appointments found in prison-api for booking history appointment ${bha.bookingHistoryAppointmentId}") }
    }

  // This should never happen but if it ever happens we are throwing NPE with a bit more context to it!
  private fun PrisonAppointment.internalLocationId() =
    nomisMappingClient.getNomisLocationMappingBy(prisonLocationId)?.nomisLocationId
      ?: throw NullPointerException("EXTERNAL APPOINTMENTS: Internal location id for key $prisonLocationId not found for prison appointment $prisonAppointmentId")

  // This should never happen but if it ever happens we are throwing NPE with a bit more context to it!
  private fun PrisonAppointment.bookingId() =
    prisonerSearchClient.getPrisoner(prisonerNumber)?.bookingId?.toLong()
      ?: throw NullPointerException("EXTERNAL APPOINTMENTS: Booking id not found for prisoner $prisonerNumber for prison appointment $prisonAppointmentId")

  private fun BookingHistoryAppointment.internalLocationId() =
    nomisMappingClient.getNomisLocationMappingBy(prisonLocationId)?.nomisLocationId
      ?: throw NullPointerException("EXTERNAL APPOINTMENTS: Internal location id for key $prisonLocationId not found for prison appointment ${bookingHistoryAppointmentId}Id")
}
