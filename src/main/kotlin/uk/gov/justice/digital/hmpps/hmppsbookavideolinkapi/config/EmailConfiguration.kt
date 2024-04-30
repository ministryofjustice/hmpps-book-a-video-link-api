package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.GovNotifyEmailService
import uk.gov.service.notify.NotificationClient

/**
 * Email configuration for the service. The sending of all emails is handled by the Gov Notify service.
 *
 * If the API key is present then emails will be sent via the Gov Notify service. If the API key is blank/not present
 * then emails will not be sent.
 */
@Configuration
class EmailConfiguration(
  @Value("\${notify.api.key:}") private val apiKey: String,
  @Value("\${notify.templates.prison.transferred:}") private val offenderTransferredPrisonEmailTemplateId: String,
  @Value("\${notify.templates.court.transferred:}") private val offenderTransferredCourtEmailTemplateId: String,
  @Value("\${notify.templates.prison.transferred-but-no-court-email:}") private val offenderTransferredPrisonEmailButNoCourtEmailTemplateId: String,
  @Value("\${notify.templates.prison.released:}") private val offenderReleasedPrisonEmailTemplateId: String,
  @Value("\${notify.templates.court.released:}") private val offenderReleasedCourtEmailTemplateId: String,
  @Value("\${notify.templates.prison.released-but-no-court-email:}") private val offenderReleasedPrisonEmailButNoCourtEmailTemplateId: String,
  @Value("\${notify.templates.appointment-canceled.prison:}") private val appointmentCanceledPrisonEmailTemplateId: String,
  @Value("\${notify.templates.appointment-canceled.court:}") private val appointmentCanceledCourtEmailTemplateId: String,
  @Value("\${notify.templates.appointment-canceled.no-court-email:}") private val appointmentCanceledPrisonEmailButNoCourtEmailTemplateId: String,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Bean
  fun emailService() =
    if (apiKey.isBlank()) {
      EmailService { log.info("Email will not be sent.") }.also { log.info("Gov Notify emails are disabled") }
    } else {
      GovNotifyEmailService(NotificationClient(apiKey), emailTemplates()).also { log.info("Gov Notify emails are enabled") }
    }

  private fun emailTemplates() = EmailTemplates(
    offenderTransferredPrisonEmailTemplateId = offenderTransferredPrisonEmailTemplateId,
    offenderTransferredCourtEmailTemplateId = offenderTransferredCourtEmailTemplateId,
    offenderTransferredPrisonEmailButNoCourtEmailTemplateId = offenderTransferredPrisonEmailButNoCourtEmailTemplateId,
    offenderReleasedPrisonEmailTemplateId = offenderReleasedPrisonEmailTemplateId,
    offenderReleasedCourtEmailTemplateId = offenderReleasedCourtEmailTemplateId,
    offenderReleasedPrisonEmailButNoCourtEmailTemplateId = offenderReleasedPrisonEmailButNoCourtEmailTemplateId,
    appointmentCanceledPrisonEmailTemplateId = appointmentCanceledPrisonEmailTemplateId,
    appointmentCanceledCourtEmailTemplateId = appointmentCanceledCourtEmailTemplateId,
    appointmentCanceledPrisonEmailButNoCourtEmailTemplateId = appointmentCanceledPrisonEmailButNoCourtEmailTemplateId,
  )
}

fun interface EmailService {
  // TODO: the actual interface for the sendEmail yet to be decided.
  fun sendEmail()
}

internal data class EmailTemplates(
  val offenderTransferredPrisonEmailTemplateId: String,
  val offenderTransferredCourtEmailTemplateId: String,
  val offenderTransferredPrisonEmailButNoCourtEmailTemplateId: String,
  val offenderReleasedPrisonEmailTemplateId: String,
  val offenderReleasedCourtEmailTemplateId: String,
  val offenderReleasedPrisonEmailButNoCourtEmailTemplateId: String,
  val appointmentCanceledPrisonEmailTemplateId: String,
  val appointmentCanceledCourtEmailTemplateId: String,
  val appointmentCanceledPrisonEmailButNoCourtEmailTemplateId: String,
)
