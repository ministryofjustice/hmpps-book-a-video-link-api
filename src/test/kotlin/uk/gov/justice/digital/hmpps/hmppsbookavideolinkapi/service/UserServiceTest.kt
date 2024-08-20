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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.userDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.userEmailAddress
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService.Companion.getClientAsUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService.Companion.getServiceAsUser

class UserServiceTest {

  private val manageUsersClient: ManageUsersClient = mock()
  private val userService = UserService(manageUsersClient)

  @Test
  fun `getServiceAsUser should return ContactDetails for valid email username`() {
    with(getServiceAsUser()) {
      username isEqualTo "BOOK_A_VIDEO_LINK_SERVICE"
      name isEqualTo "BOOK_A_VIDEO_LINK_SERVICE"
      isUserType(UserType.SERVICE) isBool true
      email isEqualTo null
    }
  }

  @Test
  fun `getClientAsUser should return ContactDetails for valid email username`() {
    with(getClientAsUser("client")) {
      username isEqualTo "client"
      name isEqualTo "client"
      isUserType(UserType.SERVICE) isBool true
      email isEqualTo null
    }
  }

  @Test
  fun `getUser should return prison user when authSource is nomis`() {
    whenever(manageUsersClient.getUsersDetails("testUser")) doReturn userDetails("testUser", "Test User", authSource = AuthSource.nomis, activeCaseLoadId = BIRMINGHAM)

    userService.getUser("testUser") isEqualTo User(username = "testUser", userType = UserType.PRISON, name = "Test User", activeCaseLoadId = BIRMINGHAM)
  }

  @Test
  fun `getUser should return probation user when authSource is auth and group code is probation user`() {
    val userDetails = userDetails("testUser", "Test User", authSource = AuthSource.auth, activeCaseLoadId = BIRMINGHAM)

    whenever(manageUsersClient.getUsersDetails(userDetails.username)) doReturn userDetails
    whenever(manageUsersClient.getUsersGroups(userDetails.userId)) doReturn listOf(UserGroup(groupCode = PROBATION_USER, "group name"))

    userService.getUser("testUser") isEqualTo User(username = userDetails.username, userType = UserType.EXTERNAL, name = "Test User", isProbationUser = true)
  }

  @Test
  fun `getUser should return court user when authSource is auth and group code is court user`() {
    val userDetails = userDetails("testUser", "Test User", authSource = AuthSource.auth, activeCaseLoadId = BIRMINGHAM)

    whenever(manageUsersClient.getUsersDetails(userDetails.username)) doReturn userDetails
    whenever(manageUsersClient.getUsersGroups(userDetails.userId)) doReturn listOf(UserGroup(groupCode = COURT_USER, "group name"))

    userService.getUser("testUser") isEqualTo User(username = userDetails.username, userType = UserType.EXTERNAL, name = "Test User", isCourtUser = true)
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
    whenever(manageUsersClient.getUsersGroups(userDetails.userId)) doReturn listOf(UserGroup(groupCode = COURT_USER, "group name"))

    userService.getUser(username)?.email isEqualTo "test@example.com"
  }

  @Test
  fun `getUser should set external users email from manageUsersClient when username is not an email`() {
    val username = "testUser"
    val userDetails = userDetails(username, "Test User")
    whenever(manageUsersClient.getUsersDetails("testUser")) doReturn userDetails
    whenever(manageUsersClient.getUsersEmail(username)) doReturn userEmailAddress(username, "test@example.com")
    whenever(manageUsersClient.getUsersGroups(userDetails.userId)) doReturn listOf(UserGroup(groupCode = COURT_USER, "group name"))

    userService.getUser(username)?.email isEqualTo "test@example.com"
  }

  @Test
  fun `getUser should set nomis users email from manageUsersClient when username is not an email`() {
    val username = "testUser"
    val userDetails = userDetails(username, "Test User", authSource = AuthSource.nomis)
    whenever(manageUsersClient.getUsersDetails("testUser")) doReturn userDetails
    whenever(manageUsersClient.getUsersEmail(username)) doReturn userEmailAddress(username, "test@example.com")

    userService.getUser(username)?.email isEqualTo "test@example.com"
  }

  @Test
  fun `should create court and probation users`() {
    User(username = "username", name = "name", userType = UserType.EXTERNAL, isCourtUser = true, isProbationUser = false)
    User(username = "username", name = "name", userType = UserType.EXTERNAL, isCourtUser = false, isProbationUser = true)
  }

  @Test
  fun `should create prison users`() {
    User(username = "username", name = "name", userType = UserType.PRISON)
    User(username = "username", name = "name", userType = UserType.PRISON, activeCaseLoadId = BIRMINGHAM)
  }

  @Test
  fun `should fail to create court or probation user`() {
    assertThrows<IllegalArgumentException> {
      User(username = "username", name = "name", userType = UserType.EXTERNAL, isCourtUser = false, isProbationUser = false)
    }.message isEqualTo "External user must be a court or probation user"

    assertThrows<IllegalArgumentException> {
      User(username = "username", name = "name", userType = UserType.PRISON, isCourtUser = true, isProbationUser = true)
    }.message isEqualTo "Only external users can be court or probation users"

    assertThrows<IllegalArgumentException> {
      User(username = "username", name = "name", userType = UserType.SERVICE, isCourtUser = true, isProbationUser = true)
    }.message isEqualTo "Only external users can be court or probation users"
  }
}
