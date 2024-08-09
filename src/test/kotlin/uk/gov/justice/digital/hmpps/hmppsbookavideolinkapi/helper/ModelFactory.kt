package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.EmailAddressDto
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.UserDetailsDto
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.UserDetailsDto.AuthSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDateTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.Appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.RequestVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.UnknownPrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner as ModelPrisoner

val birminghamLocation = location(prisonCode = BIRMINGHAM, locationKeySuffix = "ABCEDFG", localName = "Birmingham room")
val inactiveBirminghamLocation = location(prisonCode = BIRMINGHAM, locationKeySuffix = "HIJLKLM", active = false)
val moorlandLocation = location(prisonCode = MOORLAND, locationKeySuffix = "ABCEDFG", localName = "Moorland room")
val werringtonLocation = location(prisonCode = WERRINGTON, locationKeySuffix = "ABCDEFG")
val norwichLocation = location(prisonCode = NORWICH, locationKeySuffix = "ABCDEFG")

val courtUser = user(userType = UserType.EXTERNAL, name = "Court User", email = "court.user@court.com")
val prisonUser = user(userType = UserType.PRISON, name = "Prison User", email = "prison.user@prison.com")
val probationUser = user(userType = UserType.EXTERNAL, name = "Probation User", email = "probation.user@probation.com")
val serviceUser = user(userType = UserType.SERVICE, name = "service user")

fun location(prisonCode: String, locationKeySuffix: String, active: Boolean = true, localName: String? = null) = Location(
  id = UUID.randomUUID(),
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

fun userDetails(username: String, name: String = "Test User", authSource: AuthSource = AuthSource.auth) =
  UserDetailsDto(
    userId = "TEST",
    username = username,
    active = true,
    name = name,
    authSource = authSource,
  )

fun user(username: String = "user", userType: UserType = UserType.EXTERNAL, name: String = "Test User", email: String? = null) =
  User(
    username = username,
    userType = userType,
    name = name,
    email = email,
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
  prisonCode: String = "MDI",
  prisonerNumber: String = "123456",
  locationSuffix: String = "A-1-001",
  location: Location? = null,
  date: LocalDate = tomorrow(),
  startTime: LocalTime = LocalTime.now(),
  endTime: LocalTime = LocalTime.now().plusHours(1),
  comments: String = "court booking comments",
  appointments: List<Appointment> = emptyList(),
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
    comments = comments,
    videoLinkUrl = "https://video.link.com",
  )
}

fun requestCourtVideoLinkRequest(
  courtCode: String = "DRBYMC",
  prisonCode: String = "MDI",
  firstName: String = "John",
  lastName: String = "Smith",
  dateOfBirth: LocalDate = LocalDate.of(1970, 1, 1),
  locationSuffix: String = "A-1-001",
  location: Location? = null,
  startTime: LocalTime = LocalTime.now(),
  endTime: LocalTime = LocalTime.now().plusHours(1),
  comments: String = "court booking comments",
  appointments: List<Appointment> = emptyList(),
): RequestVideoBookingRequest {
  val prisoner = UnknownPrisonerDetails(
    prisonCode = prisonCode,
    firstName = firstName,
    lastName = lastName,
    dateOfBirth = dateOfBirth,
    appointments = appointments.ifEmpty {
      listOf(
        Appointment(
          type = AppointmentType.VLB_COURT_MAIN,
          locationKey = location?.key ?: "$prisonCode-$locationSuffix",
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
    comments = comments,
    videoLinkUrl = "https://video.link.com",
  )
}

fun probationBookingRequest(
  probationTeamCode: String = "BLKPPP",
  probationMeetingType: ProbationMeetingType = ProbationMeetingType.PSR,
  videoLinkUrl: String = "https://video.link.com",
  prisonCode: String = "MDI",
  prisonerNumber: String = "123456",
  locationSuffix: String = "A-1-001",
  location: Location? = null,
  appointmentType: AppointmentType = AppointmentType.VLB_PROBATION,
  appointmentDate: LocalDate = tomorrow(),
  startTime: LocalTime = LocalTime.now(),
  endTime: LocalTime = LocalTime.now().plusHours(1),
  comments: String = "probation booking comments",
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
    comments = comments,
    videoLinkUrl = videoLinkUrl,
  )
}

fun requestProbationVideoLinkRequest(
  probationTeamCode: String = "BLKPPP",
  probationMeetingType: ProbationMeetingType = ProbationMeetingType.PSR,
  prisonCode: String = "MDI",
  firstName: String = "John",
  lastName: String = "Smith",
  dateOfBirth: LocalDate = LocalDate.of(1970, 1, 1),
  locationSuffix: String = "A-1-001",
  location: Location? = null,
  startTime: LocalTime = LocalTime.now(),
  endTime: LocalTime = LocalTime.now().plusHours(1),
  comments: String = "probation booking comments",
  appointments: List<Appointment> = emptyList(),
): RequestVideoBookingRequest {
  val prisoner = UnknownPrisonerDetails(
    prisonCode = prisonCode,
    firstName = firstName,
    lastName = lastName,
    dateOfBirth = dateOfBirth,
    appointments = appointments.ifEmpty {
      listOf(
        Appointment(
          type = AppointmentType.VLB_PROBATION,
          locationKey = location?.key ?: "$prisonCode-$locationSuffix",
          date = tomorrow(),
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
    comments = comments,
    videoLinkUrl = "https://video.link.com",
  )
}

fun amendCourtBookingRequest(
  courtCode: String = "DRBYMC",
  prisonCode: String = "MDI",
  prisonerNumber: String = "123456",
  locationSuffix: String = "A-1-001",
  location: Location? = null,
  startTime: LocalTime = LocalTime.now(),
  endTime: LocalTime = LocalTime.now().plusHours(1),
  comments: String = "court booking comments",
  appointments: List<Appointment> = emptyList(),
): AmendVideoBookingRequest {
  val prisoner = PrisonerDetails(
    prisonCode = prisonCode,
    prisonerNumber = prisonerNumber,
    appointments = appointments.ifEmpty {
      listOf(
        Appointment(
          type = AppointmentType.VLB_COURT_MAIN,
          locationKey = location?.key ?: "$prisonCode-$locationSuffix",
          date = tomorrow(),
          startTime = startTime,
          endTime = endTime,
        ),
      )
    },
  )

  return AmendVideoBookingRequest(
    bookingType = BookingType.COURT,
    courtCode = courtCode,
    courtHearingType = CourtHearingType.TRIBUNAL,
    prisoners = listOf(prisoner),
    comments = comments,
    videoLinkUrl = "https://video.link.com",
  )
}

fun amendProbationBookingRequest(
  probationTeamCode: String = "BLKPPP",
  probationMeetingType: ProbationMeetingType = ProbationMeetingType.PSR,
  videoLinkUrl: String = "https://video.link.com",
  prisonCode: String = "MDI",
  prisonerNumber: String = "123456",
  locationSuffix: String = "A-1-001",
  location: Location? = null,
  appointmentType: AppointmentType = AppointmentType.VLB_PROBATION,
  appointmentDate: LocalDate = tomorrow(),
  startTime: LocalTime = LocalTime.now(),
  endTime: LocalTime = LocalTime.now().plusHours(1),
  comments: String = "probation booking comments",
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
    probationTeamCode = probationTeamCode,
    probationMeetingType = probationMeetingType,
    prisoners = listOf(prisoner),
    comments = comments,
    videoLinkUrl = videoLinkUrl,
  )
}
