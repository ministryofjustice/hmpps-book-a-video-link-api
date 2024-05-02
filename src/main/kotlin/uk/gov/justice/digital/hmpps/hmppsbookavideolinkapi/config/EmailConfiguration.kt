package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.GovNotifyEmailService
import uk.gov.service.notify.NotificationClient
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
  @Value("\${notify.templates.appointment-cancelled-court:}") private val appointmentCancelledCourt: String,
  @Value("\${notify.templates.appointment-cancelled-no-court:}") private val appointmentCancelledPrisonNoCourt: String,
  @Value("\${notify.templates.appointment-cancelled-prison:}") private val appointmentCancelledPrison: String,
  @Value("\${notify.templates.court.released:}") private val offenderReleasedCourt: String,
  @Value("\${notify.templates.court.transferred:}") private val offenderTransferredCourt: String,
  @Value("\${notify.templates.prison.released:}") private val offenderReleasedPrison: String,
  @Value("\${notify.templates.prison.released-no-court:}") private val offenderReleasedPrisonNoCourt: String,
  @Value("\${notify.templates.prison.transferred:}") private val offenderTransferredPrison: String,
  @Value("\${notify.templates.prison.transferred-no-court:}") private val offenderTransferredPrisonNoCourt: String,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Bean
  fun emailService() =
    if (apiKey.isBlank()) {
      EmailService { email -> Result.success(UUID.randomUUID()).also { log.info("Email ${email.javaClass.simpleName} not sent.") } }.also { log.info("Gov Notify emails are disabled") }
    } else {
      GovNotifyEmailService(NotificationClient(apiKey), emailTemplates()).also { log.info("Gov Notify emails are enabled") }
    }

  private fun emailTemplates() = EmailTemplates(
    appointmentCancelledCourt = appointmentCancelledCourt,
    appointmentCancelledPrison = appointmentCancelledPrison,
    appointmentCancelledPrisonNoCourt = appointmentCancelledPrisonNoCourt,
    offenderReleasedCourt = offenderReleasedCourt,
    offenderReleasedPrison = offenderReleasedPrison,
    offenderReleasedPrisonNoCourt = offenderReleasedPrisonNoCourt,
    offenderTransferredCourt = offenderTransferredCourt,
    offenderTransferredPrison = offenderTransferredPrison,
    offenderTransferredPrisonNoCourt = offenderTransferredPrisonNoCourt,
  )
}

fun interface EmailService {
  /**
   * On success returns a unique reference for each email sent.
   */
  fun send(email: Email): Result<UUID>
}

abstract class Email {
  abstract val address: String
  abstract val personalisation: Map<String, String?>
}

data class EmailTemplates(
  val appointmentCancelledCourt: String,
  val appointmentCancelledPrison: String,
  val appointmentCancelledPrisonNoCourt: String,
  val offenderReleasedCourt: String,
  val offenderReleasedPrison: String,
  val offenderReleasedPrisonNoCourt: String,
  val offenderTransferredCourt: String,
  val offenderTransferredPrison: String,
  val offenderTransferredPrisonNoCourt: String,
)
