package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
  @Value("\${notify.templates.court.new-booking:}") private val courtNewBooking: String,
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
    courtNewBooking = courtNewBooking,
  )
}

fun interface EmailService {
  /**
   * On success returns a unique reference for each email sent.
   */
  fun send(email: Email): Result<UUID>
}

abstract class Email(
  val address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  date: LocalDate = LocalDate.now(),
  comments: String? = "",
) {
  private val common = mapOf(
    "date" to date.toString(),
    "prisonerName" to prisonerFirstName.plus(" $prisonerLastName"),
    "offenderNo" to prisonerNumber,
    "comments" to comments,
  )
  private val personalisation: MutableMap<String, String?> = mutableMapOf()

  protected fun addPersonalisation(key: String, value: String) {
    personalisation[key] = value
  }

  fun personalisation() = common.plus(personalisation)
}

data class EmailTemplates(
  val courtNewBooking: String,
)
