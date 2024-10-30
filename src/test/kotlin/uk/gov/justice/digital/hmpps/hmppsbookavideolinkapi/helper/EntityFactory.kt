package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Contact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ProbationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import java.time.LocalDate
import java.time.LocalTime

val courtAppealReferenceCode = ReferenceCode(1, "COURT_HEARING_TYPE", "APPEAL", "Appeal", "TEST", enabled = true)

fun court(code: String = DERBY_JUSTICE_CENTRE, enabled: Boolean = true) = Court(
  courtId = 0,
  code = code,
  description = code,
  enabled = enabled,
  notes = null,
  createdBy = "Test",
)

fun readOnlyCourt() = Court(
  courtId = 0,
  code = "UNKNOWN",
  description = "Unknown court",
  enabled = false,
  readOnly = true,
  notes = null,
  createdBy = "Test",
)

fun prison(prisonCode: String = BIRMINGHAM, enabled: Boolean = true) = Prison(
  prisonId = 0,
  code = prisonCode,
  name = prisonNames[prisonCode] ?: "Unknown prison",
  enabled = enabled,
  notes = null,
  createdBy = "Test",
)

fun probationTeam(code: String = "BLKPPP", enabled: Boolean = true) = ProbationTeam(
  probationTeamId = 0,
  code = code,
  description = "probation team description",
  enabled = enabled,
  notes = null,
  createdBy = "Test",
)

fun courtBooking(createdBy: String = "court_user", createdByPrison: Boolean = false, court: Court = court()) = VideoBooking.newCourtBooking(
  court = court,
  hearingType = "TRIBUNAL",
  comments = "Court hearing comments",
  videoUrl = "https://court.hearing.link",
  createdBy = createdBy,
  createdByPrison = createdByPrison,
)

/**
 * This adds a prison appointment to meet the basic needs of a test. We don't care about the details of the appointment.
 */
fun VideoBooking.withMainCourtPrisonAppointment(
  date: LocalDate = tomorrow(),
  prisonCode: String = BIRMINGHAM,
  location: Location = birminghamLocation,
  prisonerNumber: String = "123456",
) =
  addAppointment(
    prison = prison(prisonCode = prisonCode),
    prisonerNumber = prisonerNumber,
    appointmentType = "VLB_COURT_MAIN",
    locationId = location.key,
    date = date,
    startTime = LocalTime.of(9, 30),
    endTime = LocalTime.of(10, 0),
  )

fun probationBooking(probationTeam: ProbationTeam = probationTeam()) = VideoBooking.newProbationBooking(
  probationTeam = probationTeam,
  probationMeetingType = "PSR",
  comments = "Probation meeting comments",
  videoUrl = "https://probation.meeting.link",
  createdBy = "Probation team user",
  createdByPrison = false,
)

/**
 * This adds a prison appointment to meet the basic needs of a test. We don't care about the details of the appointment.
 */
fun VideoBooking.withProbationPrisonAppointment(
  prisonCode: String = BIRMINGHAM,
  location: Location = birminghamLocation,
) = addAppointment(
  prison = prison(prisonCode = prisonCode),
  prisonerNumber = "123456",
  appointmentType = "VLB_PROBATION",
  date = tomorrow(),
  startTime = LocalTime.MIDNIGHT,
  endTime = LocalTime.MIDNIGHT.plusHours(1),
  locationId = location.key,
)

fun appointment(
  booking: VideoBooking,
  prisonCode: String,
  prisonerNumber: String,
  date: LocalDate = tomorrow(),
  startTime: LocalTime = LocalTime.of(9, 0),
  endTime: LocalTime = LocalTime.of(10, 0),
  appointmentType: String,
  locationKey: String,
) = PrisonAppointment.newAppointment(
  videoBooking = booking,
  prison = prison(prisonCode = prisonCode),
  prisonerNumber = prisonerNumber,
  appointmentType = appointmentType,
  appointmentDate = date,
  startTime = startTime,
  endTime = endTime,
  locationId = locationKey,
)

fun bookingContact(contactType: ContactType, email: String?, name: String? = null) = BookingContact(
  videoBookingId = 0,
  contactType = contactType,
  name = name,
  position = null,
  email = email,
  telephone = null,
  primaryContact = true,
)

fun contact(contactType: ContactType, email: String?, name: String? = null) = Contact(
  contactType = contactType,
  code = "code",
  name = name,
  position = null,
  email = email,
  telephone = null,
  primaryContact = true,
)

fun courtHearingType(description: String) = ReferenceCode(
  referenceCodeId = 0,
  groupCode = "COURT_HEARING_TYPE",
  code = "code",
  description = description,
  createdBy = "Test User",
  enabled = true,
)

fun probationMeetingType(description: String) = ReferenceCode(
  referenceCodeId = 0,
  groupCode = "PROBATION_MEETING_TYPE",
  code = "code",
  description = description,
  createdBy = "Test User",
  enabled = true,
)

fun videoAppointment(
  booking: VideoBooking,
  prisonAppointment: PrisonAppointment,
) =
  VideoAppointment(
    videoBookingId = booking.videoBookingId,
    prisonAppointmentId = prisonAppointment.prisonAppointmentId,
    bookingType = booking.bookingType,
    statusCode = booking.statusCode.name,
    courtCode = booking.court?.code,
    probationTeamCode = null,
    prisonCode = WANDSWORTH,
    prisonerNumber = "A1234AA",
    appointmentType = prisonAppointment.appointmentType,
    prisonLocationId = prisonAppointment.prisonLocationId,
    appointmentDate = prisonAppointment.appointmentDate,
    startTime = prisonAppointment.startTime,
    endTime = prisonAppointment.endTime,
  )
