package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments

import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentCancelRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSearchRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSeriesCreateRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toHourMinuteStyle
import java.time.LocalDate
import java.time.LocalTime

const val VIDEO_LINK_BOOKING = "VLB"

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

const val CANCELLED_BY_EXTERNAL_SERVICE = 4L

@Component
class ActivitiesAppointmentsClient(private val activitiesAppointmentsApiWebClient: WebClient) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

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
    comments: String,
  ): AppointmentSeries? =
    activitiesAppointmentsApiWebClient.post()
      .uri("/appointment-series")
      .bodyValue(
        AppointmentSeriesCreateRequest(
          appointmentType = AppointmentSeriesCreateRequest.AppointmentType.INDIVIDUAL,
          prisonCode = prisonCode,
          prisonerNumbers = listOf(prisonerNumber),
          categoryCode = VIDEO_LINK_BOOKING,
          tierCode = AppointmentSeriesCreateRequest.TierCode.TIER_1,
          inCell = false,
          startDate = startDate,
          startTime = startTime.toHourMinuteStyle(),
          endTime = endTime.toHourMinuteStyle(),
          internalLocationId = internalLocationId,
          extraInformation = comments,
        ),
      )
      .retrieve()
      .bodyToMono(AppointmentSeries::class.java)
      .block()

  fun getPrisonersAppointmentsAtLocations(prisonCode: String, prisonerNumber: String, onDate: LocalDate, vararg locationIds: Long) =
    if (locationIds.isNotEmpty()) {
      log.info("A&A CLIENT: query params - prisonCode=$prisonCode, prisonerNumber=$prisonerNumber, onDate=$onDate, locationIds=${locationIds.toList()}")
      getPrisonersAppointments(prisonCode, prisonerNumber, onDate)
        .also { log.info("A&A CLIENT: matches pre-location filter: $it") }
        .filter { locationIds.toList().contains(it.internalLocation?.id) }
        .also { log.info("A&A CLIENT: matches post-location filter: $it") }
    } else {
      emptyList()
    }

  private fun getPrisonersAppointments(prisonCode: String, prisonerNumber: String, onDate: LocalDate): List<AppointmentSearchResult> =
    activitiesAppointmentsApiWebClient.post()
      .uri("/appointments/{prisonCode}/search", prisonCode)
      .bodyValue(
        AppointmentSearchRequest(
          appointmentType = AppointmentSearchRequest.AppointmentType.INDIVIDUAL,
          startDate = onDate,
          endDate = onDate,
          categoryCode = VIDEO_LINK_BOOKING,
          prisonerNumbers = listOf(prisonerNumber),
        ),
      )
      .retrieve()
      .bodyToMono(typeReference<List<AppointmentSearchResult>>())
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block() ?: emptyList()

  /**
   * @param appointmentId refers the appointment identifier held in Activities and Appointments, not BVLS.
   */
  fun cancelAppointment(appointmentId: Long) {
    activitiesAppointmentsApiWebClient.put()
      .uri("/appointments/{appointmentId}/cancel", appointmentId)
      .bodyValue(
        AppointmentCancelRequest(
          cancellationReasonId = CANCELLED_BY_EXTERNAL_SERVICE,
          applyTo = AppointmentCancelRequest.ApplyTo.THIS_APPOINTMENT,
        ),
      )
      .retrieve()
      .bodyToMono(AppointmentSeries::class.java)
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()
  }
}
