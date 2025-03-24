package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.appointmentCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.isTheSameAppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.isTheSameTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.SupportedAppointmentTypes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NomisMappingService

@Service
class ActivitiesAndAppointmentsService(
  private val activitiesAppointmentsClient: ActivitiesAppointmentsClient,
  private val nomisMappingService: NomisMappingService,
  private val supportedAppointmentTypes: SupportedAppointmentTypes,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun isAppointmentsRolledOutAt(prisonCode: String) = activitiesAppointmentsClient.isAppointmentsRolledOutAt(prisonCode)

  fun findMatchingAppointments(appointment: PrisonAppointment) = AppointmentsMatcher().findMatchingAppointments(appointment)

  fun findMatchingAppointments(appointment: BookingHistoryAppointment) = AppointmentsMatcher().findMatchingAppointments(appointment)

  fun createAppointment(appointment: PrisonAppointment) = run {
    activitiesAppointmentsClient.createAppointment(
      prisonCode = appointment.prisonCode(),
      prisonerNumber = appointment.prisonerNumber,
      startDate = appointment.appointmentDate,
      startTime = appointment.startTime,
      endTime = appointment.endTime,
      internalLocationId = nomisMappingService.getNomisLocationId(appointment.prisonLocationId)!!,
      comments = appointment.comments,
      appointmentType = supportedAppointmentTypes.typeOf(appointment.bookingType()),
    )
  }

  fun cancelAppointment(appointmentId: Long, deleteOnCancel: Boolean = false) {
    activitiesAppointmentsClient.cancelAppointment(appointmentId, deleteOnCancel)
  }

  private inner class AppointmentsMatcher {
    fun findMatchingAppointments(appointment: PrisonAppointment): Collection<Long> = run {
      activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
        prisonCode = appointment.prisonCode(),
        prisonerNumber = appointment.prisonerNumber,
        onDate = appointment.appointmentDate,
        nomisMappingService.getNomisLocationId(appointment.prisonLocationId)!!,
      ).findMatchingAppointments(appointment).map { it.appointmentId }
    }

    fun findMatchingAppointments(appointment: BookingHistoryAppointment): Collection<Long> = run {
      activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
        prisonCode = appointment.prisonCode,
        prisonerNumber = appointment.prisonerNumber,
        onDate = appointment.appointmentDate,
        nomisMappingService.getNomisLocationId(appointment.prisonLocationId)!!,
      ).findMatchingAppointments(appointment).map { it.appointmentId }
    }

    private fun Collection<AppointmentSearchResult>.findMatchingAppointments(appointment: PrisonAppointment): List<AppointmentSearchResult> = filter { searchResult ->
      searchResult.isTheSameTime(appointment) &&
        (searchResult.isTheSameAppointmentType(supportedAppointmentTypes.typeOf(appointment.bookingType())) || supportedAppointmentTypes.isSupported(searchResult.appointmentCode())) &&
        searchResult.isCancelled.not()
    }.ifEmpty {
      emptyList<AppointmentSearchResult>()
        .also { log.info("ACTIVITIES APPOINTMENTS: no matching appointments found in A&A for prison appointment ${appointment.prisonAppointmentId}") }
    }

    private fun Collection<AppointmentSearchResult>.findMatchingAppointments(bha: BookingHistoryAppointment): List<AppointmentSearchResult> = filter { searchResult ->
      searchResult.isTheSameTime(bha) &&
        (searchResult.isTheSameAppointmentType(supportedAppointmentTypes.typeOf(bha.bookingType())) || supportedAppointmentTypes.isSupported(searchResult.appointmentCode())) &&
        searchResult.isCancelled.not()
    }.ifEmpty {
      emptyList<AppointmentSearchResult>()
        .also { log.info("ACTIVITIES APPOINTMENTS: no matching appointments found in A&A for booking history appointment ${bha.bookingHistoryAppointmentId}") }
    }
  }
}
