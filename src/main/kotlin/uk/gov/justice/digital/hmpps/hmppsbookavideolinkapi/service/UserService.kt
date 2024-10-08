package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.UserDetailsDto.AuthSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.isEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ProbationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import java.util.Objects

const val COURT_USER_GROUP_CODE = "VIDEO_LINK_COURT_USER"
const val PROBATION_USER_GROUP_CODE = "VIDEO_LINK_PROBATION_USER"

@Service
class UserService(
  private val manageUsersClient: ManageUsersClient,
  private val courtRepository: CourtRepository,
  private val probationTeamRepository: ProbationTeamRepository,
) {

  companion object {

    private enum class ServiceName {
      BOOK_A_VIDEO_LINK_SERVICE,
    }

    private val serviceUser = ServiceUser(
      ServiceName.BOOK_A_VIDEO_LINK_SERVICE.name,
      ServiceName.BOOK_A_VIDEO_LINK_SERVICE.name,
    )

    fun getServiceAsUser() = serviceUser

    fun getClientAsUser(clientId: String) = ServiceUser(username = clientId, name = clientId)
  }

  fun getUser(username: String): User? =
    manageUsersClient.getUsersDetails(username)?.let { userDetails ->
      when (userDetails.authSource) {
        AuthSource.nomis -> {
          PrisonUser(
            username = username,
            name = userDetails.name,
            email = if (username.isEmail()) username.lowercase() else manageUsersClient.getUsersEmail(username)?.email?.lowercase(),
            activeCaseLoadId = userDetails.activeCaseLoadId,
          )
        }

        AuthSource.auth -> {
          val userGroups = manageUsersClient.getUsersGroups(userDetails.userId)
          val isCourtUser = userGroups.any { it.groupCode == COURT_USER_GROUP_CODE }
          val isProbationUser = userGroups.any { it.groupCode == PROBATION_USER_GROUP_CODE }
          val courts = if (isCourtUser) courtRepository.findCourtsByUsername(username).map { it.code }.toSet() else emptySet()
          val probationTeams = if (isProbationUser) probationTeamRepository.findProbationTeamsByUsername(username).map { it.code }.toSet() else emptySet()

          ExternalUser(
            username = username,
            name = userDetails.name,
            email = if (username.isEmail()) username.lowercase() else manageUsersClient.getUsersEmail(username)?.email?.lowercase(),
            isCourtUser = isCourtUser,
            isProbationUser = isProbationUser,
            courts = courts,
            probationTeams = probationTeams,
          )
        }

        else -> throw AccessDeniedException("Users with auth source ${userDetails.authSource} are not supported by this service")
      }
    }
}

abstract class User(
  val username: String,
  private val userType: UserType,
  val name: String,
) {
  fun isUserType(vararg types: UserType) = types.contains(userType)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as User

    if (username != other.username) return false
    if (userType != other.userType) return false
    if (name != other.name) return false

    return true
  }

  override fun hashCode() = Objects.hash(username, userType, name)
}

class PrisonUser(
  val email: String? = null,
  val activeCaseLoadId: String? = null,
  username: String,
  name: String,
) : User(username, UserType.PRISON, name) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as PrisonUser

    if (email != other.email) return false
    if (activeCaseLoadId != other.activeCaseLoadId) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + (email?.hashCode() ?: 0)
    result = 31 * result + (activeCaseLoadId?.hashCode() ?: 0)
    return result
  }
}

class ExternalUser(
  val email: String? = null,
  val isCourtUser: Boolean = false,
  val isProbationUser: Boolean = false,
  private val courts: Set<String> = emptySet(),
  private val probationTeams: Set<String> = emptySet(),
  username: String,
  name: String,
) : User(username, UserType.EXTERNAL, name) {

  init {
    require(isCourtUser || isProbationUser) {
      "External user must be a court or probation user"
    }
  }

  fun hasAccessTo(court: Court) = courts.any { it == court.code }

  fun hasAccessTo(probationTeam: ProbationTeam) = probationTeams.any { it == probationTeam.code }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as ExternalUser

    if (email != other.email) return false
    if (isCourtUser != other.isCourtUser) return false
    if (isProbationUser != other.isProbationUser) return false
    if (courts != other.courts) return false
    if (probationTeams != other.probationTeams) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + (email?.hashCode() ?: 0)
    result = 31 * result + isCourtUser.hashCode()
    result = 31 * result + isProbationUser.hashCode()
    result = 31 * result + courts.hashCode()
    result = 31 * result + probationTeams.hashCode()
    return result
  }
}

class ServiceUser(username: String, name: String) : User(username, UserType.SERVICE, name)

enum class UserType {
  EXTERNAL,
  PRISON,
  SERVICE,
}
