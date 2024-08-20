package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.UserDetailsDto.AuthSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.isEmail

const val COURT_USER_GROUP_CODE = "VIDEO_LINK_COURT_USER"
const val PROBATION_USER_GROUP_CODE = "VIDEO_LINK_PROBATION_USER"

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

  fun getUser(username: String): User? =
    manageUsersClient.getUsersDetails(username)?.let { userDetails ->
      when (userDetails.authSource) {
        AuthSource.nomis -> {
          User(
            username = username,
            userType = UserType.PRISON,
            name = userDetails.name,
            email = if (username.isEmail()) username.lowercase() else manageUsersClient.getUsersEmail(username)?.email?.lowercase(),
            activeCaseLoadId = userDetails.activeCaseLoadId,
          )
        }

        AuthSource.auth -> {
          val userGroups = manageUsersClient.getUsersGroups(userDetails.userId)
          val isCourtUser = userGroups.any { it.groupCode == COURT_USER_GROUP_CODE }
          val isProbationUser = userGroups.any { it.groupCode == PROBATION_USER_GROUP_CODE }

          User(
            username = username,
            userType = UserType.EXTERNAL,
            name = userDetails.name,
            email = if (username.isEmail()) username.lowercase() else manageUsersClient.getUsersEmail(username)?.email?.lowercase(),
            isCourtUser = isCourtUser,
            isProbationUser = isProbationUser,
          )
        }

        else -> throw AccessDeniedException("Users with auth source ${userDetails.authSource} are not supported by this service")
      }
    }
}

data class User(
  val username: String,
  private val userType: UserType,
  val name: String,
  val email: String? = null,
  val isCourtUser: Boolean = false,
  val isProbationUser: Boolean = false,
  val activeCaseLoadId: String? = null,
) {
  init {
    require(userType != UserType.EXTERNAL || (isCourtUser || isProbationUser)) {
      "External user must be a court or probation user"
    }

    require(userType == UserType.EXTERNAL || (!isCourtUser && !isProbationUser)) {
      "Only external users can be court or probation users"
    }
  }

  fun isUserType(vararg types: UserType) = types.contains(userType)
}

enum class UserType {
  EXTERNAL,
  PRISON,
  SERVICE,
}
