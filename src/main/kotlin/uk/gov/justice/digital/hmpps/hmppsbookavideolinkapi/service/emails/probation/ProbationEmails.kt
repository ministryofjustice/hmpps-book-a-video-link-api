package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toMediumFormatStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import java.time.LocalDate

abstract class ProbationEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  probationTeam: String,
  appointmentDate: LocalDate,
  appointmentInfo: String,
  prison: String,
  comments: String?,
  prisonerNumber: String? = null,
  meetingType: String? = null,
  userName: String? = null,
  probationEmailAddress: String? = null,
  dateOfBirth: LocalDate? = null,
  prisonVideoUrl: String? = null,
  probationOfficerName: String? = null,
  probationOfficerEmailAddress: String? = null,
  probationOfficerContactNumber: String? = null,
) : Email(address, prisonerFirstName, prisonerLastName, appointmentDate, comments) {

  init {
    addPersonalisation("probationTeam", probationTeam)
    addPersonalisation("prison", prison)
    addPersonalisation("appointmentInfo", appointmentInfo)
    prisonerNumber?.let { addPersonalisation("offenderNo", prisonerNumber) }
    meetingType?.let { addPersonalisation("meetingType", meetingType) }
    userName?.let { addPersonalisation("userName", userName) }
    probationEmailAddress?.let { addPersonalisation("probationEmailAddress", probationEmailAddress) }
    dateOfBirth?.let { addPersonalisation("dateOfBirth", dateOfBirth.toMediumFormatStyle()) }
    prisonVideoUrl?.let { addPersonalisation("prisonVideoUrl", prisonVideoUrl) }
    probationOfficerName?.let { addPersonalisation("probationOfficerName", probationOfficerName) }
    probationOfficerEmailAddress?.let { addPersonalisation("probationOfficerEmailAddress", probationOfficerEmailAddress) }
    probationOfficerContactNumber?.let { addPersonalisation("probationOfficerContactNumber", probationOfficerContactNumber) }
  }
}

class CancelledProbationBookingUserEmail(
  address: String,
  userName: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  appointmentDate: LocalDate,
  probationTeam: String,
  prison: String,
  appointmentInfo: String,
  comments: String?,
) : ProbationEmail(
  address = address,
  userName = userName,
  prisonerNumber = prisonerNumber,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  appointmentInfo = appointmentInfo,
  appointmentDate = appointmentDate,
  probationTeam = probationTeam,
  prison = prison,
  comments = comments,
)

class CancelledProbationBookingProbationEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  probationTeam: String,
  prison: String,
  appointmentDate: LocalDate,
  appointmentInfo: String,
  comments: String?,
) : ProbationEmail(
  address = address,
  prisonerNumber = prisonerNumber,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  appointmentInfo = appointmentInfo,
  appointmentDate = appointmentDate,
  probationTeam = probationTeam,
  prison = prison,
  comments = comments,
)

class CancelledProbationBookingPrisonProbationEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  probationTeam: String,
  probationEmailAddress: String,
  prison: String,
  appointmentDate: LocalDate,
  appointmentInfo: String,
  comments: String?,
) : ProbationEmail(
  address = address,
  prisonerNumber = prisonerNumber,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  appointmentInfo = appointmentInfo,
  appointmentDate = appointmentDate,
  probationTeam = probationTeam,
  probationEmailAddress = probationEmailAddress,
  prison = prison,
  comments = comments,
)

class CancelledProbationBookingPrisonNoProbationEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  probationTeam: String,
  prison: String,
  appointmentDate: LocalDate,
  appointmentInfo: String,
  comments: String?,
) : ProbationEmail(
  address = address,
  prisonerNumber = prisonerNumber,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  appointmentInfo = appointmentInfo,
  appointmentDate = appointmentDate,
  probationTeam = probationTeam,
  prison = prison,
  comments = comments,
)

class NewProbationBookingUserEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  userName: String,
  appointmentDate: LocalDate,
  probationTeam: String,
  prison: String,
  appointmentInfo: String,
  prisonVideoUrl: String?,
  probationOfficerName: String?,
  probationOfficerEmailAddress: String?,
  probationOfficerContactNumber: String?,
  comments: String?,
) : ProbationEmail(
  address = address,
  prisonerNumber = prisonerNumber,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  appointmentInfo = appointmentInfo,
  appointmentDate = appointmentDate,
  probationTeam = probationTeam,
  prison = prison,
  comments = comments,
  userName = userName,
  prisonVideoUrl = prisonVideoUrl ?: "Not yet known",
  probationOfficerName = probationOfficerName ?: "Not yet known",
  probationOfficerEmailAddress = probationOfficerEmailAddress ?: "Not yet known",
  probationOfficerContactNumber = probationOfficerContactNumber ?: "Not yet known",
)

class NewProbationBookingProbationEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  appointmentDate: LocalDate,
  probationTeam: String,
  prison: String,
  appointmentInfo: String,
  prisonVideoUrl: String?,
  probationOfficerName: String?,
  probationOfficerEmailAddress: String?,
  probationOfficerContactNumber: String?,
  comments: String?,
) : ProbationEmail(
  address = address,
  prisonerNumber = prisonerNumber,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  appointmentInfo = appointmentInfo,
  appointmentDate = appointmentDate,
  probationTeam = probationTeam,
  prison = prison,
  comments = comments,
  prisonVideoUrl = prisonVideoUrl ?: "Not yet known",
  probationOfficerName = probationOfficerName ?: "Not yet known",
  probationOfficerEmailAddress = probationOfficerEmailAddress ?: "Not yet known",
  probationOfficerContactNumber = probationOfficerContactNumber ?: "Not yet known",
)

class NewProbationBookingPrisonProbationEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  appointmentDate: LocalDate,
  probationTeam: String,
  probationEmailAddress: String,
  prison: String,
  appointmentInfo: String,
  prisonVideoUrl: String?,
  probationOfficerName: String?,
  probationOfficerEmailAddress: String?,
  probationOfficerContactNumber: String?,
  comments: String?,
) : ProbationEmail(
  address = address,
  prisonerNumber = prisonerNumber,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  appointmentInfo = appointmentInfo,
  appointmentDate = appointmentDate,
  probationTeam = probationTeam,
  prison = prison,
  comments = comments,
  probationEmailAddress = probationEmailAddress,
  prisonVideoUrl = prisonVideoUrl ?: "Not yet known",
  probationOfficerName = probationOfficerName ?: "Not yet known",
  probationOfficerEmailAddress = probationOfficerEmailAddress ?: "Not yet known",
  probationOfficerContactNumber = probationOfficerContactNumber ?: "Not yet known",
)

class NewProbationBookingPrisonNoProbationEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  appointmentDate: LocalDate,
  probationTeam: String,
  prison: String,
  appointmentInfo: String,
  prisonVideoUrl: String?,
  probationOfficerName: String?,
  probationOfficerEmailAddress: String?,
  probationOfficerContactNumber: String?,
  comments: String?,
) : ProbationEmail(
  address = address,
  prisonerNumber = prisonerNumber,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  appointmentInfo = appointmentInfo,
  appointmentDate = appointmentDate,
  probationTeam = probationTeam,
  prison = prison,
  comments = comments,
  prisonVideoUrl = prisonVideoUrl ?: "Not yet known",
  probationOfficerName = probationOfficerName ?: "Not yet known",
  probationOfficerEmailAddress = probationOfficerEmailAddress ?: "Not yet known",
  probationOfficerContactNumber = probationOfficerContactNumber ?: "Not yet known",
)

class AmendedProbationBookingUserEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  userName: String,
  appointmentDate: LocalDate,
  probationTeam: String,
  prison: String,
  appointmentInfo: String,
  prisonVideoUrl: String?,
  probationOfficerName: String?,
  probationOfficerEmailAddress: String?,
  probationOfficerContactNumber: String?,
  comments: String?,
) : ProbationEmail(
  address = address,
  prisonerNumber = prisonerNumber,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  appointmentInfo = appointmentInfo,
  appointmentDate = appointmentDate,
  probationTeam = probationTeam,
  prison = prison,
  prisonVideoUrl = prisonVideoUrl ?: "Not yet known",
  probationOfficerName = probationOfficerName ?: "Not yet known",
  probationOfficerEmailAddress = probationOfficerEmailAddress ?: "Not yet known",
  probationOfficerContactNumber = probationOfficerContactNumber ?: "Not yet known",
  comments = comments,
  userName = userName,
)

class AmendedProbationBookingProbationEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  probationTeam: String,
  prison: String,
  appointmentDate: LocalDate,
  appointmentInfo: String,
  prisonVideoUrl: String?,
  probationOfficerName: String?,
  probationOfficerEmailAddress: String?,
  probationOfficerContactNumber: String?,
  comments: String?,
) : ProbationEmail(
  address = address,
  prisonerNumber = prisonerNumber,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  appointmentInfo = appointmentInfo,
  appointmentDate = appointmentDate,
  probationTeam = probationTeam,
  prison = prison,
  prisonVideoUrl = prisonVideoUrl ?: "Not yet known",
  probationOfficerName = probationOfficerName ?: "Not yet known",
  probationOfficerEmailAddress = probationOfficerEmailAddress ?: "Not yet known",
  probationOfficerContactNumber = probationOfficerContactNumber ?: "Not yet known",
  comments = comments,
)

class AmendedProbationBookingPrisonProbationEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  probationTeam: String,
  probationEmailAddress: String,
  prison: String,
  appointmentDate: LocalDate,
  appointmentInfo: String,
  prisonVideoUrl: String?,
  probationOfficerName: String?,
  probationOfficerEmailAddress: String?,
  probationOfficerContactNumber: String?,
  comments: String?,
) : ProbationEmail(
  address = address,
  prisonerNumber = prisonerNumber,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  appointmentInfo = appointmentInfo,
  appointmentDate = appointmentDate,
  probationTeam = probationTeam,
  prison = prison,
  comments = comments,
  probationEmailAddress = probationEmailAddress,
  prisonVideoUrl = prisonVideoUrl ?: "Not yet known",
  probationOfficerName = probationOfficerName ?: "Not yet known",
  probationOfficerEmailAddress = probationOfficerEmailAddress ?: "Not yet known",
  probationOfficerContactNumber = probationOfficerContactNumber ?: "Not yet known",
)

class AmendedProbationBookingPrisonNoProbationEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  probationTeam: String,
  prison: String,
  appointmentDate: LocalDate,
  appointmentInfo: String,
  prisonVideoUrl: String?,
  probationOfficerName: String?,
  probationOfficerEmailAddress: String?,
  probationOfficerContactNumber: String?,
  comments: String?,
) : ProbationEmail(
  address = address,
  prisonerNumber = prisonerNumber,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  appointmentInfo = appointmentInfo,
  appointmentDate = appointmentDate,
  probationTeam = probationTeam,
  prison = prison,
  prisonVideoUrl = prisonVideoUrl ?: "Not yet known",
  probationOfficerName = probationOfficerName ?: "Not yet known",
  probationOfficerEmailAddress = probationOfficerEmailAddress ?: "Not yet known",
  probationOfficerContactNumber = probationOfficerContactNumber ?: "Not yet known",
  comments = comments,
)

class TransferredProbationBookingProbationEmail(
  address: String,
  probationTeam: String,
  prison: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  dateOfBirth: LocalDate,
  prisonerNumber: String,
  appointmentDate: LocalDate = LocalDate.now(),
  appointmentInfo: String,
  comments: String?,
) : ProbationEmail(
  address = address,
  prisonerNumber = prisonerNumber,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  appointmentInfo = appointmentInfo,
  appointmentDate = appointmentDate,
  probationTeam = probationTeam,
  prison = prison,
  comments = comments,
  dateOfBirth = dateOfBirth,
)

class TransferredProbationBookingPrisonProbationEmail(
  address: String,
  probationTeam: String,
  probationEmailAddress: String,
  prison: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  dateOfBirth: LocalDate,
  prisonerNumber: String,
  appointmentDate: LocalDate = LocalDate.now(),
  appointmentInfo: String,
  comments: String?,
) : ProbationEmail(
  address = address,
  prisonerNumber = prisonerNumber,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  appointmentInfo = appointmentInfo,
  appointmentDate = appointmentDate,
  probationTeam = probationTeam,
  probationEmailAddress = probationEmailAddress,
  prison = prison,
  comments = comments,
  dateOfBirth = dateOfBirth,
)

class TransferredProbationBookingPrisonNoProbationEmail(
  address: String,
  probationTeam: String,
  prison: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  dateOfBirth: LocalDate,
  prisonerNumber: String,
  appointmentDate: LocalDate = LocalDate.now(),
  appointmentInfo: String,
  comments: String?,
) : ProbationEmail(
  address = address,
  prisonerNumber = prisonerNumber,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  appointmentInfo = appointmentInfo,
  appointmentDate = appointmentDate,
  probationTeam = probationTeam,
  prison = prison,
  comments = comments,
  dateOfBirth = dateOfBirth,
)

class ReleasedProbationBookingProbationEmail(
  address: String,
  probationTeam: String,
  prison: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  dateOfBirth: LocalDate,
  prisonerNumber: String,
  appointmentDate: LocalDate = LocalDate.now(),
  appointmentInfo: String,
  comments: String?,
) : ProbationEmail(
  address = address,
  prisonerNumber = prisonerNumber,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  appointmentInfo = appointmentInfo,
  appointmentDate = appointmentDate,
  probationTeam = probationTeam,
  prison = prison,
  comments = comments,
  dateOfBirth = dateOfBirth,
)

class ReleasedProbationBookingPrisonProbationEmail(
  address: String,
  probationTeam: String,
  probationEmailAddress: String,
  prison: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  dateOfBirth: LocalDate,
  prisonerNumber: String,
  appointmentDate: LocalDate = LocalDate.now(),
  appointmentInfo: String,
  comments: String?,
) : ProbationEmail(
  address = address,
  prisonerNumber = prisonerNumber,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  appointmentInfo = appointmentInfo,
  appointmentDate = appointmentDate,
  probationTeam = probationTeam,
  probationEmailAddress = probationEmailAddress,
  prison = prison,
  comments = comments,
  dateOfBirth = dateOfBirth,
)

class ReleasedProbationBookingPrisonNoProbationEmail(
  address: String,
  probationTeam: String,
  prison: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  dateOfBirth: LocalDate,
  prisonerNumber: String,
  appointmentDate: LocalDate = LocalDate.now(),
  appointmentInfo: String,
  comments: String?,
) : ProbationEmail(
  address = address,
  prisonerNumber = prisonerNumber,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  appointmentInfo = appointmentInfo,
  appointmentDate = appointmentDate,
  probationTeam = probationTeam,
  prison = prison,
  comments = comments,
  dateOfBirth = dateOfBirth,
)

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
  probationOfficerName: String?,
  probationOfficerEmailAddress: String?,
  probationOfficerContactNumber: String?,
) : ProbationEmail(
  address = address,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  probationTeam = probationTeam,
  appointmentDate = date,
  appointmentInfo = appointmentInfo,
  prison = prison,
  comments = comments,
  userName = userName,
  dateOfBirth = dateOfBirth,
  meetingType = meetingType,
  probationOfficerName = probationOfficerName ?: "Not yet known",
  probationOfficerEmailAddress = probationOfficerEmailAddress ?: "Not yet known",
  probationOfficerContactNumber = probationOfficerContactNumber ?: "Not yet known",
)

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
  probationOfficerName: String?,
  probationOfficerEmailAddress: String?,
  probationOfficerContactNumber: String?,
) : ProbationEmail(
  address = address,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  probationTeam = probationTeam,
  appointmentDate = date,
  appointmentInfo = appointmentInfo,
  prison = prison,
  comments = comments,
  dateOfBirth = dateOfBirth,
  meetingType = meetingType,
  probationEmailAddress = probationTeamEmailAddress,
  probationOfficerName = probationOfficerName ?: "Not yet known",
  probationOfficerEmailAddress = probationOfficerEmailAddress ?: "Not yet known",
  probationOfficerContactNumber = probationOfficerContactNumber ?: "Not yet known",
)

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
  probationOfficerName: String?,
  probationOfficerEmailAddress: String?,
  probationOfficerContactNumber: String?,
) : ProbationEmail(
  address = address,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  probationTeam = probationTeam,
  appointmentDate = date,
  appointmentInfo = appointmentInfo,
  prison = prison,
  comments = comments,
  dateOfBirth = dateOfBirth,
  meetingType = meetingType,
  probationOfficerName = probationOfficerName ?: "Not yet known",
  probationOfficerEmailAddress = probationOfficerEmailAddress ?: "Not yet known",
  probationOfficerContactNumber = probationOfficerContactNumber ?: "Not yet known",
)
