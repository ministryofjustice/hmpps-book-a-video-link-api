package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi

import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.model.Movement
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.model.NewAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.SupportedAppointmentTypes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDate
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDateTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

const val DO_NOT_PROPAGATE = true.toString()

@Component
class PrisonApiClient(private val prisonApiWebClient: WebClient) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun createAppointment(
    bookingId: Long,
    locationId: Long,
    appointmentDate: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
    comments: String? = null,
    appointmentType: SupportedAppointmentTypes.Type,
  ): ScheduledEvent? = prisonApiWebClient
    .post()
    .uri("/api/bookings/{bookingId}/appointments", bookingId)
    .header("no-event-propagation", DO_NOT_PROPAGATE)
    .bodyValue(
      NewAppointment(
        appointmentType = appointmentType.code,
        locationId = locationId,
        startTime = appointmentDate.atTime(startTime).toIsoDateTime(),
        endTime = appointmentDate.atTime(endTime).toIsoDateTime(),
        comment = comments,
      ),
    )
    .retrieve()
    .bodyToMono(ScheduledEvent::class.java)
    .block()

  /**
   * Returns all matching appointment (types) for a prisoner, not just video link bookings.
   */
  fun getPrisonersAppointmentsAtLocations(prisonCode: String, prisonerNumber: String, onDate: LocalDate, vararg locationIds: Long): List<PrisonerSchedule> = if (locationIds.isNotEmpty()) {
    log.info("PRISON-API CLIENT: query params - prisonCode=$prisonCode, prisonerNumber=$prisonerNumber, onDate=$onDate, locationIds=${locationIds.toList()}")
    getPrisonersAppointments(prisonCode, prisonerNumber, onDate).filter { it.locationId in locationIds }
  } else {
    emptyList()
  }

  private fun PrisonerSchedule.redacted() = copy(firstName = "xxxx", lastName = "xxxx")

  private fun getPrisonersAppointments(prisonCode: String, prisonerNumber: String, onDate: LocalDate): List<PrisonerSchedule> = prisonApiWebClient
    .post()
    .uri("/api/schedules/{prisonCode}/appointments?date={date}", prisonCode, onDate.toIsoDate())
    .bodyValue(listOf(prisonerNumber))
    .retrieve()
    .bodyToMono(typeReference<List<PrisonerSchedule>>())
    .doOnError { error -> log.info("Error looking up prisoners appointments by prison code $prisonCode, prisoner number $prisonerNumber, on date $onDate in prison api client", error) }
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block() ?: emptyList()

  /**
   * @param appointmentId refers the appointment identifier held in NOMIS, not BVLS.
   */
  fun cancelAppointment(appointmentId: Long) {
    prisonApiWebClient.delete()
      .uri("/api/appointments/{appointmentId}", appointmentId)
      .header("no-event-propagation", DO_NOT_PROPAGATE)
      .retrieve()
      .bodyToMono(Void::class.java)
      .doOnError { error -> log.info("Error cancelling appointment by appointment id $appointmentId in prison api client", error) }
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()
  }

  /**
   * Gets appointments in this prison, on this date, at a specific internal location ID.
   */
  fun getScheduledAppointments(prisonCode: String, date: LocalDate, locationId: Long) = prisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/api/schedules/{prisonCode}/appointments")
        .queryParam("date", date)
        .queryParam("locationId", locationId)
        .build(prisonCode)
    }
    .retrieve()
    .bodyToMono(typeReference<List<ScheduledAppointment>>())
    .block() ?: emptyList()

  /**
   * Returns all matching appointment types for a prison, not just video link bookings. It will however filter down to
   * the supplied location IDs where there is match.
   */
  fun getScheduledAppointments(prisonCode: String, date: LocalDate, locationIds: Collection<Long>) = run {
    (
      prisonApiWebClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/api/schedules/{prisonCode}/appointments")
            .queryParam("date", date)
            .build(prisonCode)
        }
        .retrieve()
        .bodyToMono(typeReference<List<ScheduledAppointment>>())
        .block() ?: emptyList()
      ).filter { appointment -> locationIds.isEmpty() || appointment.locationId in locationIds }
  }

  fun getLatestPrisonerMovementOnDate(prisonerNumber: String, date: LocalDate) = run {
    if (date.isAfter(LocalDate.now())) return null

    prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/movements/offender/{prisonerNumber}")
          .queryParam("movementsAfter", date)
          .build(prisonerNumber)
      }
      .retrieve()
      .bodyToMono(typeReference<List<Movement>>())
      .block()?.filter { it.movementDate == date }?.maxByOrNull { it.movementDateTime() }?.movementType
  }
}

// Overriding due to deserialisation issues from generated type. Only including fields we are interested in.
data class ScheduledEvent(val eventId: Long)

// Overriding due to deserialisation issues from generated type. Only including fields we are interested in.
data class PrisonerSchedule(
  val offenderNo: String,
  val locationId: Long,
  val firstName: String,
  val lastName: String,
  val eventId: Long,
  val event: String,
  val startTime: LocalDateTime,
  val endTime: LocalDateTime?,
) {
  fun isTheSameAppointmentType(appointmentType: SupportedAppointmentTypes.Type) = event == appointmentType.code

  fun appointmentCode() = event

  fun isTheSameTime(appointment: PrisonAppointment) = startTime == appointment.appointmentDate.atTime(appointment.startTime) &&
    appointment.appointmentDate.atTime(appointment.endTime) == endTime

  fun isTheSameTime(bha: BookingHistoryAppointment) = startTime == bha.appointmentDate.atTime(bha.startTime) && bha.appointmentDate.atTime(bha.endTime) == endTime
}

data class ScheduledAppointment(
  val id: Long,
  val agencyId: String,
  val locationId: Long,
  val locationDescription: String,
  val appointmentTypeCode: String,
  val appointmentTypeDescription: String,
  val startTime: LocalDateTime,
  val endTime: LocalDateTime?,
  val offenderNo: String,
  val firstName: String,
  val lastName: String,
  val createUserId: String,
  val eventStatus: String,
) {
  fun isTheSameAppointmentType(appointmentType: SupportedAppointmentTypes.Type) = appointmentTypeCode == appointmentType.code

  fun isTheSameTime(appointment: PrisonAppointment) = startTime == appointment.appointmentDate.atTime(appointment.startTime) &&
    appointment.appointmentDate.atTime(appointment.endTime) == endTime
}

fun Movement.movementDateTime(): LocalDateTime = movementDate.atTime(LocalTime.parse(movementTime))
