package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi

import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.model.NewAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.model.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDate
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDateTime
import java.time.LocalDate
import java.time.LocalTime

const val VIDEO_LINK_BOOKING = "VLB"

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

@Component
class PrisonApiClient(private val prisonApiWebClient: WebClient) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getInternalLocationByKey(key: String): Location? =
    prisonApiWebClient
      .get()
      .uri("/api/locations/code/{code}", key)
      .retrieve()
      .bodyToMono(Location::class.java)
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()

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
      .header("no-event-propagation", true.toString())
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

  fun getPrisonersAppointments(prisonCode: String, prisonerNumber: String, onDate: LocalDate): List<PrisonerSchedule> =
    prisonApiWebClient
      .post()
      .uri("/api/schedules/{prisonCode}/appointments?date={date}", prisonCode, onDate.toIsoDate())
      .bodyValue(listOf(prisonerNumber))
      .retrieve()
      .bodyToMono(typeReference<List<PrisonerSchedule>>())
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block() ?: emptyList()
}

// Overriding due to deserialisation issues from generated type.
data class ScheduledEvent(val eventId: Long)
