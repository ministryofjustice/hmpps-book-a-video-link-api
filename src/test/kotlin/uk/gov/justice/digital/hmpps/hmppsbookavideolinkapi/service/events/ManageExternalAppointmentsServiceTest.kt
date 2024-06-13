package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional

class ManageExternalAppointmentsServiceTest {

  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val activitiesAppointmentsClient: ActivitiesAppointmentsClient = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val prisonerSearchClient: PrisonerSearchClient = mock()
  private val prisonLocation: Location = mock { on { locationId } doReturn 123456 }
  private val booking = courtBooking()
  private val appointment = appointment(
    booking = booking,
    prisonCode = BIRMINGHAM,
    prisonerNumber = "123456",
    appointmentType = "VLB_COURT_PRE",
    date = LocalDate.of(2100, 1, 1),
    startTime = LocalTime.of(11, 0),
    endTime = LocalTime.of(11, 30),
    locationKey = "ABC",
  )
  private val service =
    ManageExternalAppointmentsService(prisonAppointmentRepository, activitiesAppointmentsClient, prisonApiClient, prisonerSearchClient)

  @Test
  fun `should call create appointment on activities client when appointments rolled out`() {
    whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(appointment)
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(BIRMINGHAM)) doReturn true
    whenever(prisonApiClient.getInternalLocationByKey(appointment.prisonLocKey)) doReturn prisonLocation

    service.createAppointment(1)

    verify(prisonApiClient).getInternalLocationByKey(appointment.prisonLocKey)

    verify(activitiesAppointmentsClient).createAppointment(
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      startDate = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      internalLocationId = 123456,
    )
  }

  @Test
  fun `should call create appointment on prison api client when appointments not rolled out`() {
    whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(appointment)
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(BIRMINGHAM)) doReturn false
    whenever(prisonerSearchClient.getPrisoner(appointment.prisonerNumber)) doReturn prisonerSearchPrisoner(prisonerNumber = appointment.prisonerNumber, prisonCode = appointment.prisonCode, bookingId = 1)
    whenever(prisonApiClient.getInternalLocationByKey(appointment.prisonLocKey)) doReturn prisonLocation

    service.createAppointment(1)

    verify(activitiesAppointmentsClient, never()).createAppointment(any(), any(), any(), any(), any(), any())
    verify(prisonApiClient).createAppointment(
      bookingId = 1,
      locationId = 123456,
      appointmentDate = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      comments = "Court hearing comments",
    )
  }

  @Test
  fun `should be no-op when appointment not found`() {
    whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.empty()

    service.createAppointment(1)

    verifyNoInteractions(activitiesAppointmentsClient)
    verifyNoInteractions(prisonApiClient)
  }
}
