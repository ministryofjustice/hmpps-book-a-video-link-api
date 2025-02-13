package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.EmailAddressDto
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.UserDetailsDto
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.UserDetailsDto.AuthSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDateTime
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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.UnknownPrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner as ModelPrisoner

val birminghamLocation = location(prisonCode = BIRMINGHAM, locationKeySuffix = "ABCEDFG", localName = "Birmingham room")
val inactiveBirminghamLocation = location(prisonCode = BIRMINGHAM, locationKeySuffix = "HIJLKLM", active = false)
val wandsworthLocation = location(prisonCode = WANDSWORTH, locationKeySuffix = "ABCEDFG", localName = "Wandsworth room")
val wandsworthLocation2 =
  location(prisonCode = WANDSWORTH, locationKeySuffix = "ABCEDFG2", localName = "Wandsworth room 2")
val wandsworthLocation3 =
  location(prisonCode = WANDSWORTH, locationKeySuffix = "ABCEDFG3", localName = "Wandsworth room 3")
val pentonvilleLocation = location(prisonCode = PENTONVILLE, locationKeySuffix = "ABCDEFG", localName = "Pentonville room 3")
val norwichLocation = location(prisonCode = NORWICH, locationKeySuffix = "ABCDEFG")
val risleyLocation = location(prisonCode = RISLEY, locationKeySuffix = "ABCDEFG", localName = "Risley room")

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
  comments: String = "court booking comments",
  appointments: List<Appointment> = emptyList(),
  videoLinkUrl: String? = "https://video.link.com",
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
    videoLinkUrl = videoLinkUrl,
  )
}

fun requestCourtVideoLinkRequest(
  courtCode: String = "DRBYMC",
  prisonCode: String = "WWI",
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
  prisonCode: String = "WWI",
  prisonerNumber: String = "123456",
  locationSuffix: String = "A-1-001",
  location: Location? = null,
  appointmentType: AppointmentType = AppointmentType.VLB_PROBATION,
  appointmentDate: LocalDate = tomorrow(),
  startTime: LocalTime = LocalTime.now(),
  endTime: LocalTime = LocalTime.now().plusHours(1),
  comments: String = "probation booking comments",
  additionalBookingDetails: AdditionalBookingDetails? = null,
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
    additionalBookingDetails = additionalBookingDetails,
  )
}

fun requestProbationVideoLinkRequest(
  probationTeamCode: String = "BLKPPP",
  probationMeetingType: ProbationMeetingType = ProbationMeetingType.PSR,
  prisonCode: String = "WWI",
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
  prisonCode: String = "WWI",
  prisonerNumber: String = "123456",
  locationSuffix: String = "A-1-001",
  location: Location? = null,
  startTime: LocalTime = LocalTime.now(),
  endTime: LocalTime = LocalTime.now().plusHours(1),
  comments: String = "court booking comments",
  appointments: List<Appointment> = emptyList(),
  appointmentDate: LocalDate = tomorrow(),
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
    comments = comments,
    videoLinkUrl = "https://video.link.com",
  )
}

fun amendProbationBookingRequest(
  probationMeetingType: ProbationMeetingType = ProbationMeetingType.PSR,
  videoLinkUrl: String = "https://video.link.com",
  prisonCode: String = WANDSWORTH,
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
    probationMeetingType = probationMeetingType,
    prisoners = listOf(prisoner),
    comments = comments,
    videoLinkUrl = videoLinkUrl,
  )
}
