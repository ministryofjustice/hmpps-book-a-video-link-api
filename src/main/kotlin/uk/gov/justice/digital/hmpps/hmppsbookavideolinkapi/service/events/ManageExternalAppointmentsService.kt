package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository

/**
 * This service is responsible for managing appointments outside of BVLS to ensure appointments are kept in sync with
 * other areas e.g. Activities and Appointments and/or Prison API (NOMIS). Any errors raised by this service can be
 * caught and logged but the must be re-thrown up the stack, so they get propagated.
 */
@Service
class ManageExternalAppointmentsService(
  private val videoBookingRepository: VideoBookingRepository,
  private val prisonAppointmentRepository: PrisonAppointmentRepository,
  private val activitiesAppointmentsClient: ActivitiesAppointmentsClient,
) {

  /**
   * This works on the assumption there is one distinct prison per booking, it doesn't support co-defendants which
   * may mean appointments in different prisons with different prisoners.
   */
  fun createAppointments(videoBookingId: Long) {
    val videoBooking = videoBookingRepository.findById(videoBookingId)
      .orElseThrow { EntityNotFoundException("Video booking with ID $videoBookingId not found") }

    val appointments = prisonAppointmentRepository.findByVideoBooking(videoBooking)

    val prisonCode = appointments.map(PrisonAppointment::prisonCode).distinct().single()

    if (activitiesAppointmentsClient.isAppointmentsRolledOutAt(prisonCode)) {
      activitiesAppointmentsClient.createAppointment()
    }

    // TODO else call prion api if not rolled out ...
  }
}
