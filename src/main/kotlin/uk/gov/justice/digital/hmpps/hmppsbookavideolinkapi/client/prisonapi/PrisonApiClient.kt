package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.model.Event
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.model.NewAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDateTime
import java.time.LocalDate
import java.time.LocalTime

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
    appointmentType: String,
    locationId: Long,
    appointmentDate: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
    comments: String? = null,
  ): Event? =
    prisonApiWebClient
      .post()
      .uri("/bookings/{bookingId}/appointments", bookingId)
      .header("no-event-propagation", true.toString())
      .bodyValue(
        NewAppointment(
          appointmentType = appointmentType,
          locationId = locationId,
          startTime = appointmentDate.atTime(startTime).toIsoDateTime(),
          endTime = appointmentDate.atTime(endTime).toIsoDateTime(),
          comment = comments,
        ),
      )
      .retrieve()
      .bodyToMono(Event::class.java)
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()
}
