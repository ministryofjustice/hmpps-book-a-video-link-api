package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ScheduleItem
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.locationAttributes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvilleLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.BookingStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.time.LocalDate
import java.time.LocalTime

class ScheduleServiceTest {
  private val scheduleRepository: ScheduleRepository = mock()
  private val locationsService: LocationsService = mock()

  private val service = ScheduleService(scheduleRepository, locationsService)

  private val courtCode = "COURT"
  private val probationTeamCode = "PROBATION"

  private fun courtItem(
    vlbId: Long,
    appId: Long,
    status: BookingStatus = BookingStatus.ACTIVE,
    date: LocalDate = today(),
    start: LocalTime,
    end: LocalTime,
    appointmentType: AppointmentType = AppointmentType.VLB_COURT_MAIN,
  ) = ScheduleItem(
    videoBookingId = vlbId,
    prisonAppointmentId = appId,
    bookingType = BookingType.COURT.name,
    statusCode = status.name,
    createdByPrison = false,
    courtId = 1L,
    courtCode = "COURT",
    courtDescription = "Derby Justice Centre",
    hearingType = CourtHearingType.TRIBUNAL.name,
    hearingTypeDescription = "Tribunal",
    probationTeamId = null,
    probationTeamCode = null,
    probationTeamDescription = null,
    probationMeetingType = null,
    probationMeetingTypeDescription = null,
    prisonCode = PENTONVILLE,
    prisonName = "Werrington",
    prisonerNumber = "A1234AA",
    appointmentType = appointmentType.name,
    appointmentTypeDescription = "Court main hearing",
    appointmentComments = "appointment comments",
    prisonLocationId = pentonvilleLocation.id,
    bookingComments = "booking comments",
    videoUrl = "http://video.url",
    appointmentDate = date,
    startTime = start,
    endTime = end,
    createdTime = yesterday().atStartOfDay(),
    createdBy = "CREATOR",
    updatedTime = today().atStartOfDay(),
    updatedBy = "AMENDER",
    probationOfficerName = null,
    probationOfficerEmailAddress = null,
    notesForPrisoners = "notes for prisoners",
    notesForStaff = "notes for staff",
  )

  private fun probationItem(
    vlbId: Long,
    appId: Long,
    status: BookingStatus = BookingStatus.ACTIVE,
    date: LocalDate = today(),
    start: LocalTime,
    end: LocalTime,
  ) = ScheduleItem(
    videoBookingId = vlbId,
    prisonAppointmentId = appId,
    bookingType = BookingType.PROBATION.name,
    statusCode = status.name,
    createdByPrison = false,
    courtId = null,
    courtCode = null,
    courtDescription = null,
    hearingType = null,
    hearingTypeDescription = null,
    probationTeamId = 1L,
    probationTeamCode = "PROBATION",
    probationTeamDescription = "Probation",
    probationMeetingType = ProbationMeetingType.PSR.name,
    probationMeetingTypeDescription = "Pre-sentence report",
    prisonCode = PENTONVILLE,
    prisonName = "Werrington",
    prisonerNumber = "A1234AA",
    appointmentType = AppointmentType.VLB_PROBATION.name,
    appointmentTypeDescription = null,
    appointmentComments = "appointment comments",
    prisonLocationId = pentonvilleLocation.id,
    bookingComments = "booking comments",
    videoUrl = "http://video.url",
    appointmentDate = date,
    startTime = start,
    endTime = end,
    createdTime = yesterday().atStartOfDay(),
    createdBy = "CREATOR",
    updatedTime = today().atStartOfDay(),
    updatedBy = "AMENDER",
    probationOfficerName = "Jane Doe",
    probationOfficerEmailAddress = "jane@doe.com",
    notesForPrisoners = "notes for prisoners",
    notesForStaff = "notes for staff",
  )

  private val eight = LocalTime.of(8, 0)
  private val nine = LocalTime.of(9, 0)
  private val ten = LocalTime.of(10, 0)
  private val eleven = LocalTime.of(11, 0)
  private val twelve = LocalTime.of(12, 0)

  private val courtItems = listOf(
    courtItem(vlbId = 1L, appId = 1L, start = eight, end = nine, appointmentType = AppointmentType.VLB_COURT_PRE),
    courtItem(vlbId = 1L, appId = 2L, start = nine, end = ten),
    courtItem(vlbId = 1L, appId = 3L, start = ten, end = eleven, appointmentType = AppointmentType.VLB_COURT_POST),
    courtItem(vlbId = 2L, appId = 4L, start = ten, end = eleven),
    courtItem(vlbId = 3L, appId = 5L, start = eleven, end = twelve),
    courtItem(vlbId = 4L, appId = 6L, start = eleven, end = twelve, status = BookingStatus.CANCELLED),
  )

  private val probationItems = listOf(
    probationItem(vlbId = 1L, appId = 1L, start = nine, end = ten),
    probationItem(vlbId = 2L, appId = 2L, start = ten, end = eleven),
    probationItem(vlbId = 3L, appId = 3L, start = eleven, end = twelve),
    probationItem(vlbId = 4L, appId = 4L, start = eleven, end = twelve, status = BookingStatus.CANCELLED),
  )

  private val prisonItems = listOf(
    courtItem(vlbId = 1L, appId = 1L, start = nine, end = ten),
    courtItem(vlbId = 2L, appId = 2L, start = ten, end = eleven),
    probationItem(vlbId = 3L, appId = 3L, start = eleven, end = twelve),
    probationItem(vlbId = 4L, appId = 4L, start = eleven, end = twelve, status = BookingStatus.CANCELLED),
  )

  @BeforeEach
  fun setUpMocks() {
    whenever(scheduleRepository.getScheduleForCourt(courtCode, LocalDate.now()))
      .thenReturn(courtItems.filter { it.statusCode == BookingStatus.ACTIVE.name })

    whenever(scheduleRepository.getScheduleForProbationTeam(probationTeamCode, LocalDate.now()))
      .thenReturn(probationItems.filter { it.statusCode == BookingStatus.ACTIVE.name })

    whenever(scheduleRepository.getScheduleForPrison(PENTONVILLE, LocalDate.now()))
      .thenReturn(prisonItems.filter { it.statusCode == BookingStatus.ACTIVE.name })

    whenever(scheduleRepository.getScheduleForCourtIncludingCancelled(courtCode, LocalDate.now()))
      .thenReturn(courtItems)

    whenever(scheduleRepository.getScheduleForProbationTeamIncludingCancelled(probationTeamCode, LocalDate.now()))
      .thenReturn(probationItems)

    whenever(scheduleRepository.getScheduleForPrisonIncludingCancelled(PENTONVILLE, LocalDate.now()))
      .thenReturn(prisonItems)

    whenever(locationsService.getLocationById(any())) doReturn pentonvilleLocation.toModel(locationAttributes())
  }

  @Test
  fun `Returns the list of scheduled court items`() {
    val response = service.getScheduleForCourt(courtCode, LocalDate.now(), false)
    assertThat(response).isNotNull
    assertThat(response).hasSize(5)
    response.map { item ->
      assertThat(item.courtCode).isEqualTo(courtCode)
      assertThat(item.courtDescription).isEqualTo("Derby Justice Centre")
      assertThat(item.hearingType).isEqualTo(CourtHearingType.TRIBUNAL)
      assertThat(item.probationTeamCode).isNull()
      assertThat(item.dpsLocationId).isEqualTo(pentonvilleLocation.id)
      assertThat(item.prisonLocDesc).isEqualTo(pentonvilleLocation.localName)

      // if appointment type is main then should be the CVP link, otherwise it should be the room link.
      when (item.appointmentType) {
        AppointmentType.VLB_COURT_PRE -> assertThat(item.videoUrl).isEqualTo("decorated-video-link-url")
        AppointmentType.VLB_COURT_MAIN -> assertThat(item.videoUrl).isEqualTo("http://video.url")
        AppointmentType.VLB_COURT_POST -> assertThat(item.videoUrl).isEqualTo("decorated-video-link-url")
        else -> fail { "Unexpected appointment type for court item ${item.appointmentType}" }
      }

      assertThat(item.createdTime).isEqualTo(yesterday().atStartOfDay())
      assertThat(item.createdBy).isEqualTo("CREATOR")
      assertThat(item.updatedTime).isEqualTo(today().atStartOfDay())
      assertThat(item.updatedBy).isEqualTo("AMENDER")
      assertThat(item.bookingComments).isEqualTo("booking comments")
      assertThat(item.appointmentComments).isEqualTo("appointment comments")
      assertThat(item.notesForStaff).isEqualTo("notes for staff")
      assertThat(item.notesForPrisoners).isEqualTo("notes for prisoners")
    }
  }

  @Test
  fun `Returns the list of probation team items`() {
    val response = service.getScheduleForProbationTeam(probationTeamCode, LocalDate.now(), false)
    assertThat(response).isNotNull
    assertThat(response).hasSize(3)
    response.map { item ->
      assertThat(item.probationTeamCode).isEqualTo(probationTeamCode)
      assertThat(item.probationTeamDescription).isEqualTo("Probation")
      assertThat(item.probationMeetingType).isEqualTo(ProbationMeetingType.PSR)
      assertThat(item.courtCode).isNull()
      assertThat(item.dpsLocationId).isEqualTo(pentonvilleLocation.id)
      assertThat(item.prisonLocDesc).isEqualTo(pentonvilleLocation.localName)
      assertThat(item.videoUrl).isEqualTo("decorated-video-link-url")
      assertThat(item.createdTime).isEqualTo(yesterday().atStartOfDay())
      assertThat(item.createdBy).isEqualTo("CREATOR")
      assertThat(item.updatedTime).isEqualTo(today().atStartOfDay())
      assertThat(item.updatedBy).isEqualTo("AMENDER")
      assertThat(item.probationOfficerName).isEqualTo("Jane Doe")
      assertThat(item.probationOfficerEmailAddress).isEqualTo("jane@doe.com")
      assertThat(item.appointmentComments).isEqualTo("appointment comments")
      assertThat(item.notesForStaff).isEqualTo("notes for staff")
      assertThat(item.notesForPrisoners).isEqualTo("notes for prisoners")
    }
  }

  @Test
  fun `Returns the list of prison items`() {
    val response = service.getScheduleForPrison(PENTONVILLE, LocalDate.now(), false)
    assertThat(response).isNotNull
    assertThat(response).hasSize(3)
    assertThat(response).extracting("courtCode").containsOnly(courtCode, null)
    assertThat(response).extracting("probationTeamCode").containsOnly(probationTeamCode, null)
    assertThat(response).extracting("dpsLocationId").containsOnly(pentonvilleLocation.id)
    assertThat(response).extracting("prisonLocDesc").containsOnly(pentonvilleLocation.localName)
    assertThat(response).extracting("notesForStaff").containsOnly("notes for staff")
    assertThat(response).extracting("notesForPrisoners").containsOnly("notes for prisoners")
  }

  @Test
  fun `Returns the list of scheduled court items including cancelled`() {
    val response = service.getScheduleForCourt(courtCode, LocalDate.now(), true)
    assertThat(response).isNotNull
    assertThat(response).hasSize(6)
  }

  @Test
  fun `Returns the list of probation team items including cancelled`() {
    val response = service.getScheduleForProbationTeam(probationTeamCode, LocalDate.now(), true)
    assertThat(response).isNotNull
    assertThat(response).hasSize(4)
  }

  @Test
  fun `Returns the list of prison items including cancelled`() {
    val response = service.getScheduleForPrison(PENTONVILLE, LocalDate.now(), true)
    assertThat(response).isNotNull
    assertThat(response).hasSize(4)
    assertThat(response)
      .extracting("statusCode")
      .containsOnly(BookingStatus.ACTIVE, BookingStatus.CANCELLED)
    assertThat(response).extracting("courtCode").containsOnly(courtCode, null)
    assertThat(response).extracting("probationTeamCode").containsOnly(probationTeamCode, null)
  }
}
