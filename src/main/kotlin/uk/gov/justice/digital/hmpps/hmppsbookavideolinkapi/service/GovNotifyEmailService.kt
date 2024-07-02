package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toMediumFormatStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailTemplates
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TemplateId
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

  override fun send(email: Email): Result<Pair<UUID, TemplateId>> =
    when (email) {
      is NewCourtBookingOwnerEmail -> send(email, emailTemplates.newCourtBookingOwner)
      is NewCourtBookingPrisonCourtEmail -> send(email, emailTemplates.newCourtBookingPrisonCourtEmail)
      is NewCourtBookingPrisonNoCourtEmail -> send(email, emailTemplates.newCourtBookingPrisonNoCourtEmail)
      is AmendedCourtBookingOwnerEmail -> send(email, emailTemplates.amendedCourtBookingOwner)
      is AmendedCourtBookingPrisonCourtEmail -> send(email, emailTemplates.amendedCourtBookingPrisonCourtEmail)
      is AmendedCourtBookingPrisonNoCourtEmail -> send(email, emailTemplates.amendedCourtBookingPrisonNoCourtEmail)
      is CancelledCourtBookingOwnerEmail -> send(email, emailTemplates.cancelledCourtBookingOwner)
      is CancelledCourtBookingPrisonCourtEmail -> send(email, emailTemplates.cancelledCourtBookingPrisonCourtEmail)
      is CancelledCourtBookingPrisonNoCourtEmail -> send(email, emailTemplates.cancelledCourtBookingPrisonNoCourtEmail)
      is CourtBookingRequestOwnerEmail -> send(email, emailTemplates.cancelledCourtBookingPrisonNoCourtEmail)
      is CourtBookingRequestPrisonCourtEmail -> send(email, emailTemplates.cancelledCourtBookingPrisonNoCourtEmail)
      is CourtBookingRequestPrisonNoCourtEmail -> send(email, emailTemplates.cancelledCourtBookingPrisonNoCourtEmail)
      else -> throw RuntimeException("Unsupported email type ${email.javaClass.simpleName}.")
    }

  private fun send(email: Email, templateId: TemplateId) =
    runCatching {
      client.sendEmail(templateId, email.address, email.personalisation(), null).notificationId!! to templateId
    }
      .onSuccess { log.info("EMAIL: sent ${email.javaClass.simpleName} email.") }
      .onFailure { exception -> log.info("EMAIL: failed to send ${email.javaClass.simpleName} email.", exception) }
}

class NewCourtBookingOwnerEmail(
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
  comments: String?,
) : Email(address, prisonerFirstName, prisonerLastName, date, comments) {
  init {
    addPersonalisation("offenderNo", prisonerNumber)
    addPersonalisation("userName", userName)
    addPersonalisation("court", court)
    addPersonalisation("prison", prison)
    addPersonalisation("preAppointmentInfo", preAppointmentInfo ?: "Not required")
    addPersonalisation("mainAppointmentInfo", mainAppointmentInfo)
    addPersonalisation("postAppointmentInfo", postAppointmentInfo ?: "Not required")
  }
}

class NewCourtBookingPrisonCourtEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  date: LocalDate = LocalDate.now(),
  court: String,
  courtEmailAddress: String,
  prison: String,
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : Email(address, prisonerFirstName, prisonerLastName, date, comments) {
  init {
    addPersonalisation("offenderNo", prisonerNumber)
    addPersonalisation("court", court)
    addPersonalisation("courtEmailAddress", courtEmailAddress)
    addPersonalisation("prison", prison)
    addPersonalisation("preAppointmentInfo", preAppointmentInfo ?: "Not required")
    addPersonalisation("mainAppointmentInfo", mainAppointmentInfo)
    addPersonalisation("postAppointmentInfo", postAppointmentInfo ?: "Not required")
  }
}

class NewCourtBookingPrisonNoCourtEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  date: LocalDate = LocalDate.now(),
  court: String,
  prison: String,
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : Email(address, prisonerFirstName, prisonerLastName, date, comments) {
  init {
    addPersonalisation("offenderNo", prisonerNumber)
    addPersonalisation("court", court)
    addPersonalisation("prison", prison)
    addPersonalisation("preAppointmentInfo", preAppointmentInfo ?: "Not required")
    addPersonalisation("mainAppointmentInfo", mainAppointmentInfo)
    addPersonalisation("postAppointmentInfo", postAppointmentInfo ?: "Not required")
  }
}

class AmendedCourtBookingOwnerEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  date: LocalDate = LocalDate.now(),
  userName: String,
  court: String,
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : Email(address, prisonerFirstName, prisonerLastName, date, comments) {
  init {
    addPersonalisation("offenderNo", prisonerNumber)
    addPersonalisation("userName", userName)
    addPersonalisation("court", court)
    addPersonalisation("preAppointmentInfo", preAppointmentInfo ?: "Not required")
    addPersonalisation("mainAppointmentInfo", mainAppointmentInfo)
    addPersonalisation("postAppointmentInfo", postAppointmentInfo ?: "Not required")
  }
}

class AmendedCourtBookingPrisonCourtEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  date: LocalDate = LocalDate.now(),
  court: String,
  courtEmailAddress: String,
  prison: String,
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : Email(address, prisonerFirstName, prisonerLastName, date, comments) {
  init {
    addPersonalisation("offenderNo", prisonerNumber)
    addPersonalisation("court", court)
    addPersonalisation("courtEmailAddress", courtEmailAddress)
    addPersonalisation("prison", prison)
    addPersonalisation("preAppointmentInfo", preAppointmentInfo ?: "Not required")
    addPersonalisation("mainAppointmentInfo", mainAppointmentInfo)
    addPersonalisation("postAppointmentInfo", postAppointmentInfo ?: "Not required")
  }
}

class AmendedCourtBookingPrisonNoCourtEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  date: LocalDate = LocalDate.now(),
  court: String,
  prison: String,
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : Email(address, prisonerFirstName, prisonerLastName, date, comments) {
  init {
    addPersonalisation("offenderNo", prisonerNumber)
    addPersonalisation("court", court)
    addPersonalisation("prison", prison)
    addPersonalisation("preAppointmentInfo", preAppointmentInfo ?: "Not required")
    addPersonalisation("mainAppointmentInfo", mainAppointmentInfo)
    addPersonalisation("postAppointmentInfo", postAppointmentInfo ?: "Not required")
  }
}

class CancelledCourtBookingOwnerEmail(
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
  comments: String?,
) : Email(address, prisonerFirstName, prisonerLastName, date, comments) {
  init {
    addPersonalisation("offenderNo", prisonerNumber)
    addPersonalisation("userName", userName)
    addPersonalisation("court", court)
    addPersonalisation("prison", prison)
    addPersonalisation("preAppointmentInfo", preAppointmentInfo ?: "Not required")
    addPersonalisation("mainAppointmentInfo", mainAppointmentInfo)
    addPersonalisation("postAppointmentInfo", postAppointmentInfo ?: "Not required")
  }
}

class CancelledCourtBookingPrisonCourtEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  date: LocalDate = LocalDate.now(),
  court: String,
  courtEmailAddress: String,
  prison: String,
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : Email(address, prisonerFirstName, prisonerLastName, date, comments) {
  init {
    addPersonalisation("offenderNo", prisonerNumber)
    addPersonalisation("court", court)
    addPersonalisation("courtEmailAddress", courtEmailAddress)
    addPersonalisation("prison", prison)
    addPersonalisation("preAppointmentInfo", preAppointmentInfo ?: "Not required")
    addPersonalisation("mainAppointmentInfo", mainAppointmentInfo)
    addPersonalisation("postAppointmentInfo", postAppointmentInfo ?: "Not required")
  }
}

class CancelledCourtBookingPrisonNoCourtEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  date: LocalDate = LocalDate.now(),
  court: String,
  prison: String,
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : Email(address, prisonerFirstName, prisonerLastName, date, comments) {
  init {
    addPersonalisation("offenderNo", prisonerNumber)
    addPersonalisation("court", court)
    addPersonalisation("prison", prison)
    addPersonalisation("preAppointmentInfo", preAppointmentInfo ?: "Not required")
    addPersonalisation("mainAppointmentInfo", mainAppointmentInfo)
    addPersonalisation("postAppointmentInfo", postAppointmentInfo ?: "Not required")
  }
}

class CourtBookingRequestOwnerEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  dateOfBirth: LocalDate,
  date: LocalDate = LocalDate.now(),
  userName: String,
  court: String,
  prison: String,
  hearingType: String,
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : Email(address, prisonerFirstName, prisonerLastName, date, comments) {
  init {
    addPersonalisation("userName", userName)
    addPersonalisation("dateOfBirth", dateOfBirth.toMediumFormatStyle())
    addPersonalisation("court", court)
    addPersonalisation("prison", prison)
    addPersonalisation("hearingType", hearingType)
    addPersonalisation("preAppointmentInfo", preAppointmentInfo ?: "Not required")
    addPersonalisation("mainAppointmentInfo", mainAppointmentInfo)
    addPersonalisation("postAppointmentInfo", postAppointmentInfo ?: "Not required")
  }
}

class CourtBookingRequestPrisonCourtEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  dateOfBirth: LocalDate,
  date: LocalDate = LocalDate.now(),
  court: String,
  courtEmailAddress: String,
  prison: String,
  hearingType: String,
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : Email(address, prisonerFirstName, prisonerLastName, date, comments) {
  init {
    addPersonalisation("dateOfBirth", dateOfBirth.toMediumFormatStyle())
    addPersonalisation("courtEmailAddress", courtEmailAddress)
    addPersonalisation("court", court)
    addPersonalisation("prison", prison)
    addPersonalisation("hearingType", hearingType)
    addPersonalisation("preAppointmentInfo", preAppointmentInfo ?: "Not required")
    addPersonalisation("mainAppointmentInfo", mainAppointmentInfo)
    addPersonalisation("postAppointmentInfo", postAppointmentInfo ?: "Not required")
  }
}

class CourtBookingRequestPrisonNoCourtEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  dateOfBirth: LocalDate,
  date: LocalDate = LocalDate.now(),
  court: String,
  prison: String,
  hearingType: String,
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : Email(address, prisonerFirstName, prisonerLastName, date, comments) {
  init {
    addPersonalisation("dateOfBirth", dateOfBirth.toMediumFormatStyle())
    addPersonalisation("court", court)
    addPersonalisation("prison", prison)
    addPersonalisation("hearingType", hearingType)
    addPersonalisation("preAppointmentInfo", preAppointmentInfo ?: "Not required")
    addPersonalisation("mainAppointmentInfo", mainAppointmentInfo)
    addPersonalisation("postAppointmentInfo", postAppointmentInfo ?: "Not required")
  }
}
