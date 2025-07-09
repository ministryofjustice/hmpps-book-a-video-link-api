package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.EmailAddressDto
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.UserDetailsDto
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.UserDetailsDto.AuthSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDateTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.RoomAttributes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AdditionalBookingDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.Appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.RequestVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.RequestedAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.UnknownPrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.VideoLinkBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner as ModelPrisoner

val birminghamLocation = location(prisonCode = BIRMINGHAM, locationKeySuffix = "ABCEDFG", localName = "Birmingham room")
val birminghamLocation2 = location(prisonCode = BIRMINGHAM, locationKeySuffix = "XXXXXXX", localName = "Birmingham room 2")
val inactiveBirminghamLocation = location(prisonCode = BIRMINGHAM, locationKeySuffix = "HIJLKLM", active = false)
val norwichLocation = location(prisonCode = NORWICH, locationKeySuffix = "ABCDEFG")
val pentonvilleLocation = location(prisonCode = PENTONVILLE, locationKeySuffix = "ABCDEFG", localName = "Pentonville room 3")
val risleyLocation = location(prisonCode = RISLEY, locationKeySuffix = "ABCDEFG", localName = "Risley room")
val risleyLocation2 = location(prisonCode = RISLEY, locationKeySuffix = "ABCDEFG", localName = "Risley room 2")
val wandsworthLocation = location(prisonCode = WANDSWORTH, locationKeySuffix = "ABCEDFG", localName = "Wandsworth room")
val wandsworthLocation2 =
  location(prisonCode = WANDSWORTH, locationKeySuffix = "ABCEDFG2", localName = "Wandsworth room 2")
val wandsworthLocation3 =
  location(prisonCode = WANDSWORTH, locationKeySuffix = "ABCEDFG3", localName = "Wandsworth room 3")

val allLocations = setOf(
  birminghamLocation,
  inactiveBirminghamLocation,
  norwichLocation,
  pentonvilleLocation,
  risleyLocation,
  risleyLocation2,
  wandsworthLocation,
  wandsworthLocation2,
  wandsworthLocation3,
)

fun location(prisonCode: String, locationKeySuffix: String, active: Boolean = true, localName: String? = null, id: UUID = UUID.randomUUID()) = Location(
  id = id,
  prisonId = prisonCode,
  code = "VIDEOLINK",
  pathHierarchy = "VIDEOLINK",
  locationType = Location.LocationType.VIDEO_LINK,
  permanentlyInactive = false,
  active = active,
  deactivatedByParent = false,
  topLevelId = UUID.randomUUID(),
  key = "$prisonCode-$locationKeySuffix",
  isResidential = false,
  localName = localName,
  lastModifiedBy = "test user",
  lastModifiedDate = LocalDateTime.now().toIsoDateTime(),
  level = 2,
  leafLevel = true,
  status = Location.Status.ACTIVE,
)

fun locationAttributes() = RoomAttributes(
  attributeId = 1,
  locationUsage = LocationUsage.SHARED,
  locationStatus = LocationStatus.ACTIVE,
  expectedActiveDate = LocalDate.now(),
  notes = null,
  prisonVideoUrl = "decorated-video-link-url",
  statusMessage = null,
)

fun prisonerSearchPrisoner(
  prisonerNumber: String,
  prisonCode: String,
  firstName: String = "Fred",
  lastName: String = "Bloggs",
  bookingId: Long = -1,
  lastPrisonCode: String? = null,
) = Prisoner(
  prisonerNumber = prisonerNumber,
  prisonId = prisonCode,
  firstName = firstName,
  lastName = lastName,
  bookingId = bookingId.toString(),
  dateOfBirth = LocalDate.of(2000, 1, 1),
  lastPrisonId = lastPrisonCode,
)

fun userEmailAddress(username: String, email: String, verified: Boolean = true) = EmailAddressDto(username, verified, email)

fun userDetails(
  username: String,
  name: String = "Test User",
  authSource: AuthSource = AuthSource.auth,
  activeCaseLoadId: String? = null,
  userId: String = "TEST",
) = UserDetailsDto(
  userId = userId,
  username = username,
  active = true,
  name = name,
  authSource = authSource,
  activeCaseLoadId = activeCaseLoadId,
)

fun serviceUser() = UserService.getServiceAsUser()

fun prisonUser(
  username: String = "prison_user",
  name: String = "Prison User",
  email: String? = "prison.user@prison.com",
  activeCaseLoadId: String = BIRMINGHAM,
) = PrisonUser(
  username = username,
  name = name,
  email = email,
  activeCaseLoadId = activeCaseLoadId,
)

fun courtUser(username: String = "user", name: String = "Test User", email: String? = null) = ExternalUser(
  username = username,
  name = name,
  email = email,
  isCourtUser = true,
  isProbationUser = false,
  courts = courts,
)

fun probationUser(username: String = "user", name: String = "Test User", email: String? = null) = ExternalUser(
  username = username,
  name = name,
  email = email,
  isProbationUser = true,
  isCourtUser = false,
  probationTeams = probationTeams,
)

fun prisoner(
  prisonerNumber: String,
  prisonCode: String,
  firstName: String = "Fred",
  lastName: String = "Bloggs",
) = ModelPrisoner(
  prisonerNumber = prisonerNumber,
  prisonCode = prisonCode,
  firstName = firstName,
  lastName = lastName,
  dateOfBirth = LocalDate.of(2000, 1, 1),
)

fun courtBookingRequest(
  courtCode: String = "DRBYMC",
  prisonCode: String = "WWI",
  prisonerNumber: String = "123456",
  locationSuffix: String = "A-1-001",
  location: Location? = null,
  date: LocalDate = tomorrow(),
  startTime: LocalTime = LocalTime.now(),
  endTime: LocalTime = LocalTime.now().plusHours(1),
  appointments: List<Appointment> = emptyList(),
  videoLinkUrl: String? = "https://video.link.com",
  notesForStaff: String = "Some private staff notes",
  notesForPrisoners: String = "Some public prisoners notes",
): CreateVideoBookingRequest {
  val prisoner = PrisonerDetails(
    prisonCode = prisonCode,
    prisonerNumber = prisonerNumber,
    appointments = appointments.ifEmpty {
      listOf(
        Appointment(
          type = AppointmentType.VLB_COURT_MAIN,
          locationKey = location?.key ?: "$prisonCode-$locationSuffix",
          date = date,
          startTime = startTime,
          endTime = endTime,
        ),
      )
    },
  )

  return CreateVideoBookingRequest(
    bookingType = BookingType.COURT,
    courtCode = courtCode,
    courtHearingType = CourtHearingType.TRIBUNAL,
    prisoners = listOf(prisoner),
    videoLinkUrl = videoLinkUrl,
    notesForStaff = notesForStaff,
    notesForPrisoners = notesForPrisoners,
  )
}

fun requestCourtVideoLinkRequest(
  courtCode: String = "DRBYMC",
  prisonCode: String = "WWI",
  firstName: String = "John",
  lastName: String = "Smith",
  dateOfBirth: LocalDate = LocalDate.of(1970, 1, 1),
  startTime: LocalTime = LocalTime.now(),
  endTime: LocalTime = LocalTime.now().plusHours(1),
  appointments: List<RequestedAppointment> = emptyList(),
  notesForStaff: String? = null,
): RequestVideoBookingRequest {
  val prisoner = UnknownPrisonerDetails(
    prisonCode = prisonCode,
    firstName = firstName,
    lastName = lastName,
    dateOfBirth = dateOfBirth,
    appointments = appointments.ifEmpty {
      listOf(
        RequestedAppointment(
          type = AppointmentType.VLB_COURT_MAIN,
          date = tomorrow(),
          startTime = startTime,
          endTime = endTime,
        ),
      )
    },
  )

  return RequestVideoBookingRequest(
    bookingType = BookingType.COURT,
    courtCode = courtCode,
    courtHearingType = CourtHearingType.TRIBUNAL,
    prisoners = listOf(prisoner),
    videoLinkUrl = "https://video.link.com",
    notesForStaff = notesForStaff,
  )
}

fun probationBookingRequest(
  probationTeamCode: String = "BLKPPP",
  probationMeetingType: ProbationMeetingType = ProbationMeetingType.PSR,
  prisonCode: String = "WWI",
  prisonerNumber: String = "123456",
  locationSuffix: String = "A-1-001",
  location: Location? = null,
  appointmentType: AppointmentType = AppointmentType.VLB_PROBATION,
  appointmentDate: LocalDate = tomorrow(),
  startTime: LocalTime = LocalTime.now(),
  endTime: LocalTime = LocalTime.now().plusHours(1),
  additionalBookingDetails: AdditionalBookingDetails? = null,
  notesForStaff: String? = "Some private staff notes",
  notesForPrisoners: String? = "Some public prisoners notes",
): CreateVideoBookingRequest {
  val appointment = Appointment(
    type = appointmentType,
    locationKey = location?.key ?: "$prisonCode-$locationSuffix",
    date = appointmentDate,
    startTime = startTime,
    endTime = endTime,
  )

  val prisoner = PrisonerDetails(
    prisonCode = prisonCode,
    prisonerNumber = prisonerNumber,
    appointments = listOf(appointment),
  )

  return CreateVideoBookingRequest(
    bookingType = BookingType.PROBATION,
    probationTeamCode = probationTeamCode,
    probationMeetingType = probationMeetingType,
    prisoners = listOf(prisoner),
    videoLinkUrl = null,
    additionalBookingDetails = additionalBookingDetails,
    notesForStaff = notesForStaff,
    notesForPrisoners = notesForPrisoners,
  )
}

fun requestProbationVideoLinkRequest(
  probationTeamCode: String = "BLKPPP",
  probationMeetingType: ProbationMeetingType = ProbationMeetingType.PSR,
  prisonCode: String = "WWI",
  firstName: String = "John",
  lastName: String = "Smith",
  dateOfBirth: LocalDate = LocalDate.of(1970, 1, 1),
  startTime: LocalTime = LocalTime.now(),
  endTime: LocalTime = LocalTime.now().plusHours(1),
  appointments: List<RequestedAppointment> = emptyList(),
  notesForStaff: String? = null,
  date: LocalDate? = tomorrow(),
): RequestVideoBookingRequest {
  val prisoner = UnknownPrisonerDetails(
    prisonCode = prisonCode,
    firstName = firstName,
    lastName = lastName,
    dateOfBirth = dateOfBirth,
    appointments = appointments.ifEmpty {
      listOf(
        RequestedAppointment(
          type = AppointmentType.VLB_PROBATION,
          date = date,
          startTime = startTime,
          endTime = endTime,
        ),
      )
    },
  )

  return RequestVideoBookingRequest(
    bookingType = BookingType.PROBATION,
    probationTeamCode = probationTeamCode,
    probationMeetingType = probationMeetingType,
    prisoners = listOf(prisoner),
    videoLinkUrl = "https://video.link.com",
    additionalBookingDetails = AdditionalBookingDetails(
      contactName = "probation officer name",
      contactEmail = "probation.officer@email.address",
      contactNumber = "123456",
    ),
    notesForStaff = notesForStaff,
  )
}

fun amendCourtBookingRequest(
  prisonCode: String = "WWI",
  prisonerNumber: String = "123456",
  locationSuffix: String = "A-1-001",
  location: Location? = null,
  startTime: LocalTime = LocalTime.now(),
  endTime: LocalTime = LocalTime.now().plusHours(1),
  appointments: List<Appointment> = emptyList(),
  appointmentDate: LocalDate = tomorrow(),
  notesForStaff: String? = null,
  notesForPrisoners: String? = null,
): AmendVideoBookingRequest {
  val prisoner = PrisonerDetails(
    prisonCode = prisonCode,
    prisonerNumber = prisonerNumber,
    appointments = appointments.ifEmpty {
      listOf(
        Appointment(
          type = AppointmentType.VLB_COURT_MAIN,
          locationKey = location?.key ?: "$prisonCode-$locationSuffix",
          date = appointmentDate,
          startTime = startTime,
          endTime = endTime,
        ),
      )
    },
  )

  return AmendVideoBookingRequest(
    bookingType = BookingType.COURT,
    courtHearingType = CourtHearingType.TRIBUNAL,
    prisoners = listOf(prisoner),
    videoLinkUrl = "https://video.link.com",
    notesForStaff = notesForStaff,
    notesForPrisoners = notesForPrisoners,
  )
}

fun amendProbationBookingRequest(
  probationMeetingType: ProbationMeetingType = ProbationMeetingType.PSR,
  prisonCode: String = WANDSWORTH,
  prisonerNumber: String = "123456",
  locationSuffix: String = "A-1-001",
  location: Location? = null,
  appointmentType: AppointmentType = AppointmentType.VLB_PROBATION,
  appointmentDate: LocalDate = tomorrow(),
  startTime: LocalTime = LocalTime.now(),
  endTime: LocalTime = LocalTime.now().plusHours(1),
  additionalBookingDetails: AdditionalBookingDetails? = null,
  notesForStaff: String? = "Some private staff notes",
  notesForPrisoners: String? = "Some public prisoners notes",
): AmendVideoBookingRequest {
  val appointment = Appointment(
    type = appointmentType,
    locationKey = location?.key ?: "$prisonCode-$locationSuffix",
    date = appointmentDate,
    startTime = startTime,
    endTime = endTime,
  )

  val prisoner = PrisonerDetails(
    prisonCode = prisonCode,
    prisonerNumber = prisonerNumber,
    appointments = listOf(appointment),
  )

  return AmendVideoBookingRequest(
    bookingType = BookingType.PROBATION,
    probationMeetingType = probationMeetingType,
    prisoners = listOf(prisoner),
    videoLinkUrl = null,
    additionalBookingDetails = additionalBookingDetails,
    notesForStaff = notesForStaff,
    notesForPrisoners = notesForPrisoners,
  )
}

fun VideoLinkBooking.hasBookingType(that: BookingType) = also { it.bookingType isEqualTo that }
fun VideoLinkBooking.hasCourt(that: String?) = also { it.courtCode isEqualTo that }
fun VideoLinkBooking.hasCourtHearingType(that: CourtHearingType?) = also { it.courtHearingType isEqualTo that }
fun VideoLinkBooking.hasCourtDescription(that: String?) = also { it.courtDescription isEqualTo that }
fun VideoLinkBooking.hasCourtHearingTypeDescription(that: String?) = also { it.courtHearingTypeDescription isEqualTo that }
fun VideoLinkBooking.hasProbationTeam(that: String?) = also { it.probationTeamCode isEqualTo that }
fun VideoLinkBooking.hasProbationTeamDescription(that: String?) = also { it.probationTeamDescription isEqualTo that }
fun VideoLinkBooking.hasMeetingType(that: ProbationMeetingType?) = also { it.probationMeetingType isEqualTo that }
fun VideoLinkBooking.hasMeetingTypeDescription(that: String?) = also { it.probationMeetingTypeDescription isEqualTo that }
fun VideoLinkBooking.hasVideoUrl(that: String) = also { it.videoLinkUrl isEqualTo that }
fun VideoLinkBooking.hasCreatedBy(that: User) = also { it.createdBy isEqualTo that.username }
fun VideoLinkBooking.hasCreatedTimeCloseTo(that: LocalDateTime) = also { it.createdAt isCloseTo that }
fun VideoLinkBooking.hasCreatedByPrisonerUser(that: Boolean) = also { it.createdByPrison!! isBool that }
fun VideoLinkBooking.hasNotesForStaff(that: String): VideoLinkBooking = also { it.notesForStaff isEqualTo that }
fun VideoLinkBooking.hasNotesForPrisoner(that: String?): VideoLinkBooking = also { it.notesForPrisoners isEqualTo that }
