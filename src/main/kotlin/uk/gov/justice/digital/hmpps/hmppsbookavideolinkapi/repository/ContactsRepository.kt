package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Contact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType

/**
 * This repository is read-only and accessed via the view v_all_contacts.
 */
@Repository
interface ContactsRepository : JpaRepository<Contact, Long> {
  fun findContactsByContactTypeAndCodeAndPrimaryContactTrue(contactType: ContactType, code: String): List<Contact>
}
