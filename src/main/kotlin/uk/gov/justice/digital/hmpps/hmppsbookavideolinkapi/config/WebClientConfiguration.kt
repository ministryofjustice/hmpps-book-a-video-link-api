package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @Value("\${api.base.url.activities-appointments}") val activitiesAppointmentsApiBaseUri: String,
  @Value("\${api.base.url.hmpps-auth}") val hmppsAuthBaseUri: String,
  @Value("\${api.base.url.locations-inside-prison}") val locationsInsidePrisonApiBaseUri: String,
  @Value("\${api.base.url.manage-users}") private val manageUsersBaseUri: String,
  @Value("\${api.base.url.prison-api}") val prisonApiBaseUri: String,
  @Value("\${api.base.url.prisoner-search}") val prisonerSearchBaseUri: String,
  @Value("\${api.base.url.whereabouts}") val whereaboutsBaseUri: String,
  @Value("\${api.base.url.nomis-mapping}") val nomisMappingBaseUri: String,
  @Value("\${api.health-timeout:2s}") val healthTimeout: Duration,
  @Value("\${api.timeout:60s}") val timeout: Duration,
) {
  @Bean
  fun activitiesAppointmentsApiHealthWebClient(builder: WebClient.Builder) = builder.healthWebClient(activitiesAppointmentsApiBaseUri, healthTimeout)

  @Bean
  fun activitiesAppointmentsApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder) =
    builder.authorisedWebClient(authorizedClientManager, "activities-appointments", activitiesAppointmentsApiBaseUri, timeout)

  @Bean
  fun hmppsAuthHealthWebClient(builder: WebClient.Builder) = builder.healthWebClient(hmppsAuthBaseUri, healthTimeout)

  @Bean
  fun locationsInsidePrisonApiHealthWebClient(builder: WebClient.Builder) = builder.healthWebClient(locationsInsidePrisonApiBaseUri, healthTimeout)

  @Bean
  fun locationsInsidePrisonApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder) =
    builder.authorisedWebClient(authorizedClientManager, "locations-inside-prison", locationsInsidePrisonApiBaseUri, timeout)

  @Bean
  fun manageUsersApiHealthWebClient(builder: WebClient.Builder): WebClient =
    builder.healthWebClient(manageUsersBaseUri, healthTimeout)

  @Bean
  fun manageUsersApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder) =
    builder.authorisedWebClient(authorizedClientManager, "manage-users", manageUsersBaseUri, timeout)

  @Bean
  fun nomisMappingApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder) =
    builder.authorisedWebClient(authorizedClientManager, "nomis-mapping-api", nomisMappingBaseUri, timeout)

  @Bean
  fun nomisMappingApiHealthWebClient(builder: WebClient.Builder) = builder.healthWebClient(nomisMappingBaseUri, healthTimeout)

  @Bean
  fun prisonApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder) =
    builder.authorisedWebClient(authorizedClientManager, "prisoner-api", prisonApiBaseUri, timeout)

  @Bean
  fun prisonApiHealthWebClient(builder: WebClient.Builder) = builder.healthWebClient(prisonApiBaseUri, healthTimeout)

  @Bean
  fun prisonerSearchApiHealthWebClient(builder: WebClient.Builder) = builder.healthWebClient(prisonerSearchBaseUri, healthTimeout)

  @Bean
  fun prisonerSearchApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder) =
    builder.authorisedWebClient(authorizedClientManager, "prisoner-search", prisonerSearchBaseUri, timeout)

  @Bean
  fun whereaboutsApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder) =
    builder.authorisedWebClient(authorizedClientManager, "whereabouts-api", whereaboutsBaseUri, timeout)

  @Bean
  fun whereaboutsApiHealthWebClient(builder: WebClient.Builder) = builder.healthWebClient(whereaboutsBaseUri, healthTimeout)
}
