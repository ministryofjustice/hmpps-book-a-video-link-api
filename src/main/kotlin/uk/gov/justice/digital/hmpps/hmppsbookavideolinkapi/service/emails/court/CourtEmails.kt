package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toHourMinuteStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toMediumFormatStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import java.time.LocalDate
import java.time.LocalTime

@Deprecated(message = "Deprecated", replaceWith = ReplaceWith("CourtEmail"))
abstract class DeprecatedCourtEmail(
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
  courtHearingLink: String?,
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
    addPersonalisation("courtHearingLink", courtHearingLink ?: "Not yet known")
    userName?.let { addPersonalisation("userName", userName) }
    courtEmailAddress?.let { addPersonalisation("courtEmailAddress", courtEmailAddress) }
  }
}

abstract class CourtEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  appointmentDate: LocalDate = LocalDate.now(),
  court: String,
  prison: String,
  preAppointmentDetails: AppointmentDetails?,
  mainAppointmentDetails: AppointmentDetails,
  postAppointmentDetails: AppointmentDetails?,
  comments: String?,
  courtHearingLink: String?,
  userName: String? = null,
  courtEmailAddress: String? = null,
) : Email(address, prisonerFirstName, prisonerLastName, appointmentDate, comments) {
  init {
    addPersonalisation("offenderNo", prisonerNumber)
    addPersonalisation("court", court)
    addPersonalisation("prison", prison)
    addPersonalisation("preAppointmentInfo", preAppointmentDetails?.toString() ?: "Not required")
    addPersonalisation("prePrisonVideoUrl", preAppointmentDetails?.prisonVideoUrl?.let { "Pre-court hearing link (PVL): $it" } ?: "")
    addPersonalisation("mainAppointmentInfo", mainAppointmentDetails.toString())
    addPersonalisation("postAppointmentInfo", postAppointmentDetails?.toString() ?: "Not required")
    addPersonalisation("postPrisonVideoUrl", postAppointmentDetails?.prisonVideoUrl?.let { "Post-court hearing link (PVL): $it" } ?: "")
    addPersonalisation("courtHearingLink", courtHearingLink ?: "Not yet known")
    userName?.let { addPersonalisation("userName", userName) }
    courtEmailAddress?.let { addPersonalisation("courtEmailAddress", courtEmailAddress) }
  }
}

data class AppointmentDetails(val description: String, val startTime: LocalTime, val endTime: LocalTime, val prisonVideoUrl: String?) {
  override fun toString() = "$description - ${startTime.toHourMinuteStyle()} to ${endTime.toHourMinuteStyle()}"
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
  preAppointmentDetails: AppointmentDetails?,
  mainAppointmentDetails: AppointmentDetails,
  postAppointmentDetails: AppointmentDetails?,
  comments: String?,
  courtHearingLink: String?,
) : CourtEmail(
  address = address,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  prisonerNumber = prisonerNumber,
  appointmentDate = appointmentDate,
  userName = userName,
  court = court,
  prison = prison,
  preAppointmentDetails = preAppointmentDetails,
  mainAppointmentDetails = mainAppointmentDetails,
  postAppointmentDetails = postAppointmentDetails,
  comments = comments,
  courtHearingLink = courtHearingLink,
)

class NewCourtBookingCourtEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  appointmentDate: LocalDate = LocalDate.now(),
  court: String,
  prison: String,
  preAppointmentDetails: AppointmentDetails?,
  mainAppointmentDetails: AppointmentDetails,
  postAppointmentDetails: AppointmentDetails?,
  comments: String?,
  courtHearingLink: String?,
) : CourtEmail(
  address = address,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  prisonerNumber = prisonerNumber,
  appointmentDate = appointmentDate,
  court = court,
  prison = prison,
  preAppointmentDetails = preAppointmentDetails,
  mainAppointmentDetails = mainAppointmentDetails,
  postAppointmentDetails = postAppointmentDetails,
  comments = comments,
  courtHearingLink = courtHearingLink,
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
  preAppointmentDetails: AppointmentDetails?,
  mainAppointmentDetails: AppointmentDetails,
  postAppointmentDetails: AppointmentDetails?,
  comments: String?,
  courtHearingLink: String?,
) : CourtEmail(
  address = address,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  prisonerNumber = prisonerNumber,
  appointmentDate = appointmentDate,
  court = court,
  courtEmailAddress = courtEmailAddress,
  prison = prison,
  preAppointmentDetails = preAppointmentDetails,
  mainAppointmentDetails = mainAppointmentDetails,
  postAppointmentDetails = postAppointmentDetails,
  comments = comments,
  courtHearingLink = courtHearingLink,
)

class NewCourtBookingPrisonNoCourtEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  appointmentDate: LocalDate = LocalDate.now(),
  court: String,
  prison: String,
  preAppointmentDetails: AppointmentDetails?,
  mainAppointmentDetails: AppointmentDetails,
  postAppointmentDetails: AppointmentDetails?,
  comments: String?,
  courtHearingLink: String?,
) : CourtEmail(
  address = address,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  prisonerNumber = prisonerNumber,
  appointmentDate = appointmentDate,
  court = court,
  prison = prison,
  preAppointmentDetails = preAppointmentDetails,
  mainAppointmentDetails = mainAppointmentDetails,
  postAppointmentDetails = postAppointmentDetails,
  comments = comments,
  courtHearingLink = courtHearingLink,
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
  preAppointmentDetails: AppointmentDetails?,
  mainAppointmentDetails: AppointmentDetails,
  postAppointmentDetails: AppointmentDetails?,
  comments: String?,
  courtHearingLink: String?,
) : CourtEmail(
  address = address,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  prisonerNumber = prisonerNumber,
  appointmentDate = appointmentDate,
  userName = userName,
  court = court,
  prison = prison,
  preAppointmentDetails = preAppointmentDetails,
  mainAppointmentDetails = mainAppointmentDetails,
  postAppointmentDetails = postAppointmentDetails,
  comments = comments,
  courtHearingLink = courtHearingLink,
)

class AmendedCourtBookingCourtEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  appointmentDate: LocalDate = LocalDate.now(),
  court: String,
  prison: String,
  preAppointmentDetails: AppointmentDetails?,
  mainAppointmentDetails: AppointmentDetails,
  postAppointmentDetails: AppointmentDetails?,
  comments: String?,
  courtHearingLink: String?,
) : CourtEmail(
  address = address,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  prisonerNumber = prisonerNumber,
  appointmentDate = appointmentDate,
  court = court,
  prison = prison,
  preAppointmentDetails = preAppointmentDetails,
  mainAppointmentDetails = mainAppointmentDetails,
  postAppointmentDetails = postAppointmentDetails,
  comments = comments,
  courtHearingLink = courtHearingLink,
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
  preAppointmentDetails: AppointmentDetails?,
  mainAppointmentDetails: AppointmentDetails,
  postAppointmentDetails: AppointmentDetails?,
  comments: String?,
  courtHearingLink: String?,
) : CourtEmail(
  address = address,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  prisonerNumber = prisonerNumber,
  appointmentDate = appointmentDate,
  court = court,
  courtEmailAddress = courtEmailAddress,
  prison = prison,
  preAppointmentDetails = preAppointmentDetails,
  mainAppointmentDetails = mainAppointmentDetails,
  postAppointmentDetails = postAppointmentDetails,
  comments = comments,
  courtHearingLink = courtHearingLink,
)

class AmendedCourtBookingPrisonNoCourtEmail(
  address: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  appointmentDate: LocalDate = LocalDate.now(),
  court: String,
  prison: String,
  preAppointmentDetails: AppointmentDetails?,
  mainAppointmentDetails: AppointmentDetails,
  postAppointmentDetails: AppointmentDetails?,
  comments: String?,
  courtHearingLink: String?,
) : CourtEmail(
  address = address,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  prisonerNumber = prisonerNumber,
  appointmentDate = appointmentDate,
  court = court,
  prison = prison,
  preAppointmentDetails = preAppointmentDetails,
  mainAppointmentDetails = mainAppointmentDetails,
  postAppointmentDetails = postAppointmentDetails,
  comments = comments,
  courtHearingLink = courtHearingLink,
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
  courtHearingLink: String?,
) : DeprecatedCourtEmail(
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
  courtHearingLink = courtHearingLink,
)

class CancelledCourtBookingCourtEmail(
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
  courtHearingLink: String?,
) : DeprecatedCourtEmail(
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
  courtHearingLink = courtHearingLink,
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
  courtHearingLink: String?,
) : DeprecatedCourtEmail(
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
  courtHearingLink = courtHearingLink,
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
  courtHearingLink: String?,
) : DeprecatedCourtEmail(
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
  courtHearingLink = courtHearingLink,
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
  courtHearingLink: String?,
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
    addPersonalisation("courtHearingLink", courtHearingLink ?: "Not yet known")
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
  courtHearingLink: String?,
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
    addPersonalisation("courtHearingLink", courtHearingLink ?: "Not yet known")
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
  courtHearingLink: String?,
) : Email(address, prisonerFirstName, prisonerLastName, date, comments) {
  init {
    addPersonalisation("dateOfBirth", dateOfBirth.toMediumFormatStyle())
    addPersonalisation("court", court)
    addPersonalisation("prison", prison)
    addPersonalisation("hearingType", hearingType)
    addPersonalisation("preAppointmentInfo", preAppointmentInfo ?: "Not required")
    addPersonalisation("mainAppointmentInfo", mainAppointmentInfo)
    addPersonalisation("postAppointmentInfo", postAppointmentInfo ?: "Not required")
    addPersonalisation("courtHearingLink", courtHearingLink ?: "Not yet known")
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

class CourtHearingLinkReminderEmail(
  address: String,
  prison: String,
  prisonerFirstName: String,
  prisonerLastName: String,
  prisonerNumber: String,
  court: String,
  date: LocalDate = LocalDate.now(),
  preAppointmentInfo: String?,
  mainAppointmentInfo: String,
  postAppointmentInfo: String?,
  comments: String?,
  bookingId: String,
) : DeprecatedCourtEmail(
  address = address,
  prisonerFirstName = prisonerFirstName,
  prisonerLastName = prisonerLastName,
  prisonerNumber = prisonerNumber,
  appointmentDate = date,
  court = court,
  prison = prison,
  preAppointmentInfo = preAppointmentInfo,
  mainAppointmentInfo = mainAppointmentInfo,
  postAppointmentInfo = postAppointmentInfo,
  comments = comments,
  courtHearingLink = null,
) {
  init {
    addPersonalisation("bookingId", bookingId)
  }
}
