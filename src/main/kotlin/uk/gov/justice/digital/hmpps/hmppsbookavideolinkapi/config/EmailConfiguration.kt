package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toMediumFormatStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.GovNotifyEmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.AmendedCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.AmendedCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.AmendedCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.AmendedCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CancelledCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CancelledCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CancelledCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtBookingRequestPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtBookingRequestPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtBookingRequestUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.ReleasedCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.ReleasedCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.ReleasedCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.TransferredCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.TransferredCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.TransferredCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.AmendedProbationBookingPrisonNoProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.AmendedProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.AmendedProbationBookingProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.AmendedProbationBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.CancelledProbationBookingPrisonNoProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.CancelledProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.CancelledProbationBookingProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.CancelledProbationBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.NewProbationBookingPrisonNoProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.NewProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.NewProbationBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationBookingRequestPrisonNoProbationTeamEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationBookingRequestPrisonProbationTeamEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationBookingRequestUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ReleasedProbationBookingPrisonNoProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ReleasedProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ReleasedProbationBookingProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.TransferredProbationBookingPrisonNoProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.TransferredProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.TransferredProbationBookingProbationEmail
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
  @Value("\${notify.templates.court.new-booking.user:}") private val newCourtBookingUser: String,
  @Value("\${notify.templates.court.new-booking.court:}") private val newCourtBookingCourt: String,
  @Value("\${notify.templates.court.new-booking.prison-court-email:}") private val newCourtBookingPrisonCourtEmail: String,
  @Value("\${notify.templates.court.new-booking.prison-no-court-email:}") private val newCourtBookingPrisonNoCourtEmail: String,
  @Value("\${notify.templates.court.amended-booking.user:}") private val amendedCourtBookingUser: String,
  @Value("\${notify.templates.court.amended-booking.court:}") private val amendedCourtBookingCourtEmail: String,
  @Value("\${notify.templates.court.amended-booking.prison-court-email:}") private val amendedCourtBookingPrisonCourtEmail: String,
  @Value("\${notify.templates.court.amended-booking.prison-no-court-email:}") private val amendedCourtBookingPrisonNoCourtEmail: String,
  @Value("\${notify.templates.court.cancelled-booking.user:}") private val cancelledCourtBookingUser: String,
  @Value("\${notify.templates.court.cancelled-booking.prison-court-email:}") private val cancelledCourtBookingPrisonCourtEmail: String,
  @Value("\${notify.templates.court.cancelled-booking.prison-no-court-email:}") private val cancelledCourtBookingPrisonNoCourtEmail: String,
  @Value("\${notify.templates.court.booking-request.user:}") private val courtBookingRequestUser: String,
  @Value("\${notify.templates.court.booking-request.prison-court-email:}") private val courtBookingRequestPrisonCourtEmail: String,
  @Value("\${notify.templates.court.booking-request.prison-no-court-email:}") private val courtBookingRequestPrisonNoCourtEmail: String,
  @Value("\${notify.templates.court.transfer-booking.court:}") private val transferCourtBookingCourt: String,
  @Value("\${notify.templates.court.transfer-booking.prison-court-email:}") private val transferCourtBookingPrisonCourtEmail: String,
  @Value("\${notify.templates.court.transfer-booking.prison-no-court-email:}") private val transferCourtBookingPrisonNoCourtEmail: String,
  @Value("\${notify.templates.court.release-booking.court:}") private val releaseCourtBookingCourt: String,
  @Value("\${notify.templates.court.release-booking.prison-court-email:}") private val releaseCourtBookingPrisonCourtEmail: String,
  @Value("\${notify.templates.court.release-booking.prison-no-court-email:}") private val releaseCourtBookingPrisonNoCourtEmail: String,
  @Value("\${notify.templates.probation.booking-request.user:}") private val probationBookingRequestUser: String,
  @Value("\${notify.templates.probation.booking-request.prison-probation-team-email:}") private val probationBookingRequestPrisonProbationTeamEmail: String,
  @Value("\${notify.templates.probation.booking-request.prison-no-probation-team-email:}") private val probationBookingRequestPrisonNoProbationTeamEmail: String,
  @Value("\${notify.templates.probation.new-booking.user:}") private val newProbationBookingUser: String,
  @Value("\${notify.templates.probation.new-booking.prison-probation-email:}") private val newProbationBookingPrisonProbationEmail: String,
  @Value("\${notify.templates.probation.new-booking.prison-no-probation-email:}") private val newProbationBookingPrisonNoProbationEmail: String,
  @Value("\${notify.templates.probation.amended-booking.user:}") private val amendedProbationBookingUser: String,
  @Value("\${notify.templates.probation.amended-booking.prison-probation-email:}") private val amendedProbationBookingPrisonProbationEmail: String,
  @Value("\${notify.templates.probation.amended-booking.prison-no-probation-email:}") private val amendedProbationBookingPrisonNoProbationEmail: String,
  @Value("\${notify.templates.probation.amended-booking.probation:}") private val amendedProbationBookingProbationEmail: String,
  @Value("\${notify.templates.probation.cancelled-booking.user:}") private val cancelledProbationBookingUser: String,
  @Value("\${notify.templates.probation.cancelled-booking.probation:}") private val cancelledProbationBookingProbationEmail: String,
  @Value("\${notify.templates.probation.cancelled-booking.prison-probation-email:}") private val cancelledProbationBookingPrisonProbationEmail: String,
  @Value("\${notify.templates.probation.cancelled-booking.prison-no-probation-email:}") private val cancelledProbationBookingPrisonNoProbationEmail: String,
  @Value("\${notify.templates.probation.release-booking.probation:}") private val releaseProbationBookingProbation: String,
  @Value("\${notify.templates.probation.release-booking.prison-probation-email:}") private val releaseProbationBookingPrisonProbationEmail: String,
  @Value("\${notify.templates.probation.release-booking.prison-no-probation-email:}") private val releaseProbationBookingPrisonNoProbationEmail: String,
  @Value("\${notify.templates.probation.transfer-booking.probation:}") private val transferProbationBookingProbation: String,
  @Value("\${notify.templates.probation.transfer-booking.prison-probation-email:}") private val transferProbationBookingPrisonProbationEmail: String,
  @Value("\${notify.templates.probation.transfer-booking.prison-no-probation-email:}") private val transferProbationBookingPrisonNoProbationEmail: String,
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
    amendedCourtBookingCourtEmail = amendedCourtBookingCourtEmail,
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
    newProbationBookingUser = newProbationBookingUser,
    newProbationBookingPrisonProbationEmail = newProbationBookingPrisonProbationEmail,
    newProbationBookingPrisonNoProbationEmail = newProbationBookingPrisonNoProbationEmail,
    amendedProbationBookingUser = amendedProbationBookingUser,
    amendedProbationBookingPrisonProbationEmail = amendedProbationBookingPrisonProbationEmail,
    amendedProbationBookingPrisonNoProbationEmail = amendedProbationBookingPrisonNoProbationEmail,
    amendedProbationBookingProbationEmail = amendedProbationBookingProbationEmail,
    cancelledProbationBookingUser = cancelledProbationBookingUser,
    cancelledProbationBookingProbationEmail = cancelledProbationBookingProbationEmail,
    cancelledProbationBookingPrisonProbationEmail = cancelledProbationBookingPrisonProbationEmail,
    cancelledProbationBookingPrisonNoProbationEmail = cancelledProbationBookingPrisonNoProbationEmail,
    releaseProbationBookingProbation = releaseProbationBookingProbation,
    releaseProbationBookingPrisonProbationEmail = releaseProbationBookingPrisonProbationEmail,
    releaseProbationBookingPrisonNoProbationEmail = releaseProbationBookingPrisonNoProbationEmail,
    transferProbationBookingProbation = transferProbationBookingProbation,
    transferProbationBookingPrisonProbationEmail = transferProbationBookingPrisonProbationEmail,
    transferProbationBookingPrisonNoProbationEmail = transferProbationBookingPrisonNoProbationEmail,
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
  val amendedCourtBookingCourtEmail: String,
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
  val newProbationBookingUser: String,
  val newProbationBookingPrisonProbationEmail: String,
  val newProbationBookingPrisonNoProbationEmail: String,
  val amendedProbationBookingUser: String,
  val amendedProbationBookingPrisonProbationEmail: String,
  val amendedProbationBookingPrisonNoProbationEmail: String,
  val amendedProbationBookingProbationEmail: String,
  val cancelledProbationBookingUser: String,
  val cancelledProbationBookingProbationEmail: String,
  val cancelledProbationBookingPrisonProbationEmail: String,
  val cancelledProbationBookingPrisonNoProbationEmail: String,
  val releaseProbationBookingProbation: String,
  val releaseProbationBookingPrisonProbationEmail: String,
  val releaseProbationBookingPrisonNoProbationEmail: String,
  val transferProbationBookingProbation: String,
  val transferProbationBookingPrisonProbationEmail: String,
  val transferProbationBookingPrisonNoProbationEmail: String,
) {

  private val emailTemplateMappings = mapOf(
    NewCourtBookingUserEmail::class.java to newCourtBookingUser,
    NewCourtBookingCourtEmail::class.java to newCourtBookingCourt,
    NewCourtBookingPrisonCourtEmail::class.java to newCourtBookingPrisonCourtEmail,
    NewCourtBookingPrisonNoCourtEmail::class.java to newCourtBookingPrisonNoCourtEmail,
    AmendedCourtBookingUserEmail::class.java to amendedCourtBookingUser,
    AmendedCourtBookingCourtEmail::class.java to amendedCourtBookingCourtEmail,
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
    NewProbationBookingUserEmail::class.java to newProbationBookingUser,
    NewProbationBookingPrisonProbationEmail::class.java to newProbationBookingPrisonProbationEmail,
    NewProbationBookingPrisonNoProbationEmail::class.java to newProbationBookingPrisonNoProbationEmail,
    AmendedProbationBookingUserEmail::class.java to amendedProbationBookingUser,
    AmendedProbationBookingPrisonProbationEmail::class.java to amendedProbationBookingPrisonProbationEmail,
    AmendedProbationBookingPrisonNoProbationEmail::class.java to amendedProbationBookingPrisonNoProbationEmail,
    AmendedProbationBookingProbationEmail::class.java to amendedProbationBookingProbationEmail,
    CancelledProbationBookingUserEmail::class.java to cancelledProbationBookingUser,
    CancelledProbationBookingProbationEmail::class.java to cancelledProbationBookingProbationEmail,
    CancelledProbationBookingPrisonProbationEmail::class.java to cancelledProbationBookingPrisonProbationEmail,
    CancelledProbationBookingPrisonNoProbationEmail::class.java to cancelledProbationBookingPrisonNoProbationEmail,
    ReleasedProbationBookingProbationEmail::class.java to releaseProbationBookingProbation,
    ReleasedProbationBookingPrisonProbationEmail::class.java to releaseProbationBookingPrisonProbationEmail,
    ReleasedProbationBookingPrisonNoProbationEmail::class.java to releaseProbationBookingPrisonNoProbationEmail,
    TransferredProbationBookingProbationEmail::class.java to transferProbationBookingProbation,
    TransferredProbationBookingPrisonProbationEmail::class.java to transferProbationBookingPrisonProbationEmail,
    TransferredProbationBookingPrisonNoProbationEmail::class.java to transferProbationBookingPrisonNoProbationEmail,
  )

  init {
    require(EmailTemplates::class.constructors.single().parameters.size == emailTemplateMappings.size) {
      "Number of email template mappings does not match that of the number of possible templates"
    }

    require(EmailTemplates::class.constructors.single().parameters.size == emailTemplateMappings.values.distinct().size) {
      "Template IDs must be unique."
    }
  }

  fun <T : Email> templateFor(type: Class<T>) = emailTemplateMappings[type]
}
