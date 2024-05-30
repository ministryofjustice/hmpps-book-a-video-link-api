package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.EmailAddressDto
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.UserDetailsDto
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.Appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner as ModelPrisoner

val birminghamLocation = location(prisonCode = BIRMINGHAM, locationKeySuffix = "ABCEDFG")
val inactiveBirminghamLocation = location(prisonCode = BIRMINGHAM, locationKeySuffix = "HIJLKLM", active = false)
val moorlandLocation = location(prisonCode = MOORLAND, locationKeySuffix = "ABCEDFG")
val werringtonLocation = location(prisonCode = WERRINGTON, locationKeySuffix = "ABCDEFG")

fun location(prisonCode: String, locationKeySuffix: String, active: Boolean = true) = Location(
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
)

fun prisonerSearchPrisoner(
  prisonerNumber: String,
  prisonCode: String,
  firstName: String = "Fred",
  lastName: String = "Bloggs",
) = Prisoner(
  prisonerNumber = prisonerNumber,
  prisonId = prisonCode,
  firstName = firstName,
  lastName = lastName,
)

fun userEmail(username: String, email: String) = EmailAddressDto(username, email, true)

fun userDetails(username: String, name: String) = UserDetailsDto(
  username = username,
  active = true,
  name = name,
  authSource = "TEST",
  activeCaseLoadId = null,
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
)

fun courtBookingRequest(
  courtId: Long = 1,
  prisonCode: String = "MDI",
  prisonerNumber: String = "123456",
  locationSuffix: String = "A-1-001",
  location: Location? = null,
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
          date = tomorrow(),
          startTime = startTime,
          endTime = endTime,
        ),
      )
    },
  )

  return CreateVideoBookingRequest(
    bookingType = BookingType.COURT,
    courtId = courtId,
    courtHearingType = CourtHearingType.TRIBUNAL,
    prisoners = listOf(prisoner),
    comments = comments,
    videoLinkUrl = "https://video.link.com",
  )
}

fun probationBookingRequest(
  probationTeamId: Long = 1,
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
    probationTeamId = probationTeamId,
    probationMeetingType = probationMeetingType,
    prisoners = listOf(prisoner),
    comments = comments,
    videoLinkUrl = videoLinkUrl,
  )
}

fun bookingContact(contactType: ContactType, email: String?, name: String? = null) = BookingContact(
  videoBookingId = 0,
  contactType = contactType,
  name = name,
  position = null,
  email = email,
  telephone = null,
)
