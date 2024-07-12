package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.security.access.AccessDeniedException
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.UserDetailsDto.AuthSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isNotEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.userDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.userEmailAddress
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService.Companion.getClientAsUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService.Companion.getServiceAsUser

class UserServiceTest {

  private val manageUsersClient: ManageUsersClient = mock()
  private val userService = UserService(manageUsersClient)

  @Test
  fun `getServiceAsUser should return ContactDetails for valid email username`() {
    val result = getServiceAsUser()

    result.username isEqualTo "BOOK_A_VIDEO_LINK_SERVICE"
    result.name isEqualTo "BOOK_A_VIDEO_LINK_SERVICE"
    result.userType isEqualTo UserType.SERVICE
    result.email isEqualTo null
  }

  @Test
  fun `getClientAsUser should return ContactDetails for valid email username`() {
    val result = getClientAsUser("client")

    result.username isEqualTo "client"
    result.name isEqualTo "client"
    result.userType isEqualTo UserType.SERVICE
    result.email isEqualTo null
  }

  @Test
  fun `getUser should return user with PRISON type when authSource is nomis`() {
    val username = "testUser"
    whenever(manageUsersClient.getUsersDetails(username)) doReturn userDetails(username, "Test User", authSource = AuthSource.nomis)

    val user = userService.getUser(username)

    user isNotEqualTo null
    user?.userType isEqualTo UserType.PRISON
    user?.name isEqualTo "Test User"
  }

  @Test
  fun `getUser should return user with EXTERNAL type when authSource is auth`() {
    val username = "testUser"
    whenever(manageUsersClient.getUsersDetails(username)) doReturn userDetails(username, "Test User", authSource = AuthSource.auth)

    val user = userService.getUser(username)

    user isNotEqualTo null
    user?.userType isEqualTo UserType.EXTERNAL
    user?.name isEqualTo "Test User"
  }

  @Test
  fun `getUser should throw AccessDeniedException for unsupported authSource`() {
    val username = "testUser"
    whenever(manageUsersClient.getUsersDetails(username)) doReturn userDetails(username, "Test User", authSource = AuthSource.delius)

    val exception = assertThrows<AccessDeniedException> { userService.getUser(username) }

    exception.message isEqualTo "Users with auth source delius are not supported by this service"
  }

  @Test
  fun `getUser should return null when the user is not found`() {
    val username = "testUser"
    whenever(manageUsersClient.getUsersDetails(username)) doReturn null

    val user = userService.getUser(username)

    user isEqualTo null
  }

  @Test
  fun `getUser should set email when username is an email`() {
    val username = "test@example.com"
    whenever(manageUsersClient.getUsersDetails(username)) doReturn userDetails(username, "Test User")

    val user = userService.getUser(username)

    user isNotEqualTo null
    user?.email isEqualTo "test@example.com"
  }

  @Test
  fun `getUser should set email from manageUsersClient when username is not an email`() {
    val username = "testUser"
    whenever(manageUsersClient.getUsersDetails(username)) doReturn userDetails(username, "Test User")
    whenever(manageUsersClient.getUsersEmail(username)) doReturn userEmailAddress(username, "test@example.com")

    val user = userService.getUser(username)

    user isNotEqualTo null
    user?.email isEqualTo "test@example.com"
  }
}
