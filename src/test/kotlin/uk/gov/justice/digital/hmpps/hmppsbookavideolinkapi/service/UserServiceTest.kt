package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isNotEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.userDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.userEmail

class UserServiceTest {

  private val manageUsersClient: ManageUsersClient = mock()
  private val userService = UserService(manageUsersClient)

  @Test
  fun `getContactDetails should return ContactDetails for valid email username`() {
    val username = "user@example.com"

    whenever(manageUsersClient.getUsersDetails(username)) doReturn userDetails(username, "Test User Name")

    val result = userService.getContactDetails(username)

    result!! isNotEqualTo null
    result.name isEqualTo "Test User Name"
    result.email isEqualTo username
  }

  @Test
  fun `getContactDetails should return ContactDetails for valid non-email username`() {
    val username = "test_user"

    whenever(manageUsersClient.getUsersDetails(username)) doReturn userDetails(username, "Test User Name")
    whenever(manageUsersClient.getUsersEmail(username)) doReturn userEmail("test_user", "test_user@email.com")

    val result = userService.getContactDetails(username)

    result!! isNotEqualTo null
    result.name isEqualTo "Test User Name"
    result.email isEqualTo "test_user@email.com"
  }

  @Test
  fun `getContactDetails should return null for non-existent username`() {
    val username = "nonexistentuser"

    whenever(manageUsersClient.getUsersDetails(username)) doReturn null

    val result = userService.getContactDetails(username)

    result isEqualTo null
  }
}
