package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.SupportedAppointmentTypes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NomisMappingService

@Service
class PrisonService(
  private val prisonApiClient: PrisonApiClient,
  private val nomisMappingService: NomisMappingService,
  private val supportedAppointmentTypes: SupportedAppointmentTypes,
  private val prisonerSearchClient: PrisonerSearchClient,
) {
  private val appointmentsMatcher = AppointmentsMatcher()

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun findMatchingAppointments(appointment: PrisonAppointment) = appointmentsMatcher.findMatchingAppointments(appointment)

  fun findMatchingAppointments(appointment: BookingHistoryAppointment) = appointmentsMatcher.findMatchingAppointments(appointment)

  fun createAppointment(appointment: PrisonAppointment) = run {
    prisonApiClient.createAppointment(
      bookingId = prisonerSearchClient.getPrisoner(appointment.prisonerNumber)?.bookingId?.toLong()!!,
      locationId = nomisMappingService.getNomisLocationId(appointment.prisonLocationId)!!,
      appointmentDate = appointment.appointmentDate,
      startTime = appointment.startTime,
      endTime = appointment.endTime,
      comments = appointment.notesForPrisoners,
      appointmentType = supportedAppointmentTypes.typeOf(appointment.bookingType()),
    )
  }

  fun cancelAppointment(appointmentId: Long) {
    prisonApiClient.cancelAppointment(appointmentId)
  }

  private inner class AppointmentsMatcher {
    fun findMatchingAppointments(appointment: PrisonAppointment): Collection<Long> = run {
      prisonApiClient.getPrisonersAppointmentsAtLocations(
        prisonCode = appointment.prisonCode(),
        prisonerNumber = appointment.prisonerNumber,
        onDate = appointment.appointmentDate,
        nomisMappingService.getNomisLocationId(appointment.prisonLocationId)!!,
      ).findMatching(appointment).map { it.eventId }
    }

    fun findMatchingAppointments(appointment: BookingHistoryAppointment): Collection<Long> = run {
      prisonApiClient.getPrisonersAppointmentsAtLocations(
        prisonCode = appointment.prisonCode,
        prisonerNumber = appointment.prisonerNumber,
        onDate = appointment.appointmentDate,
        nomisMappingService.getNomisLocationId(appointment.prisonLocationId)!!,
      ).findMatching(appointment).map { it.eventId }
    }

    private fun Collection<PrisonerSchedule>.findMatching(appointment: PrisonAppointment): List<PrisonerSchedule> = filter { schedule ->
      schedule.isTheSameTime(appointment) &&
        (schedule.isTheSameAppointmentType(supportedAppointmentTypes.typeOf(appointment.bookingType())) || supportedAppointmentTypes.isSupported(schedule.appointmentCode()))
    }.ifEmpty {
      emptyList<PrisonerSchedule>()
        .also { log.info("PRISON APPOINTMENTS: no matching appointments found in prison-api for prison appointment ${appointment.prisonAppointmentId}") }
    }

    private fun Collection<PrisonerSchedule>.findMatching(bha: BookingHistoryAppointment): List<PrisonerSchedule> = filter { schedule ->
      schedule.isTheSameTime(bha) &&
        (schedule.isTheSameAppointmentType(supportedAppointmentTypes.typeOf(bha.bookingType())) || supportedAppointmentTypes.isSupported(schedule.appointmentCode()))
    }.ifEmpty {
      emptyList<PrisonerSchedule>()
        .also { log.info("PRISON APPOINTMENTS: no matching appointments found in prison-api for booking history appointment ${bha.bookingHistoryAppointmentId}") }
    }
  }
}
