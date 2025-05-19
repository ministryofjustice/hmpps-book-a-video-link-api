package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AdditionalBookingDetail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Contact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationAttribute
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationScheduleUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ProbationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

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

fun probationTeam(code: String = "BLKPPP", enabled: Boolean = true, readOnly: Boolean = false) = ProbationTeam(
  probationTeamId = 0,
  code = code,
  description = "probation team description",
  enabled = enabled,
  readOnly = readOnly,
  notes = null,
  createdBy = "Test",
)

fun courtBooking(createdBy: String = "court_user", createdByPrison: Boolean = false, court: Court = court(), comments: String? = "Court hearing comments") = VideoBooking.newCourtBooking(
  court = court,
  hearingType = "TRIBUNAL",
  comments = comments,
  videoUrl = "https://court.hearing.link",
  createdBy = if (createdByPrison) prisonUser(createdBy) else courtUser(createdBy),
  notesForStaff = "Some private staff notes",
  notesForPrisoners = "Some public prisoners notes",
)

fun bookingHistory(historyType: HistoryType, booking: VideoBooking, comments: String? = "history comments") = BookingHistory(
  bookingHistoryId = 1L,
  videoBookingId = booking.videoBookingId,
  historyType = historyType,
  courtId = booking.court?.courtId,
  probationTeamId = booking.probationTeam?.probationTeamId,
  hearingType = CourtHearingType.TRIBUNAL.name,
  videoUrl = "https://edited.video.url",
  comments = comments,
  createdBy = booking.createdBy,
)

/**
 * This adds a prison appointment to meet the basic needs of a test. We don't care about the details of the appointment.
 */
fun VideoBooking.withMainCourtPrisonAppointment(
  date: LocalDate = tomorrow(),
  prisonCode: String = BIRMINGHAM,
  location: Location = birminghamLocation,
  prisonerNumber: String = "123456",
  startTime: LocalTime = LocalTime.of(9, 30),
  endTime: LocalTime = LocalTime.of(10, 0),
) = addAppointment(
  prison = prison(prisonCode = prisonCode),
  prisonerNumber = prisonerNumber,
  appointmentType = "VLB_COURT_MAIN",
  locationId = location.id,
  date = date,
  startTime = startTime,
  endTime = endTime,
)

/**
 * This adds a prison pre and main appointment to meet the basic needs of a test. We don't care about the details of the appointment.
 */
fun VideoBooking.withPreMainCourtPrisonAppointment(
  date: LocalDate = tomorrow(),
  prisonCode: String = BIRMINGHAM,
  location: Location = birminghamLocation,
  prisonerNumber: String = "123456",
  startTime: LocalTime = LocalTime.of(9, 30),
  endTime: LocalTime = LocalTime.of(10, 0),
) = apply {
  addAppointment(
    prison = prison(prisonCode = prisonCode),
    prisonerNumber = prisonerNumber,
    appointmentType = "VLB_COURT_PRE",
    locationId = location.id,
    date = date,
    startTime = startTime.minusMinutes(15),
    endTime = endTime.minusMinutes(15),
  )
  addAppointment(
    prison = prison(prisonCode = prisonCode),
    prisonerNumber = prisonerNumber,
    appointmentType = "VLB_COURT_MAIN",
    locationId = location.id,
    date = date,
    startTime = startTime,
    endTime = endTime,
  )
}

/**
 * This adds a prison pre, main and post appointment to meet the basic needs of a test. We don't care about the details of the appointment.
 */
fun VideoBooking.withPreMainPostCourtPrisonAppointment(
  date: LocalDate = tomorrow(),
  prisonCode: String = BIRMINGHAM,
  location: Location = birminghamLocation,
  prisonerNumber: String = "123456",
  startTime: LocalTime = LocalTime.of(9, 30),
  endTime: LocalTime = LocalTime.of(10, 0),
) = apply {
  addAppointment(
    prison = prison(prisonCode = prisonCode),
    prisonerNumber = prisonerNumber,
    appointmentType = "VLB_COURT_PRE",
    locationId = location.id,
    date = date,
    startTime = startTime.minusMinutes(15),
    endTime = endTime.minusMinutes(15),
  )
  addAppointment(
    prison = prison(prisonCode = prisonCode),
    prisonerNumber = prisonerNumber,
    appointmentType = "VLB_COURT_MAIN",
    locationId = location.id,
    date = date,
    startTime = startTime,
    endTime = endTime,
  )
  addAppointment(
    prison = prison(prisonCode = prisonCode),
    prisonerNumber = prisonerNumber,
    appointmentType = "VLB_COURT_POST",
    locationId = location.id,
    date = date,
    startTime = startTime.plusMinutes(15),
    endTime = endTime.plusMinutes(15),
  )
}

fun probationBooking(probationTeam: ProbationTeam = probationTeam(), meetingType: ProbationMeetingType = ProbationMeetingType.PSR, createdBy: User? = null, comments: String? = "Probation meeting comments") = VideoBooking.newProbationBooking(
  probationTeam = probationTeam,
  probationMeetingType = meetingType.name,
  comments = comments,
  createdBy = createdBy ?: PROBATION_USER,
  notesForStaff = "Some private staff notes",
  notesForPrisoners = "Some public prisoners notes",
)

/**
 * This adds a prison appointment to meet the basic needs of a test. We don't care about the details of the appointment.
 */
fun VideoBooking.withProbationPrisonAppointment(
  date: LocalDate = tomorrow(),
  prisonCode: String = BIRMINGHAM,
  location: Location = birminghamLocation,
  prisonerNumber: String = "123456",
  startTime: LocalTime = LocalTime.MIDNIGHT,
  endTime: LocalTime = LocalTime.MIDNIGHT.plusHours(1),
) = addAppointment(
  prison = prison(prisonCode = prisonCode),
  prisonerNumber = prisonerNumber,
  appointmentType = "VLB_PROBATION",
  date = date,
  startTime = startTime,
  endTime = endTime,
  locationId = location.id,
)

fun appointment(
  booking: VideoBooking,
  prisonCode: String,
  prisonerNumber: String,
  date: LocalDate = tomorrow(),
  startTime: LocalTime = LocalTime.of(9, 0),
  endTime: LocalTime = LocalTime.of(10, 0),
  appointmentType: String,
  locationId: UUID,
) = PrisonAppointment.newAppointment(
  videoBooking = booking,
  prison = prison(prisonCode = prisonCode),
  prisonerNumber = prisonerNumber,
  appointmentType = appointmentType,
  appointmentDate = date,
  startTime = startTime,
  endTime = endTime,
  locationId = locationId,
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
) = VideoAppointment(
  videoBookingId = booking.videoBookingId,
  prisonAppointmentId = prisonAppointment.prisonAppointmentId,
  bookingType = booking.bookingType.name,
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
  lastCreatedOrAmended = booking.amendedTime ?: booking.createdTime,
)

fun videoRoomAttributesWithSchedule(
  prisonCode: String,
  dpsLocationId: UUID,
  locationStatus: LocationStatus = LocationStatus.ACTIVE,
  locationUsage: LocationUsage = LocationUsage.SCHEDULE,
) = LocationAttribute.decoratedRoom(
  dpsLocationId = dpsLocationId,
  prison = Prison(
    prisonId = 1,
    code = prisonCode,
    name = "TEST",
    enabled = true,
    createdBy = "TEST",
    notes = null,
  ),
  locationUsage = locationUsage,
  locationStatus = locationStatus,
  allowedParties = emptySet(),
  prisonVideoUrl = null,
  notes = null,
  createdBy = COURT_USER,
).apply {
  addSchedule(
    usage = LocationScheduleUsage.BLOCKED,
    startDayOfWeek = DayOfWeek.MONDAY.value,
    endDayOfWeek = DayOfWeek.SUNDAY.value,
    startTime = LocalTime.of(1, 0),
    endTime = LocalTime.of(23, 0),
    allowedParties = emptySet(),
    createdBy = COURT_USER,
  )
}

fun videoRoomAttributesWithoutSchedule(
  prisonCode: String,
  dpsLocationId: UUID,
  locationStatus: LocationStatus = LocationStatus.ACTIVE,
  locationUsage: LocationUsage = LocationUsage.SHARED,
) = LocationAttribute.decoratedRoom(
  dpsLocationId = dpsLocationId,
  prison = Prison(
    prisonId = 1,
    code = prisonCode,
    name = "TEST",
    enabled = true,
    createdBy = "TEST",
    notes = null,
  ),
  locationUsage = locationUsage,
  locationStatus = locationStatus,
  allowedParties = emptySet(),
  prisonVideoUrl = null,
  notes = null,
  createdBy = COURT_USER,
)

fun additionalDetails(
  booking: VideoBooking,
  contactName: String = "contact name",
  contactEmail: String = "contact@email.com",
  contactNumber: String = "0114 2345678",
) = AdditionalBookingDetail.newDetails(
  videoBooking = booking,
  contactName = contactName,
  contactEmail = contactEmail,
  contactPhoneNumber = contactNumber,
)

fun VideoBooking.hasBookingType(that: BookingType): VideoBooking = also { it.bookingType isEqualTo that }
fun VideoBooking.hasProbationTeam(that: ProbationTeam): VideoBooking = also { it.probationTeam isEqualTo that }
fun VideoBooking.hasMeetingType(that: ProbationMeetingType): VideoBooking = also { it.probationMeetingType isEqualTo that.name }
fun VideoBooking.hasComments(that: String): VideoBooking = also { it.comments isEqualTo that }
fun VideoBooking.hasVideoUrl(that: String): VideoBooking = also { it.videoUrl isEqualTo that }
fun VideoBooking.hasCreatedBy(that: User): VideoBooking = also { it.createdBy isEqualTo that.username }
fun VideoBooking.hasCreatedTimeCloseTo(that: LocalDateTime) = also { it.createdTime isCloseTo that }
fun VideoBooking.hasCreatedByPrison(that: Boolean): VideoBooking = also { it.createdByPrison isBool that }
fun VideoBooking.hasAmendedBy(that: User): VideoBooking = also { it.amendedBy isEqualTo that.username }
fun VideoBooking.hasAmendedTimeCloseTo(that: LocalDateTime) = also { it.amendedTime isCloseTo that }
fun VideoBooking.hasStaffNotes(that: String): VideoBooking = also { it.notesForStaff isEqualTo that }
fun VideoBooking.hasPrisonersNotes(that: String?): VideoBooking = also { it.notesForPrisoners isEqualTo that }

fun PrisonAppointment.hasPrisonCode(that: String): PrisonAppointment = also { it.prisonCode() isEqualTo that }
fun PrisonAppointment.hasPrisonerNumber(that: String): PrisonAppointment = also { it.prisonerNumber isEqualTo that }
fun PrisonAppointment.hasAppointmentTypeProbation(): PrisonAppointment = also { it.appointmentType isEqualTo "VLB_PROBATION" }
fun PrisonAppointment.hasAppointmentDate(that: LocalDate): PrisonAppointment = also { it.appointmentDate isEqualTo that }
fun PrisonAppointment.hasStartTime(that: LocalTime): PrisonAppointment = also { it.startTime isEqualTo that }
fun PrisonAppointment.hasEndTime(that: LocalTime): PrisonAppointment = also { it.endTime isEqualTo that }
fun PrisonAppointment.hasLocation(that: Location): PrisonAppointment = also { it.prisonLocationId isEqualTo that.id }
fun PrisonAppointment.hasComments(that: String): PrisonAppointment = also { it.comments isEqualTo that }

fun AdditionalBookingDetail.hasContactName(that: String): AdditionalBookingDetail = also { it.contactName isEqualTo that }
fun AdditionalBookingDetail.hasEmailAddress(that: String): AdditionalBookingDetail = also { it.contactEmail isEqualTo that }
fun AdditionalBookingDetail.hasPhoneNumber(that: String): AdditionalBookingDetail = also { it.contactNumber isEqualTo that }
fun BookingHistory.hasHistoryType(that: HistoryType): BookingHistory = also { it.historyType isEqualTo that }
fun BookingHistory.hasProbationMeetingType(that: ProbationMeetingType): BookingHistory = also { it.probationMeetingType isEqualTo that.name }
fun BookingHistory.hasProbationTeam(that: ProbationTeam): BookingHistory = also { it.probationTeamId isEqualTo that.probationTeamId }
