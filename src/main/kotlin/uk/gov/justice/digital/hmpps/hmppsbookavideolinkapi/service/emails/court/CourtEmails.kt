package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toMediumFormatStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import java.time.LocalDate

abstract class CourtEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  appointmentDate: LocalDate = LocalDate.now(),
  court: String,
  prison: String,
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
  userName: String? = null,
  courtEmailAddress: String? = null,
) : Email(address, prisonerFirstName, prisonerLastName, appointmentDate, comments) {
  init {
    addPersonalisation("offenderNo", prisonerNumber)
    addPersonalisation("court", court)
    addPersonalisation("prison", prison)
    addPersonalisation("preAppointmentInfo", preAppointmentInfo ?: "Not required")
    addPersonalisation("mainAppointmentInfo", mainAppointmentInfo)
    addPersonalisation("postAppointmentInfo", postAppointmentInfo ?: "Not required")
    userName?.let { addPersonalisation("userName", userName) }
    courtEmailAddress?.let { addPersonalisation("courtEmailAddress", courtEmailAddress) }
  }
}

class NewCourtBookingUserEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  appointmentDate: LocalDate = LocalDate.now(),
  userName: String,
  court: String,
  prison: String,
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : CourtEmail(
  address = address,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  prisonerNumber = prisonerNumber,
  appointmentDate = appointmentDate,
  userName = userName,
  court = court,
  prison = prison,
  preAppointmentInfo = preAppointmentInfo,
  mainAppointmentInfo = mainAppointmentInfo,
  postAppointmentInfo = postAppointmentInfo,
  comments = comments,
)

class NewCourtBookingCourtEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  appointmentDate: LocalDate = LocalDate.now(),
  court: String,
  prison: String,
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : CourtEmail(
  address = address,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  prisonerNumber = prisonerNumber,
  appointmentDate = appointmentDate,
  court = court,
  prison = prison,
  preAppointmentInfo = preAppointmentInfo,
  mainAppointmentInfo = mainAppointmentInfo,
  postAppointmentInfo = postAppointmentInfo,
  comments = comments,
)

class NewCourtBookingPrisonCourtEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  appointmentDate: LocalDate = LocalDate.now(),
  court: String,
  courtEmailAddress: String,
  prison: String,
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : CourtEmail(
  address = address,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  prisonerNumber = prisonerNumber,
  appointmentDate = appointmentDate,
  court = court,
  courtEmailAddress = courtEmailAddress,
  prison = prison,
  preAppointmentInfo = preAppointmentInfo,
  mainAppointmentInfo = mainAppointmentInfo,
  postAppointmentInfo = postAppointmentInfo,
  comments = comments,
)

class NewCourtBookingPrisonNoCourtEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  appointmentDate: LocalDate = LocalDate.now(),
  court: String,
  prison: String,
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : CourtEmail(
  address = address,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  prisonerNumber = prisonerNumber,
  appointmentDate = appointmentDate,
  court = court,
  prison = prison,
  preAppointmentInfo = preAppointmentInfo,
  mainAppointmentInfo = mainAppointmentInfo,
  postAppointmentInfo = postAppointmentInfo,
  comments = comments,
)

class AmendedCourtBookingUserEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  appointmentDate: LocalDate = LocalDate.now(),
  userName: String,
  court: String,
  prison: String,
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : CourtEmail(
  address = address,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  prisonerNumber = prisonerNumber,
  appointmentDate = appointmentDate,
  userName = userName,
  court = court,
  prison = prison,
  preAppointmentInfo = preAppointmentInfo,
  mainAppointmentInfo = mainAppointmentInfo,
  postAppointmentInfo = postAppointmentInfo,
  comments = comments,
)

class AmendedCourtBookingPrisonCourtEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  appointmentDate: LocalDate = LocalDate.now(),
  court: String,
  courtEmailAddress: String,
  prison: String,
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : CourtEmail(
  address = address,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  prisonerNumber = prisonerNumber,
  appointmentDate = appointmentDate,
  court = court,
  courtEmailAddress = courtEmailAddress,
  prison = prison,
  preAppointmentInfo = preAppointmentInfo,
  mainAppointmentInfo = mainAppointmentInfo,
  postAppointmentInfo = postAppointmentInfo,
  comments = comments,
)

class AmendedCourtBookingPrisonNoCourtEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  appointmentDate: LocalDate = LocalDate.now(),
  court: String,
  prison: String,
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : CourtEmail(
  address = address,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  prisonerNumber = prisonerNumber,
  appointmentDate = appointmentDate,
  court = court,
  prison = prison,
  preAppointmentInfo = preAppointmentInfo,
  mainAppointmentInfo = mainAppointmentInfo,
  postAppointmentInfo = postAppointmentInfo,
  comments = comments,
)

class CancelledCourtBookingUserEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  appointmentDate: LocalDate = LocalDate.now(),
  userName: String,
  court: String,
  prison: String,
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : CourtEmail(
  address = address,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  prisonerNumber = prisonerNumber,
  appointmentDate = appointmentDate,
  userName = userName,
  court = court,
  prison = prison,
  preAppointmentInfo = preAppointmentInfo,
  mainAppointmentInfo = mainAppointmentInfo,
  postAppointmentInfo = postAppointmentInfo,
  comments = comments,
)

class CancelledCourtBookingPrisonCourtEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  appointmentDate: LocalDate = LocalDate.now(),
  court: String,
  courtEmailAddress: String,
  prison: String,
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : CourtEmail(
  address = address,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  prisonerNumber = prisonerNumber,
  appointmentDate = appointmentDate,
  court = court,
  courtEmailAddress = courtEmailAddress,
  prison = prison,
  preAppointmentInfo = preAppointmentInfo,
  mainAppointmentInfo = mainAppointmentInfo,
  postAppointmentInfo = postAppointmentInfo,
  comments = comments,
)

class CancelledCourtBookingPrisonNoCourtEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  appointmentDate: LocalDate = LocalDate.now(),
  court: String,
  prison: String,
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
) : CourtEmail(
  address = address,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  prisonerNumber = prisonerNumber,
  appointmentDate = appointmentDate,
  court = court,
  prison = prison,
  preAppointmentInfo = preAppointmentInfo,
  mainAppointmentInfo = mainAppointmentInfo,
  postAppointmentInfo = postAppointmentInfo,
  comments = comments,
)

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
