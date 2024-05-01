package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailTemplates
import uk.gov.service.notify.NotificationClient

internal class GovNotifyEmailService(client: NotificationClient, emailTemplates: EmailTemplates) : EmailService {
  override fun sendEmail() {
    TODO("Service is not yet implemented")
  }
}
