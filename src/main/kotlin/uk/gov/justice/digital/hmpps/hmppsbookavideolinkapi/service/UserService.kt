package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.isEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactDetails

@Service
class UserService(private val manageUsersClient: ManageUsersClient) {

  fun getContactDetails(username: String): ContactDetails? {
    val mayBeUserDetails = manageUsersClient.getUsersDetails(username) ?: return null

    return if (username.isEmail()) {
      ContactDetails(name = mayBeUserDetails.name, email = username)
    } else {
      return ContactDetails(name = mayBeUserDetails.name, email = manageUsersClient.getUsersEmail(username)?.email)
    }
  }
}
