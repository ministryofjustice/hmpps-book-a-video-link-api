package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.availability

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.SupportedAppointmentTypes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.appointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvilleLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvillePrisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import java.time.LocalDate
import java.time.LocalTime

class ReplacementExternalAppointmentsServiceTest {
  private val appointmentsClient: ActivitiesAppointmentsClient = mock()
  private val appointmentTypes: SupportedAppointmentTypes = mock()
  private val service = ReplacementExternalAppointmentsService(appointmentsClient, appointmentTypes)
  private val scheduledAppointment1 = appointmentSearchResult(
    pentonvillePrisoner,
    pentonvilleLocation,
    today(),
    LocalTime.of(10, 0),
    LocalTime.of(11, 0),
    appointmentType = "THIS",
  )
  private val scheduledAppointment2 = appointmentSearchResult(
    pentonvillePrisoner,
    pentonvilleLocation,
    today(),
    LocalTime.of(14, 0),
    LocalTime.of(15, 0),
    appointmentType = "THAT",
  )
  private val scheduledAppointmentNoEndTime = appointmentSearchResult(
    pentonvillePrisoner,
    pentonvilleLocation,
    today(),
    LocalTime.of(14, 0),
    appointmentType = "THAT",
  )
  private val cancelledAppointment = appointmentSearchResult(
    pentonvillePrisoner,
    pentonvilleLocation,
    today(),
    LocalTime.of(14, 0),
    LocalTime.of(15, 0),
    isCancelled = true,
  )
  private val deletedAppointment = appointmentSearchResult(
    pentonvillePrisoner,
    pentonvilleLocation,
    today(),
    LocalTime.of(14, 0),
    LocalTime.of(15, 0),
    isDeleted = true,
  )

  @Test
  fun `should get one external appointment slot`() {
    whenever { appointmentsClient.getScheduledPrisonAppointments(PENTONVILLE, today(), pentonvilleLocation.id) } doReturn listOf(scheduledAppointment1)

    val slots = service.getAppointmentSlots(PENTONVILLE, today(), pentonvilleLocation.id)

    slots.single()
      .hasPrisoner(pentonvillePrisoner)
      .hasLocation(pentonvilleLocation)
      .hasAppointmentDate(today())
      .hasStartTime(LocalTime.of(10, 0))
      .hasEndTime(LocalTime.of(11, 0))
  }

  @Test
  fun `should get one external appointment slot when other is unsupported`() {
    whenever { appointmentsClient.getScheduledPrisonAppointments(PENTONVILLE, today(), pentonvilleLocation.id) } doReturn listOf(scheduledAppointment1, scheduledAppointment2)
    whenever { appointmentTypes.isSupported("THAT") } doReturn true

    val slots = service.getAppointmentSlots(PENTONVILLE, today(), pentonvilleLocation.id)

    slots.single()
      .hasPrisoner(pentonvillePrisoner)
      .hasLocation(pentonvilleLocation)
      .hasAppointmentDate(today())
      .hasStartTime(LocalTime.of(10, 0))
      .hasEndTime(LocalTime.of(11, 0))
  }

  @Test
  fun `should get two appointment slots`() {
    whenever { appointmentsClient.getScheduledPrisonAppointments(PENTONVILLE, today(), pentonvilleLocation.id) } doReturn listOf(scheduledAppointment1, scheduledAppointment2)

    val slots = service.getAppointmentSlots(PENTONVILLE, today(), pentonvilleLocation.id)

    slots hasSize 2

    slots.first()
      .hasPrisoner(pentonvillePrisoner)
      .hasLocation(pentonvilleLocation)
      .hasAppointmentDate(today())
      .hasStartTime(LocalTime.of(10, 0))
      .hasEndTime(LocalTime.of(11, 0))

    slots.last()
      .hasPrisoner(pentonvillePrisoner)
      .hasLocation(pentonvilleLocation)
      .hasAppointmentDate(today())
      .hasStartTime(LocalTime.of(14, 0))
      .hasEndTime(LocalTime.of(15, 0))
  }

  @Test
  fun `should ignore cancelled appointment slot`() {
    whenever { appointmentsClient.getScheduledPrisonAppointments(PENTONVILLE, today(), pentonvilleLocation.id) } doReturn listOf(scheduledAppointment1, cancelledAppointment)

    val slots = service.getAppointmentSlots(PENTONVILLE, today(), pentonvilleLocation.id)

    slots.single()
      .hasPrisoner(pentonvillePrisoner)
      .hasLocation(pentonvilleLocation)
      .hasAppointmentDate(today())
      .hasStartTime(LocalTime.of(10, 0))
      .hasEndTime(LocalTime.of(11, 0))
  }

  @Test
  fun `should ignore deleted appointment slot`() {
    whenever { appointmentsClient.getScheduledPrisonAppointments(PENTONVILLE, today(), pentonvilleLocation.id) } doReturn listOf(scheduledAppointment1, deletedAppointment)

    val slots = service.getAppointmentSlots(PENTONVILLE, today(), pentonvilleLocation.id)

    slots.single()
      .hasPrisoner(pentonvillePrisoner)
      .hasLocation(pentonvilleLocation)
      .hasAppointmentDate(today())
      .hasStartTime(LocalTime.of(10, 0))
      .hasEndTime(LocalTime.of(11, 0))
  }

  @Test
  fun `should ignore appointment slot without an end time`() {
    whenever { appointmentsClient.getScheduledPrisonAppointments(PENTONVILLE, today(), pentonvilleLocation.id) } doReturn listOf(scheduledAppointment1, scheduledAppointmentNoEndTime)

    val slots = service.getAppointmentSlots(PENTONVILLE, today(), pentonvilleLocation.id)

    slots.single()
      .hasPrisoner(pentonvillePrisoner)
      .hasLocation(pentonvilleLocation)
      .hasAppointmentDate(today())
      .hasStartTime(LocalTime.of(10, 0))
      .hasEndTime(LocalTime.of(11, 0))
  }

  private fun ReplacementExternalAppointmentsService.ExternalAppointmentSlot.hasLocation(location: Location) = also { prisonLocationId isEqualTo location.id }
  private fun ReplacementExternalAppointmentsService.ExternalAppointmentSlot.hasPrisoner(prisoner: Prisoner) = also { prisonerNumber isEqualTo prisoner.number }
  private fun ReplacementExternalAppointmentsService.ExternalAppointmentSlot.hasAppointmentDate(date: LocalDate) = also { appointmentDate isEqualTo date }
  private fun ReplacementExternalAppointmentsService.ExternalAppointmentSlot.hasStartTime(startTime: LocalTime) = also { startTime isEqualTo startTime }
  private fun ReplacementExternalAppointmentsService.ExternalAppointmentSlot.hasEndTime(endTime: LocalTime) = also { endTime isEqualTo endTime }
}
