package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository

/**
 * This service is responsible for the creation/updating/cancelling of appointments outside of BVLS to ensure
 * appointments are kept in sync with other areas e.g. Activities and Appointments and/or Prison API (NOMIS)
 *
 * Any errors raised by this service can be caught and logged but the must be re-thrown up the stack, so they get propagated.
 */
@Service
class ManageExternalAppointmentsService(
  private val videoBookingRepository: VideoBookingRepository,
  private val prisonAppointmentRepository: PrisonAppointmentRepository,
  private val activitiesAppointmentsClient: ActivitiesAppointmentsClient,
  private val prisonApiClient: PrisonApiClient,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  // TODO: Assumes one person per booking, so revisit for co-defendant cases
  fun createAppointments(videoBookingId: Long) {
    log.info("EXTERNAL APPOINTMENTS: creating appointments for video booking ID $videoBookingId")

    val videoBooking = videoBookingRepository.findById(videoBookingId)
      .orElseThrow { EntityNotFoundException("Video booking with ID $videoBookingId not found") }

    val appointments = prisonAppointmentRepository.findByVideoBooking(videoBooking)

    val prisonCode = appointments.map(PrisonAppointment::prisonCode).distinct().single()

    if (activitiesAppointmentsClient.isAppointmentsRolledOutAt(prisonCode)) {
      // TODO change to call proper create method when we can establish the internal location ID.
      activitiesAppointmentsClient.createAppointment()

      log.info("EXTERNAL APPOINTMENTS: created appointments for video booking ID $videoBookingId in activities and appointments")
    } else {
      prisonApiClient.createAppointment()

      log.info("EXTERNAL APPOINTMENTS: created appointments for video booking ID $videoBookingId in prison api")
    }
  }
}
