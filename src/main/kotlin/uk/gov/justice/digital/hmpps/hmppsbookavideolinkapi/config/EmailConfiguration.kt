package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toMediumFormatStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.AmendedCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.AmendedCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.AmendedCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CancelledCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CancelledCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CancelledCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CourtBookingRequestPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CourtBookingRequestPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CourtBookingRequestUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.GovNotifyEmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ProbationBookingRequestPrisonNoProbationTeamEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ProbationBookingRequestPrisonProbationTeamEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ProbationBookingRequestUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ReleasedCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ReleasedCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ReleasedCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.TransferredCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.TransferredCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.TransferredCourtBookingPrisonNoCourtEmail
import uk.gov.service.notify.NotificationClient
import java.time.LocalDate
import java.util.UUID

/**
 * Email configuration for the service. The sending of all emails is handled by the Gov Notify service.
 *
 * If the API key is present then emails will be sent via the Gov Notify service. If the API key is blank/not present
 * then emails will not be sent.
 */
@Configuration
class EmailConfiguration(
  @Value("\${notify.api.key:}") private val apiKey: String,
  @Value("\${notify.templates.new-court-booking.user:}") private val newCourtBookingUser: String,
  @Value("\${notify.templates.new-court-booking.court:}") private val newCourtBookingCourt: String,
  @Value("\${notify.templates.new-court-booking.prison-court-email:}") private val newCourtBookingPrisonCourtEmail: String,
  @Value("\${notify.templates.new-court-booking.prison-no-court-email:}") private val newCourtBookingPrisonNoCourtEmail: String,
  @Value("\${notify.templates.amended-court-booking.user:}") private val amendedCourtBookingUser: String,
  @Value("\${notify.templates.amended-court-booking.prison-court-email:}") private val amendedCourtBookingPrisonCourtEmail: String,
  @Value("\${notify.templates.amended-court-booking.prison-no-court-email:}") private val amendedCourtBookingPrisonNoCourtEmail: String,
  @Value("\${notify.templates.cancelled-court-booking.user:}") private val cancelledCourtBookingUser: String,
  @Value("\${notify.templates.cancelled-court-booking.prison-court-email:}") private val cancelledCourtBookingPrisonCourtEmail: String,
  @Value("\${notify.templates.cancelled-court-booking.prison-no-court-email:}") private val cancelledCourtBookingPrisonNoCourtEmail: String,
  @Value("\${notify.templates.court-booking-request.user:}") private val courtBookingRequestUser: String,
  @Value("\${notify.templates.court-booking-request.prison-court-email:}") private val courtBookingRequestPrisonCourtEmail: String,
  @Value("\${notify.templates.court-booking-request.prison-no-court-email:}") private val courtBookingRequestPrisonNoCourtEmail: String,
  @Value("\${notify.templates.probation-booking-request.user:}") private val probationBookingRequestUser: String,
  @Value("\${notify.templates.probation-booking-request.prison-probation-team-email:}") private val probationBookingRequestPrisonProbationTeamEmail: String,
  @Value("\${notify.templates.probation-booking-request.prison-no-probation-team-email:}") private val probationBookingRequestPrisonNoProbationTeamEmail: String,
  @Value("\${notify.templates.transfer-court-booking.court:}") private val transferCourtBookingCourt: String,
  @Value("\${notify.templates.transfer-court-booking.prison-court-email:}") private val transferCourtBookingPrisonCourtEmail: String,
  @Value("\${notify.templates.transfer-court-booking.prison-no-court-email:}") private val transferCourtBookingPrisonNoCourtEmail: String,
  @Value("\${notify.templates.release-court-booking.court:}") private val releaseCourtBookingCourt: String,
  @Value("\${notify.templates.release-court-booking.prison-court-email:}") private val releaseCourtBookingPrisonCourtEmail: String,
  @Value("\${notify.templates.release-court-booking.prison-no-court-email:}") private val releaseCourtBookingPrisonNoCourtEmail: String,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Bean
  fun emailService() =
    if (apiKey.isBlank()) {
      EmailService { email -> Result.success(UUID.randomUUID() to "fake template id").also { log.info("Email ${email.javaClass.simpleName} not sent.") } }.also { log.info("Gov Notify emails are disabled") }
    } else {
      GovNotifyEmailService(NotificationClient(apiKey), emailTemplates()).also { log.info("Gov Notify emails are enabled") }
    }

  private fun emailTemplates() = EmailTemplates(
    newCourtBookingUser = newCourtBookingUser,
    newCourtBookingCourt = newCourtBookingCourt,
    newCourtBookingPrisonCourtEmail = newCourtBookingPrisonCourtEmail,
    newCourtBookingPrisonNoCourtEmail = newCourtBookingPrisonNoCourtEmail,
    amendedCourtBookingUser = amendedCourtBookingUser,
    amendedCourtBookingPrisonCourtEmail = amendedCourtBookingPrisonCourtEmail,
    amendedCourtBookingPrisonNoCourtEmail = amendedCourtBookingPrisonNoCourtEmail,
    cancelledCourtBookingUser = cancelledCourtBookingUser,
    cancelledCourtBookingPrisonCourtEmail = cancelledCourtBookingPrisonCourtEmail,
    cancelledCourtBookingPrisonNoCourtEmail = cancelledCourtBookingPrisonNoCourtEmail,
    courtBookingRequestUser = courtBookingRequestUser,
    courtBookingRequestPrisonCourtEmail = courtBookingRequestPrisonCourtEmail,
    courtBookingRequestPrisonNoCourtEmail = courtBookingRequestPrisonNoCourtEmail,
    probationBookingRequestUser = probationBookingRequestUser,
    probationBookingRequestPrisonProbationTeamEmail = probationBookingRequestPrisonProbationTeamEmail,
    probationBookingRequestPrisonNoProbationTeamEmail = probationBookingRequestPrisonNoProbationTeamEmail,
    transferCourtBookingCourt = transferCourtBookingCourt,
    transferCourtBookingPrisonCourtEmail = transferCourtBookingPrisonCourtEmail,
    transferCourtBookingPrisonNoCourtEmail = transferCourtBookingPrisonNoCourtEmail,
    releaseCourtBookingCourt = releaseCourtBookingCourt,
    releaseCourtBookingPrisonCourtEmail = releaseCourtBookingPrisonCourtEmail,
    releaseCourtBookingPrisonNoCourtEmail = releaseCourtBookingPrisonNoCourtEmail,
  )
}

typealias TemplateId = String

fun interface EmailService {
  /**
   * On success returns a unique reference for each email sent along with the template identifier used.
   */
  fun send(email: Email): Result<Pair<UUID, TemplateId>>
}

abstract class Email(
  val address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  date: LocalDate = LocalDate.now(),
  comments: String? = null,
) {
  private val common = mapOf(
    "date" to date.toMediumFormatStyle(),
    "prisonerName" to prisonerFirstName.plus(" $prisonerLastName"),
    "comments" to (comments ?: "None entered"),
  )
  private val personalisation: MutableMap<String, String?> = mutableMapOf()

  protected fun addPersonalisation(key: String, value: String) {
    personalisation[key] = value
  }

  fun personalisation() = common.plus(personalisation)
}

data class EmailTemplates(
  val newCourtBookingUser: String,
  val newCourtBookingCourt: String,
  val newCourtBookingPrisonCourtEmail: String,
  val newCourtBookingPrisonNoCourtEmail: String,
  val amendedCourtBookingUser: String,
  val amendedCourtBookingPrisonCourtEmail: String,
  val amendedCourtBookingPrisonNoCourtEmail: String,
  val cancelledCourtBookingUser: String,
  val cancelledCourtBookingPrisonCourtEmail: String,
  val cancelledCourtBookingPrisonNoCourtEmail: String,
  val courtBookingRequestUser: String,
  val courtBookingRequestPrisonCourtEmail: String,
  val courtBookingRequestPrisonNoCourtEmail: String,
  val probationBookingRequestUser: String,
  val probationBookingRequestPrisonProbationTeamEmail: String,
  val probationBookingRequestPrisonNoProbationTeamEmail: String,
  val transferCourtBookingCourt: String,
  val transferCourtBookingPrisonCourtEmail: String,
  val transferCourtBookingPrisonNoCourtEmail: String,
  val releaseCourtBookingCourt: String,
  val releaseCourtBookingPrisonCourtEmail: String,
  val releaseCourtBookingPrisonNoCourtEmail: String,
) {

  private val emailTemplateMappings = mapOf(
    NewCourtBookingUserEmail::class.java to newCourtBookingUser,
    NewCourtBookingCourtEmail::class.java to newCourtBookingCourt,
    NewCourtBookingPrisonCourtEmail::class.java to newCourtBookingPrisonCourtEmail,
    NewCourtBookingPrisonNoCourtEmail::class.java to newCourtBookingPrisonNoCourtEmail,
    AmendedCourtBookingUserEmail::class.java to amendedCourtBookingUser,
    AmendedCourtBookingPrisonCourtEmail::class.java to amendedCourtBookingPrisonCourtEmail,
    AmendedCourtBookingPrisonNoCourtEmail::class.java to amendedCourtBookingPrisonNoCourtEmail,
    CancelledCourtBookingUserEmail::class.java to cancelledCourtBookingUser,
    CancelledCourtBookingPrisonCourtEmail::class.java to cancelledCourtBookingPrisonCourtEmail,
    CancelledCourtBookingPrisonNoCourtEmail::class.java to cancelledCourtBookingPrisonNoCourtEmail,
    CourtBookingRequestUserEmail::class.java to courtBookingRequestUser,
    CourtBookingRequestPrisonCourtEmail::class.java to courtBookingRequestPrisonCourtEmail,
    CourtBookingRequestPrisonNoCourtEmail::class.java to courtBookingRequestPrisonNoCourtEmail,
    ProbationBookingRequestUserEmail::class.java to probationBookingRequestUser,
    ProbationBookingRequestPrisonProbationTeamEmail::class.java to probationBookingRequestPrisonProbationTeamEmail,
    ProbationBookingRequestPrisonNoProbationTeamEmail::class.java to probationBookingRequestPrisonNoProbationTeamEmail,
    TransferredCourtBookingCourtEmail::class.java to transferCourtBookingCourt,
    TransferredCourtBookingPrisonCourtEmail::class.java to transferCourtBookingPrisonCourtEmail,
    TransferredCourtBookingPrisonNoCourtEmail::class.java to transferCourtBookingPrisonNoCourtEmail,
    ReleasedCourtBookingCourtEmail::class.java to releaseCourtBookingCourt,
    ReleasedCourtBookingPrisonCourtEmail::class.java to releaseCourtBookingPrisonCourtEmail,
    ReleasedCourtBookingPrisonNoCourtEmail::class.java to releaseCourtBookingPrisonNoCourtEmail,
  )

  init {
    require(EmailTemplates::class.constructors.single().parameters.size == emailTemplateMappings.size) {
      "Number of email template mappings does not match that of the number of possible templates"
    }
  }

  fun <T : Email> templateFor(type: Class<T>) = emailTemplateMappings[type]
}
