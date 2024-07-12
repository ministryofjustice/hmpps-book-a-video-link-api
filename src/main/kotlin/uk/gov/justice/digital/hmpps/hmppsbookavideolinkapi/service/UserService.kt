package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.UserDetailsDto.AuthSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.isEmail

@Service
class UserService(private val manageUsersClient: ManageUsersClient) {

  companion object {
    private enum class ServiceName {
      BOOK_A_VIDEO_LINK_SERVICE,
    }

    fun getServiceAsUser() = User(
      username = ServiceName.BOOK_A_VIDEO_LINK_SERVICE.name,
      userType = UserType.SERVICE,
      name = ServiceName.BOOK_A_VIDEO_LINK_SERVICE.name,
    )

    fun getClientAsUser(clientId: String) = User(
      username = clientId,
      userType = UserType.SERVICE,
      name = clientId,
    )
  }

  fun getUser(username: String): User? {
    val userDetails = manageUsersClient.getUsersDetails(username) ?: return null
    val userType = when (userDetails.authSource) {
      AuthSource.nomis -> UserType.PRISON
      AuthSource.auth -> UserType.EXTERNAL
      else -> throw AccessDeniedException("Users with auth source ${userDetails.authSource} are not supported by this service")
    }

    return User(
      username = username,
      userType = userType,
      name = userDetails.name,
      email = if (username.isEmail()) username.lowercase() else manageUsersClient.getUsersEmail(username)?.email,
    )
  }
}

data class User(val username: String, val userType: UserType, val name: String, val email: String? = null) {
  fun isPrisonUser() = userType == UserType.PRISON
  fun isExternalUser() = userType == UserType.EXTERNAL
}

enum class UserType {
  EXTERNAL,
  PRISON,
  SERVICE,
}
