package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.EmailAddressDto
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.UserDetailsDto

@Component
class ManageUsersClient(private val manageUsersApiWebClient: WebClient) {

  fun getUsersDetails(username: String): UserDetailsDto? =
    manageUsersApiWebClient
      .get()
      .uri("/users/{username}", username)
      .retrieve()
      .bodyToMono(UserDetailsDto::class.java)
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()

  fun getUsersEmail(username: String): EmailAddressDto? =
    manageUsersApiWebClient
      .get()
      .uri("/users/{username}/email?unverified=false", username)
      .retrieve()
      .bodyToMono(EmailAddressDto::class.java)
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()?.takeIf(EmailAddressDto::verified)
}
