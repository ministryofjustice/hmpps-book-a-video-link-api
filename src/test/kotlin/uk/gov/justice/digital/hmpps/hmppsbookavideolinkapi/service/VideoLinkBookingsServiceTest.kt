package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import java.time.LocalDateTime
import java.util.Optional

class VideoLinkBookingsServiceTest {
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val referenceCodeRepository: ReferenceCodeRepository = mock()

  private val service = VideoLinkBookingsService(
    videoBookingRepository,
    prisonAppointmentRepository,
    referenceCodeRepository,
  )

  @Test
  fun `get a court video link booking by ID`() {
    val courtHearingGroup = "COURT_HEARING_TYPE"
    val courtHearingCode = "TRIBUNAL"
    val createdBy = "TIM"
    val createdTime = LocalDateTime.now()
    val prisonerNumber = "A1234FF"

    // Mock response for findById
    val courtBooking = courtBooking(createdBy = "test_user")

    // Mock response for reference code lookup
    val courtHearingType = ReferenceCode(
      referenceCodeId = 1L,
      groupCode = courtHearingGroup,
      code = courtHearingCode,
      description = "Tribunal",
      createdBy = createdBy,
      createdTime = createdTime,
    )

    // Mock response for prison appointments
    val courtAppointments = listOf(
      appointment(
        booking = courtBooking,
        prisonCode = MOORLAND,
        prisonerNumber = prisonerNumber,
        appointmentType = AppointmentType.VLB_COURT_PRE.name,
        locationKey = moorlandLocation.key,
      ),
      appointment(
        booking = courtBooking,
        prisonCode = MOORLAND,
        prisonerNumber = prisonerNumber,
        appointmentType = AppointmentType.VLB_COURT_MAIN.name,
        locationKey = moorlandLocation.key,
      ),
      appointment(
        booking = courtBooking,
        prisonCode = MOORLAND,
        prisonerNumber = prisonerNumber,
        appointmentType = AppointmentType.VLB_COURT_POST.name,
        locationKey = moorlandLocation.key,
      ),
    )

    whenever(videoBookingRepository.findById(1L)).thenReturn(Optional.of(courtBooking))
    whenever(prisonAppointmentRepository.findByVideoBooking(courtBooking)).thenReturn(courtAppointments)
    whenever(referenceCodeRepository.findByGroupCodeAndCode(courtHearingGroup, courtHearingCode)).thenReturn(courtHearingType)

    val response = service.getVideoLinkBookingById(1L)

    assertThat(response).isNotNull()
    assertThat(response.prisonAppointments).hasSize(3)
    assertThat(response.prisonAppointments.first().appointmentType).isIn(
      AppointmentType.VLB_COURT_PRE.name,
      AppointmentType.VLB_COURT_MAIN.name,
      AppointmentType.VLB_COURT_POST.name,
    )

    // Should be present for a court booking
    assertThat(response.courtCode).isEqualTo(DERBY_JUSTICE_CENTRE)
    assertThat(response.courtDescription).isEqualTo(DERBY_JUSTICE_CENTRE)
    assertThat(response.courtHearingType).isEqualTo(CourtHearingType.TRIBUNAL)
    assertThat(response.courtHearingTypeDescription).isEqualTo("Tribunal")
    assertThat(response.comments).isEqualTo("Court hearing comments")
    assertThat(response.videoLinkUrl).isEqualTo("https://court.hearing.link")

    // Should be null for a court booking
    assertThat(response.probationTeamCode).isNull()
    assertThat(response.probationTeamDescription).isNull()
    assertThat(response.probationMeetingType).isNull()
    assertThat(response.probationMeetingTypeDescription).isNull()
  }

  @Test
  fun `get a probation video booking by ID`() {
    val probationMeetingGroup = "PROBATION_MEETING_TYPE"
    val probationMeetingCode = "PSR"
    val createdBy = "TIM"
    val createdTime = LocalDateTime.now()
    val prisonerNumber = "A1234FF"

    // Mock response for findById
    val probationBooking = probationBooking()

    // Mock response for reference code lookup
    val probationMeetingType = ReferenceCode(
      referenceCodeId = 1L,
      groupCode = probationMeetingGroup,
      code = probationMeetingCode,
      description = "Pre-sentence report",
      createdBy = createdBy,
      createdTime = createdTime,
    )

    // Mock response for prison appointment
    val probationAppointments = listOf(
      appointment(
        booking = probationBooking,
        prisonCode = MOORLAND,
        prisonerNumber = prisonerNumber,
        appointmentType = AppointmentType.VLB_PROBATION.name,
        locationKey = moorlandLocation.key,
      ),
    )

    whenever(videoBookingRepository.findById(1L)).thenReturn(Optional.of(probationBooking))
    whenever(prisonAppointmentRepository.findByVideoBooking(probationBooking)).thenReturn(probationAppointments)
    whenever(referenceCodeRepository.findByGroupCodeAndCode(probationMeetingGroup, probationMeetingCode)).thenReturn(probationMeetingType)

    val response = service.getVideoLinkBookingById(1L)

    assertThat(response).isNotNull()
    assertThat(response.prisonAppointments).hasSize(1)
    assertThat(response.prisonAppointments.first().appointmentType).isEqualTo(AppointmentType.VLB_PROBATION.name)

    // Should be present for a probation booking
    assertThat(response.probationTeamCode).isEqualTo("BLKPPP")
    assertThat(response.probationTeamDescription).isEqualTo("probation team description")
    assertThat(response.probationMeetingType).isEqualTo(ProbationMeetingType.PSR)
    assertThat(response.probationMeetingTypeDescription).isEqualTo("Pre-sentence report")
    assertThat(response.videoLinkUrl).isEqualTo("https://probation.meeting.link")
    assertThat(response.comments).isEqualTo("Probation meeting comments")

    // Should be null for a probation booking
    assertThat(response.courtCode).isNull()
    assertThat(response.courtDescription).isNull()
    assertThat(response.courtHearingType).isNull()
    assertThat(response.courtHearingTypeDescription).isNull()
  }

  @Test
  fun `video booking by ID - not found`() {
    whenever(videoBookingRepository.findById(1L)).thenReturn(Optional.empty())
    assertThrows<EntityNotFoundException> {
      service.getVideoLinkBookingById(1L)
    }
  }
}
