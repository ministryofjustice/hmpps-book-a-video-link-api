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
    runCatching {
      val templateId = emailTemplates.templateFor(email::class.java)
        ?: throw RuntimeException("EMAIL: Missing template ID for email type ${email.javaClass.simpleName}.")

      client.sendEmail(templateId, email.address, email.personalisation(), null).notificationId!! to templateId
    }
      .onSuccess { log.info("EMAIL: sent ${email.javaClass.simpleName} email.") }
      .onFailure { exception -> log.info("EMAIL: failed to send ${email.javaClass.simpleName} email.", exception) }
}

class NewCourtBookingUserEmail(
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

class NewCourtBookingCourtEmail(
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

class AmendedCourtBookingUserEmail(
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

class CancelledCourtBookingUserEmail(
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

class CourtBookingRequestUserEmail(
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

class ProbationBookingRequestUserEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  dateOfBirth: LocalDate,
  date: LocalDate = LocalDate.now(),
  userName: String,
  probationTeam: String,
  prison: String,
  meetingType: String,
  appointmentInfo: String,
  comments: String?,
) : Email(address, prisonerFirstName, prisonerLastName, date, comments) {
  init {
    addPersonalisation("userName", userName)
    addPersonalisation("dateOfBirth", dateOfBirth.toMediumFormatStyle())
    addPersonalisation("probationTeam", probationTeam)
    addPersonalisation("prison", prison)
    addPersonalisation("meetingType", meetingType)
    addPersonalisation("appointmentInfo", appointmentInfo)
  }
}

class ProbationBookingRequestPrisonProbationTeamEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  dateOfBirth: LocalDate,
  date: LocalDate = LocalDate.now(),
  probationTeam: String,
  probationTeamEmailAddress: String,
  prison: String,
  meetingType: String,
  appointmentInfo: String,
  comments: String?,
) : Email(address, prisonerFirstName, prisonerLastName, date, comments) {
  init {
    addPersonalisation("dateOfBirth", dateOfBirth.toMediumFormatStyle())
    addPersonalisation("probationTeamEmailAddress", probationTeamEmailAddress)
    addPersonalisation("probationTeam", probationTeam)
    addPersonalisation("prison", prison)
    addPersonalisation("meetingType", meetingType)
    addPersonalisation("appointmentInfo", appointmentInfo)
  }
}

class ProbationBookingRequestPrisonNoProbationTeamEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  dateOfBirth: LocalDate,
  date: LocalDate = LocalDate.now(),
  probationTeam: String,
  prison: String,
  meetingType: String,
  appointmentInfo: String,
  comments: String?,
) : Email(address, prisonerFirstName, prisonerLastName, date, comments) {
  init {
    addPersonalisation("dateOfBirth", dateOfBirth.toMediumFormatStyle())
    addPersonalisation("probationTeam", probationTeam)
    addPersonalisation("prison", prison)
    addPersonalisation("meetingType", meetingType)
    addPersonalisation("appointmentInfo", appointmentInfo)
  }
}

class TransferredCourtBookingCourtEmail(
  address: String,
  court: String,
  prison: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  dateOfBirth: LocalDate,
  prisonerNumber: String,
  date: LocalDate = LocalDate.now(),
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : Email(address, prisonerFirstName, prisonerLastName, date, comments) {
  init {
    addPersonalisation("dateOfBirth", dateOfBirth.toMediumFormatStyle())
    addPersonalisation("offenderNo", prisonerNumber)
    addPersonalisation("court", court)
    addPersonalisation("prison", prison)
    addPersonalisation("preAppointmentInfo", preAppointmentInfo ?: "Not required")
    addPersonalisation("mainAppointmentInfo", mainAppointmentInfo)
    addPersonalisation("postAppointmentInfo", postAppointmentInfo ?: "Not required")
  }
}

class ReleasedCourtBookingCourtEmail(
  address: String,
  court: String,
  prison: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  dateOfBirth: LocalDate,
  prisonerNumber: String,
  date: LocalDate = LocalDate.now(),
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : Email(address, prisonerFirstName, prisonerLastName, date, comments) {
  init {
    addPersonalisation("dateOfBirth", dateOfBirth.toMediumFormatStyle())
    addPersonalisation("offenderNo", prisonerNumber)
    addPersonalisation("court", court)
    addPersonalisation("prison", prison)
    addPersonalisation("preAppointmentInfo", preAppointmentInfo ?: "Not required")
    addPersonalisation("mainAppointmentInfo", mainAppointmentInfo)
    addPersonalisation("postAppointmentInfo", postAppointmentInfo ?: "Not required")
  }
}

class ReleasedCourtBookingPrisonCourtEmail(
  address: String,
  prison: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  dateOfBirth: LocalDate,
  prisonerNumber: String,
  court: String,
  date: LocalDate = LocalDate.now(),
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : Email(address, prisonerFirstName, prisonerLastName, date, comments) {
  init {
    addPersonalisation("dateOfBirth", dateOfBirth.toMediumFormatStyle())
    addPersonalisation("offenderNo", prisonerNumber)
    addPersonalisation("court", court)
    addPersonalisation("prison", prison)
    addPersonalisation("preAppointmentInfo", preAppointmentInfo ?: "Not required")
    addPersonalisation("mainAppointmentInfo", mainAppointmentInfo)
    addPersonalisation("postAppointmentInfo", postAppointmentInfo ?: "Not required")
  }
}

class ReleasedCourtBookingPrisonNoCourtEmail(
  address: String,
  prison: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  dateOfBirth: LocalDate,
  prisonerNumber: String,
  court: String,
  date: LocalDate = LocalDate.now(),
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : Email(address, prisonerFirstName, prisonerLastName, date, comments) {
  init {
    addPersonalisation("dateOfBirth", dateOfBirth.toMediumFormatStyle())
    addPersonalisation("offenderNo", prisonerNumber)
    addPersonalisation("court", court)
    addPersonalisation("prison", prison)
    addPersonalisation("preAppointmentInfo", preAppointmentInfo ?: "Not required")
    addPersonalisation("mainAppointmentInfo", mainAppointmentInfo)
    addPersonalisation("postAppointmentInfo", postAppointmentInfo ?: "Not required")
  }
}

class TransferredCourtBookingPrisonCourtEmail(
  address: String,
  prison: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  dateOfBirth: LocalDate,
  prisonerNumber: String,
  court: String,
  date: LocalDate = LocalDate.now(),
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : Email(address, prisonerFirstName, prisonerLastName, date, comments) {
  init {
    addPersonalisation("dateOfBirth", dateOfBirth.toMediumFormatStyle())
    addPersonalisation("offenderNo", prisonerNumber)
    addPersonalisation("court", court)
    addPersonalisation("prison", prison)
    addPersonalisation("preAppointmentInfo", preAppointmentInfo ?: "Not required")
    addPersonalisation("mainAppointmentInfo", mainAppointmentInfo)
    addPersonalisation("postAppointmentInfo", postAppointmentInfo ?: "Not required")
  }
}

class TransferredCourtBookingPrisonNoCourtEmail(
  address: String,
  prison: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  dateOfBirth: LocalDate,
  prisonerNumber: String,
  court: String,
  date: LocalDate = LocalDate.now(),
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : Email(address, prisonerFirstName, prisonerLastName, date, comments) {
  init {
    addPersonalisation("dateOfBirth", dateOfBirth.toMediumFormatStyle())
    addPersonalisation("offenderNo", prisonerNumber)
    addPersonalisation("court", court)
    addPersonalisation("prison", prison)
    addPersonalisation("preAppointmentInfo", preAppointmentInfo ?: "Not required")
    addPersonalisation("mainAppointmentInfo", mainAppointmentInfo)
    addPersonalisation("postAppointmentInfo", postAppointmentInfo ?: "Not required")
  }
}

class NewProbationBookingUserEmail(
  address: String,
  prisonerNumber: String,
  userName: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  probationTeam: String,
  prison: String,
  date: LocalDate = LocalDate.now(),
  appointmentInfo: String,
  comments: String?,
) : Email(address, prisonerFirstName, prisonerLastName, date, comments) {
  init {
    addPersonalisation("offenderNo", prisonerNumber)
    addPersonalisation("userName", userName)
    addPersonalisation("probationTeam", probationTeam)
    addPersonalisation("prison", prison)
    addPersonalisation("appointmentInfo", appointmentInfo)
  }
}
