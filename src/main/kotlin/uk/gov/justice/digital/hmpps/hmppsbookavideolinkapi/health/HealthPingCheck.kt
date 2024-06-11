package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.health

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.health.HealthPingCheck

@Component("activitiesAppointmentsApi")
class ActivitiesAppointmentsHealthPingCheck(@Qualifier("activitiesAppointmentsApiHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)

@Component("hmppsAuth")
class HmppsAuthHealthPingCheck(@Qualifier("hmppsAuthHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)

@Component("locationsInsidePrisonApi")
class LocationsInsidePrisonApiHealthPingCheck(
  @Qualifier("locationsInsidePrisonApiHealthWebClient") webClient: WebClient,
) : HealthPingCheck(webClient)

@Component("manageUsersApi")
class ManageUsersHealthPingCheck(@Qualifier("manageUsersApiHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)

@Component("prisonApi")
class PrisonApiHealthPingCheck(@Qualifier("prisonApiHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)

@Component("prisonerSearchApi")
class PrisonerSearchApiHealthPingCheck(@Qualifier("prisonerSearchApiHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)
