package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments

import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.Appointment

class ActivitiesAppointmentsClient(private val activitiesAppointmentsApiWebClient: WebClient) {

  fun getAppointment(appointmentId: Long): Appointment? =
    activitiesAppointmentsApiWebClient
      .get()
      .uri("/appointments/{appointmentId}", appointmentId)
      .retrieve()
      .bodyToMono(Appointment::class.java)
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()
}