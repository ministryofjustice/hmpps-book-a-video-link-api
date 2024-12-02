package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi

import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.model.NewAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDate
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDateTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

const val VIDEO_LINK_BOOKING = "VLB"

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
  ): ScheduledEvent? =
    prisonApiWebClient
      .post()
      .uri("/api/bookings/{bookingId}/appointments", bookingId)
      .header("no-event-propagation", DO_NOT_PROPAGATE)
      .bodyValue(
        NewAppointment(
          appointmentType = VIDEO_LINK_BOOKING,
          locationId = locationId,
          startTime = appointmentDate.atTime(startTime).toIsoDateTime(),
          endTime = appointmentDate.atTime(endTime).toIsoDateTime(),
          comment = comments,
        ),
      )
      .retrieve()
      .bodyToMono(ScheduledEvent::class.java)
      .block()

  fun getPrisonersAppointmentsAtLocations(prisonCode: String, prisonerNumber: String, onDate: LocalDate, vararg locationIds: Long): List<PrisonerSchedule> =
    if (locationIds.isNotEmpty()) {
      log.info("PRISON-API CLIENT: query params - prisonCode=$prisonCode, prisonerNumber=$prisonerNumber, onDate=$onDate, locationIds=${locationIds.toList()}")
      getPrisonersAppointments(prisonCode, prisonerNumber, onDate)
        .also { log.info("PRISON-API CLIENT: matches pre-location filter: $it") }
        .filter { locationIds.contains(it.locationId) && it.event == VIDEO_LINK_BOOKING }
        .also { log.info("PRISON-API CLIENT matches post-location filter: $it") }
    } else {
      emptyList()
    }

  private fun getPrisonersAppointments(prisonCode: String, prisonerNumber: String, onDate: LocalDate): List<PrisonerSchedule> =
    prisonApiWebClient
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
)
