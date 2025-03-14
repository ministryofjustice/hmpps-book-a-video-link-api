package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers

import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.EmailAddressDto
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.UserDetailsDto
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.UserGroup

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

@Component
class ManageUsersClient(private val manageUsersApiWebClient: WebClient) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getUsersDetails(username: String): UserDetailsDto? = manageUsersApiWebClient
    .get()
    .uri("/users/{username}", username)
    .retrieve()
    .bodyToMono(UserDetailsDto::class.java)
    .doOnError { error -> log.info("Error looking up user details by username $username in manage users client", error) }
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()

  fun getUsersEmail(username: String): EmailAddressDto? = manageUsersApiWebClient
    .get()
    .uri("/users/{username}/email?unverified=false", username)
    .retrieve()
    .bodyToMono(EmailAddressDto::class.java)
    .doOnError { error -> log.info("Error looking up users email by username $username in manage users client", error) }
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()?.takeIf(EmailAddressDto::verified)

  fun getUsersGroups(userId: String): List<UserGroup> = manageUsersApiWebClient
    .get()
    .uri("/externalusers/{userId}/groups", userId)
    .retrieve()
    .bodyToMono(typeReference<List<UserGroup>>())
    .doOnError { error -> log.info("Error looking up users groups by user id $userId in manage users client", error) }
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block() ?: emptyList()
}
