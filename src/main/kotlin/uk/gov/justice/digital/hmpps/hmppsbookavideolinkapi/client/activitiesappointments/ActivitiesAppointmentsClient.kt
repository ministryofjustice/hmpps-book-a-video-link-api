package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments

import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.SupportedAppointmentTypes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toHourMinuteStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.CacheConfiguration
import java.time.LocalDate
import java.time.LocalTime

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

// These constants map to values in the A&A DB appointment cancellation reasons.
const val CANCELLED_BY_EXTERNAL_SERVICE = 4L
const val CANCELLED_BY_USER = 2L

@Component
class ActivitiesAppointmentsClient(private val activitiesAppointmentsApiWebClient: WebClient) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Cacheable(CacheConfiguration.ROLLED_OUT_PRISONS_CACHE_NAME)
  fun isAppointmentsRolledOutAt(prisonCode: String) = activitiesAppointmentsApiWebClient
    .get()
    .uri("/rollout/{prisonCode}", prisonCode)
    .retrieve()
    .bodyToMono(RolloutPrisonPlan::class.java)
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()?.prisonLive == true

  fun createAppointment(
    prisonCode: String,
    prisonerNumber: String,
    startDate: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
    internalLocationId: Long,
    comments: String?,
    appointmentType: SupportedAppointmentTypes.Type,
  ): AppointmentSeries? = activitiesAppointmentsApiWebClient.post()
    .uri("/appointment-series")
    .bodyValue(
      AppointmentSeriesCreateRequest(
        appointmentType = AppointmentSeriesCreateRequest.AppointmentType.INDIVIDUAL,
        prisonCode = prisonCode,
        prisonerNumbers = listOf(prisonerNumber),
        categoryCode = appointmentType.code,
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

  /**
   * Returns all matching appointment (types) for a prisoner, not just video link bookings.
   */
  fun getPrisonersAppointmentsAtLocations(prisonCode: String, prisonerNumber: String, onDate: LocalDate, vararg locationIds: Long) = if (locationIds.isNotEmpty()) {
    log.info("A&A CLIENT: query params - prisonCode=$prisonCode, prisonerNumber=$prisonerNumber, onDate=$onDate, locationIds=${locationIds.toList()}")
    getPrisonersAppointments(prisonCode, prisonerNumber, onDate)
      .also { log.info("A&A CLIENT: matches pre-location filter: $it") }
      .filter { locationIds.toList().contains(it.internalLocation?.id) }
      .also { log.info("A&A CLIENT: matches post-location filter: $it") }
  } else {
    emptyList()
  }

  private fun getPrisonersAppointments(prisonCode: String, prisonerNumber: String, onDate: LocalDate) = activitiesAppointmentsApiWebClient.post()
    .uri("/appointments/{prisonCode}/search", prisonCode)
    .bodyValue(
      AppointmentSearchRequest(
        startDate = onDate,
        prisonerNumbers = listOf(prisonerNumber),
      ),
    )
    .retrieve()
    .bodyToMono(typeReference<List<AppointmentSearchResult>>())
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block() ?: emptyList()

  /**
   * @param appointmentId refers the appointment identifier held in Activities and Appointments, not BVLS.
   * @param deleteOnCancel true will hard delete the appointment whereas a soft delete will keep a history of the cancellation.
   */
  fun cancelAppointment(appointmentId: Long, deleteOnCancel: Boolean = false) {
    activitiesAppointmentsApiWebClient.put()
      .uri("/appointments/{appointmentId}/cancel", appointmentId)
      .bodyValue(
        AppointmentCancelRequest(
          cancellationReasonId = if (deleteOnCancel) CANCELLED_BY_EXTERNAL_SERVICE else CANCELLED_BY_USER,
          applyTo = AppointmentCancelRequest.ApplyTo.THIS_APPOINTMENT,
        ),
      )
      .retrieve()
      .bodyToMono(AppointmentSeries::class.java)
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()
  }

  /**
   * Returns all matching appointment types for a prison, not just video link bookings. It will however filter down to
   * the supplied location IDs where there is match.
   */
  fun getScheduledAppointments(prisonCode: String, onDate: LocalDate, locationIds: Collection<Long>) = if (locationIds.isNotEmpty()) {
    log.info("A&A CLIENT: query params - prisonCode=$prisonCode, onDate=$onDate, locationIds=${locationIds.toList()}")
    getPrisonAppointments(prisonCode, onDate)
      .also { log.info("A&A CLIENT: matches pre-location filter: $it") }
      .filter { locationIds.toList().contains(it.internalLocation?.id) }
      .also { log.info("A&A CLIENT: matches post-location filter: $it") }
  } else {
    emptyList()
  }

  private fun getPrisonAppointments(prisonCode: String, onDate: LocalDate) = activitiesAppointmentsApiWebClient.post()
    .uri("/appointments/{prisonCode}/search", prisonCode)
    .bodyValue(AppointmentSearchRequest(startDate = onDate))
    .retrieve()
    .bodyToMono(typeReference<List<AppointmentSearchResult>>())
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block() ?: emptyList()
}
