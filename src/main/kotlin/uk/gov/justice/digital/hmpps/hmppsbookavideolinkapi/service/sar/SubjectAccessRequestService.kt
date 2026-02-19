package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.sar

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.SubjectAccessRequestRepository
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate

/**
 * Prisoners have the right to access and receive a copy of their personal data and other supplementary information.
 *
 * This is commonly referred to as a subject access request or ‘SAR’.
 *
 * The purpose of this service is to surface all relevant prisoner-specific information for a subject access request.
 *
 * By extending HmppsPrisonSubjectAccessRequestService the endpoint needed for this is automatically included, no
 * additional controller (endpoint) is required.
 */
@Service
@Transactional(readOnly = true)
class SubjectAccessRequestService(private val repository: SubjectAccessRequestRepository) : HmppsPrisonSubjectAccessRequestService {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun getPrisonContentFor(prn: String, fromDate: LocalDate?, toDate: LocalDate?): HmppsSubjectAccessRequestContent? = run {
    val from = fromDate ?: LocalDate.EPOCH
    val to = toDate ?: LocalDate.now()

    log.info("SAR: processing subject access request for prisoner $prn, from $from to $to.")

    val matchingPrisonerData = repository.findBy(prn, from, to)

    if (matchingPrisonerData.isEmpty()) {
      log.info("SAR: no matches found for prisoner $prn, from $from to $to.")

      return null
    }

    log.info("SAR: matches found for prisoner $prn, from $from to $to.")

    HmppsSubjectAccessRequestContent(content = matchingPrisonerData)
  }
}
