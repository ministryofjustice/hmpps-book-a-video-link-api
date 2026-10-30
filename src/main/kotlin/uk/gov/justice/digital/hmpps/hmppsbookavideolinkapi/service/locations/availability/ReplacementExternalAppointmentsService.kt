package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.availability

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.appointmentCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.SupportedAppointmentTypes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AppointmentSlot
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Service
class ReplacementExternalAppointmentsService(
  private val appointmentsClient: ActivitiesAppointmentsClient,
  private val appointmentTypes: SupportedAppointmentTypes,
) {
  /**
   * Get the prison appointments on a date at a specific internal location ID in the prison
   * - filter any A&A appointments that are cancelled
   * - filter any appointments where the end-time is null (we cannot infer the duration)
   * - filter any BVLS-mastered appointment types as these will be retrieved from BVLS itself for checking.
   */
  fun getAppointmentSlots(prisonCode: String, date: LocalDate, location: UUID) = run {
    appointmentsClient.getScheduledPrisonAppointments(prisonCode, date, location)
      .filterNot { it.isDeleted || it.isCancelled || it.endTime == null || appointmentTypes.isSupported(it.appointmentCode()) }
      .flatMap { appointment ->
        appointment.attendees.map { prisoner ->
          ExternalAppointmentSlot(
            location,
            prisoner.prisonerNumber,
            date,
            LocalTime.parse(appointment.startTime),
            LocalTime.parse(appointment.endTime!!),
          )
        }
      }
  }

  class ExternalAppointmentSlot(
    override val prisonLocationId: UUID,
    override val prisonerNumber: String,
    override val appointmentDate: LocalDate,
    override val startTime: LocalTime,
    override val endTime: LocalTime,
  ) : AppointmentSlot
}
