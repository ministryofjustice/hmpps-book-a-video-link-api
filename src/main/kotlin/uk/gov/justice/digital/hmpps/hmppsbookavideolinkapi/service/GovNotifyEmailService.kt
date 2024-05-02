package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailTemplates
import uk.gov.service.notify.NotificationClient
import java.util.UUID

class GovNotifyEmailService(
  private val client: NotificationClient,
  private val emailTemplates: EmailTemplates,
) : EmailService {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun send(email: Email): Result<UUID> =
    when (email) {
      is AppointmentCancelledCourtEmail -> send(email, emailTemplates.appointmentCancelledCourt)
      is AppointmentCancelledPrisonEmail -> send(email, emailTemplates.appointmentCancelledPrison)
      is AppointmentCancelledPrisonNoCourtEmail -> send(email, emailTemplates.appointmentCancelledPrisonNoCourt)
      is OffenderReleasedCourtEmail -> send(email, emailTemplates.offenderReleasedCourt)
      is OffenderReleasedPrisonEmail -> send(email, emailTemplates.offenderReleasedPrison)
      is OffenderReleasedPrisonNoCourtEmail -> send(email, emailTemplates.offenderReleasedPrisonNoCourt)
      is OffenderTransferredCourtEmail -> send(email, emailTemplates.offenderTransferredCourt)
      is OffenderTransferredPrisonEmail -> send(email, emailTemplates.offenderTransferredPrison)
      is OffenderTransferredPrisonNoCourtEmail -> send(email, emailTemplates.offenderTransferredPrisonNoCourt)
      else -> throw RuntimeException("Unsupported email type ${email.javaClass.simpleName}.")
    }

  private fun send(email: Email, templateId: String) =
    runCatching {
      client.sendEmail(templateId, email.address, email.personalisation, null).notificationId!!
    }
      .onSuccess { log.info("EMAIL: sent ${email.javaClass.simpleName} email.") }
      .onFailure { log.info("EMAIL: failed to send ${email.javaClass.simpleName} email.") }
}

class AppointmentCancelledCourtEmail(override val address: String, override val personalisation: Map<String, String?>) : Email()

class AppointmentCancelledPrisonEmail(override val address: String, override val personalisation: Map<String, String?>) : Email()

class AppointmentCancelledPrisonNoCourtEmail(override val address: String, override val personalisation: Map<String, String?>) : Email()

class OffenderReleasedCourtEmail(override val address: String, override val personalisation: Map<String, String?>) : Email()

class OffenderReleasedPrisonEmail(override val address: String, override val personalisation: Map<String, String?>) : Email()

class OffenderReleasedPrisonNoCourtEmail(override val address: String, override val personalisation: Map<String, String?>) : Email()

class OffenderTransferredCourtEmail(override val address: String, override val personalisation: Map<String, String?>) : Email()

class OffenderTransferredPrisonEmail(override val address: String, override val personalisation: Map<String, String?>) : Email()

class OffenderTransferredPrisonNoCourtEmail(override val address: String, override val personalisation: Map<String, String?>) : Email()
