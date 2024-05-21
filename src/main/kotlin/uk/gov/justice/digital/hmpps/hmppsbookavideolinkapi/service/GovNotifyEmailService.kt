package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailTemplates
import uk.gov.service.notify.NotificationClient
import java.time.LocalDate
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
      is CourtNewBookingEmail -> send(email, emailTemplates.courtNewBooking)
      else -> throw RuntimeException("Unsupported email type ${email.javaClass.simpleName}.")
    }

  private fun send(email: Email, templateId: String) =
    runCatching {
      client.sendEmail(templateId, email.address, email.personalisation(), null).notificationId!!
    }
      .onSuccess { log.info("EMAIL: sent ${email.javaClass.simpleName} email.") }
      .onFailure { log.info("EMAIL: failed to send ${email.javaClass.simpleName} email.") }
}

class CourtNewBookingEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  date: LocalDate = LocalDate.now(),
  userName: String,
  court: String,
  prison: String,
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String? = "None entered",
) : Email(address, prisonerFirstName, prisonerLastName, prisonerNumber, date, comments) {
  init {
    addPersonalisation("userName", userName)
    addPersonalisation("court", court)
    addPersonalisation("prison", prison)
    addPersonalisation("preAppointmentInfo", preAppointmentInfo ?: "Not required")
    addPersonalisation("mainAppointmentInfo", mainAppointmentInfo)
    addPersonalisation("postAppointmentInfo", postAppointmentInfo ?: "Not required")
  }
}
