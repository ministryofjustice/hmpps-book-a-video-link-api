package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toMediumFormatStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.GovNotifyEmailService
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
  @Value("\${notify.templates.transfer-court-booking.user:}") private val transferCourtBookingUser: String,
  @Value("\${notify.templates.transfer-court-booking.prison-court-email:}") private val transferCourtBookingPrisonCourtEmail: String,
  @Value("\${notify.templates.transfer-court-booking.prison-no-court-email:}") private val transferCourtBookingPrisonNoCourtEmail: String,
  @Value("\${notify.templates.release-court-booking.user:}") private val releaseCourtBookingUser: String,
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
    transferCourtBookingUser = transferCourtBookingUser,
    transferCourtBookingPrisonCourtEmail = transferCourtBookingPrisonCourtEmail,
    transferCourtBookingPrisonNoCourtEmail = transferCourtBookingPrisonNoCourtEmail,
    releaseCourtBookingUser = releaseCourtBookingUser,
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
  val transferCourtBookingUser: String,
  val transferCourtBookingPrisonCourtEmail: String,
  val transferCourtBookingPrisonNoCourtEmail: String,
  val releaseCourtBookingUser: String,
  val releaseCourtBookingPrisonCourtEmail: String,
  val releaseCourtBookingPrisonNoCourtEmail: String,
)
