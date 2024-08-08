package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailTemplates
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TemplateId
import uk.gov.service.notify.NotificationClient
import java.util.UUID

class GovNotifyEmailService(
  private val client: NotificationClient,
  private val emailTemplates: EmailTemplates,
) : EmailService {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun send(email: Email): Result<Pair<UUID, TemplateId>> =
    runCatching {
      val templateId = emailTemplates.templateFor(email::class.java)
        ?: throw RuntimeException("EMAIL: Missing template ID for email type ${email.javaClass.simpleName}.")

      client.sendEmail(templateId, email.address, email.personalisation(), null).notificationId!! to templateId
    }
      .onSuccess { log.info("EMAIL: sent ${email.javaClass.simpleName} email.") }
      .onFailure { exception -> log.info("EMAIL: failed to send ${email.javaClass.simpleName} email.", exception) }
}
