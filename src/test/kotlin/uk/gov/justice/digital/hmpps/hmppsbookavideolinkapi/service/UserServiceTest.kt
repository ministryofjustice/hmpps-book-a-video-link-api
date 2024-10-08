package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.security.access.AccessDeniedException
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.UserDetailsDto.AuthSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.UserGroup
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.userDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.userEmailAddress
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService.Companion.getClientAsUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService.Companion.getServiceAsUser

class UserServiceTest {

  private val manageUsersClient: ManageUsersClient = mock()
  private val courtRepository: CourtRepository = mock()
  private val probationTeamRepository: ProbationTeamRepository = mock()
  private val userService = UserService(manageUsersClient, courtRepository, probationTeamRepository)

  @Test
  fun `getServiceAsUser should return ContactDetails for valid email username`() {
    with(getServiceAsUser()) {
      username isEqualTo "BOOK_A_VIDEO_LINK_SERVICE"
      name isEqualTo "BOOK_A_VIDEO_LINK_SERVICE"
      isUserType(UserType.SERVICE) isBool true
    }
  }

  @Test
  fun `getClientAsUser should return ContactDetails for valid email username`() {
    with(getClientAsUser("client")) {
      username isEqualTo "client"
      name isEqualTo "client"
      isUserType(UserType.SERVICE) isBool true
    }
  }

  @Test
  fun `getUser should return prison user when authSource is nomis`() {
    whenever(manageUsersClient.getUsersDetails("testUser")) doReturn userDetails("testUser", "Test User", authSource = AuthSource.nomis, activeCaseLoadId = BIRMINGHAM)

    userService.getUser("testUser") as PrisonUser isEqualTo PrisonUser(username = "testUser", name = "Test User", activeCaseLoadId = BIRMINGHAM)
  }

  @Test
  fun `getUser should return probation user when authSource is auth and group code is probation user`() {
    val userDetails = userDetails("testUser", "Test User", authSource = AuthSource.auth)
    val probationTeam = probationTeam()

    whenever(manageUsersClient.getUsersDetails(userDetails.username)) doReturn userDetails
    whenever(manageUsersClient.getUsersGroups(userDetails.userId)) doReturn listOf(UserGroup(groupCode = PROBATION_USER_GROUP_CODE, "group name"))
    whenever(probationTeamRepository.findProbationTeamsByUsername(userDetails.username)) doReturn listOf(probationTeam)

    val probationUser = userService.getUser("testUser") as ExternalUser
    probationUser isEqualTo ExternalUser(username = userDetails.username, name = "Test User", isProbationUser = true, probationTeams = setOf(probationTeam.code))
    probationUser.hasAccessTo(probationTeam) isBool true
    probationUser.hasAccessTo(probationTeam(code = "NO_ACCESS")) isBool false
  }

  @Test
  fun `getUser should return court user when authSource is auth and group code is court user`() {
    val userDetails = userDetails("testUser", "Test User", authSource = AuthSource.auth)
    val court = court()

    whenever(manageUsersClient.getUsersDetails(userDetails.username)) doReturn userDetails
    whenever(manageUsersClient.getUsersGroups(userDetails.userId)) doReturn listOf(UserGroup(groupCode = COURT_USER_GROUP_CODE, "group name"))
    whenever(courtRepository.findCourtsByUsername(userDetails.username)) doReturn listOf(court)

    val courtUser = userService.getUser("testUser") as ExternalUser
    courtUser isEqualTo ExternalUser(username = userDetails.username, name = "Test User", isCourtUser = true, courts = setOf(court.code))
    courtUser.hasAccessTo(court) isBool true
    courtUser.hasAccessTo(court(code = "NO_ACCESS")) isBool false
  }

  @Test
  fun `getUser should throw AccessDeniedException for unsupported authSource`() {
    whenever(manageUsersClient.getUsersDetails("testUser")) doReturn userDetails("testUser", "Test User", authSource = AuthSource.delius)

    val exception = assertThrows<AccessDeniedException> { userService.getUser("testUser") }

    exception.message isEqualTo "Users with auth source delius are not supported by this service"
  }

  @Test
  fun `getUser should return null when the user is not found`() {
    whenever(manageUsersClient.getUsersDetails("testUser")) doReturn null

    userService.getUser("testUser") isEqualTo null
  }

  @Test
  fun `getUser should set external users email when username is an email`() {
    val username = "test@example.com"
    val userDetails = userDetails(username, "Test User")
    whenever(manageUsersClient.getUsersDetails(username)) doReturn userDetails
    whenever(manageUsersClient.getUsersGroups(userDetails.userId)) doReturn listOf(UserGroup(groupCode = COURT_USER_GROUP_CODE, "group name"))

    (userService.getUser(username) as ExternalUser).email isEqualTo "test@example.com"
  }

  @Test
  fun `getUser should set external users email from manageUsersClient when username is not an email`() {
    val username = "testUser"
    val userDetails = userDetails(username, "Test User")
    whenever(manageUsersClient.getUsersDetails("testUser")) doReturn userDetails
    whenever(manageUsersClient.getUsersEmail(username)) doReturn userEmailAddress(username, "test@example.com")
    whenever(manageUsersClient.getUsersGroups(userDetails.userId)) doReturn listOf(UserGroup(groupCode = COURT_USER_GROUP_CODE, "group name"))

    (userService.getUser(username) as ExternalUser).email isEqualTo "test@example.com"
  }

  @Test
  fun `getUser should set nomis users email from manageUsersClient when username is not an email`() {
    val username = "testUser"
    val userDetails = userDetails(username, "Test User", authSource = AuthSource.nomis)
    whenever(manageUsersClient.getUsersDetails("testUser")) doReturn userDetails
    whenever(manageUsersClient.getUsersEmail(username)) doReturn userEmailAddress(username, "test@example.com")

    (userService.getUser(username) as PrisonUser).email isEqualTo "test@example.com"
  }

  @Test
  fun `should create court and probation users`() {
    ExternalUser(username = "username", name = "name", isCourtUser = true, isProbationUser = false)
    ExternalUser(username = "username", name = "name", isCourtUser = false, isProbationUser = true)
  }

  @Test
  fun `should create prison users`() {
    PrisonUser(username = "username", name = "name")
    PrisonUser(username = "username", name = "name", activeCaseLoadId = BIRMINGHAM)
  }

  @Test
  fun `should fail to create court or probation user`() {
    assertThrows<IllegalArgumentException> {
      ExternalUser(username = "username", name = "name", isCourtUser = false, isProbationUser = false)
    }.message isEqualTo "External user must be a court or probation user"
  }
}
