package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSeriesCreateRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoTime
import java.time.LocalDate
import java.time.LocalTime

const val VIDEO_LINK_BOOKING = "VLB"

@Component
class ActivitiesAppointmentsClient(private val activitiesAppointmentsApiWebClient: WebClient) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getAppointment(appointmentId: Long): Appointment? =
    activitiesAppointmentsApiWebClient
      .get()
      .uri("/appointments/{appointmentId}", appointmentId)
      .retrieve()
      .bodyToMono(Appointment::class.java)
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()

  fun isAppointmentsRolledOutAt(prisonCode: String) =
    activitiesAppointmentsApiWebClient
      .get()
      .uri("/rollout/{prisonCode}", prisonCode)
      .retrieve()
      .bodyToMono(RolloutPrisonPlan::class.java)
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()?.appointmentsRolledOut == true

  fun createAppointment(
    prisonCode: String,
    prisonerNumber: String,
    startDate: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
    internalLocationId: Long,
  ): AppointmentSeries? =
    activitiesAppointmentsApiWebClient.post()
      .uri("/appointment-series")
      .bodyValue(
        // TODO should we be setting the event/appointment organiser e.g. OTHER or EXTERNAL_PROVIDER
        AppointmentSeriesCreateRequest(
          appointmentType = AppointmentSeriesCreateRequest.AppointmentType.INDIVIDUAL,
          prisonCode = prisonCode,
          prisonerNumbers = listOf(prisonerNumber),
          categoryCode = VIDEO_LINK_BOOKING,
          tierCode = AppointmentSeriesCreateRequest.TierCode.TIER_1,
          inCell = false,
          startDate = startDate,
          startTime = startTime.toIsoTime(),
          endTime = endTime.toIsoTime(),
          internalLocationId = internalLocationId,
        ),
      )
      .retrieve()
      .bodyToMono(AppointmentSeries::class.java)
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()
}
