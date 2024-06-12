package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional

class ManageExternalAppointmentsServiceTest {

  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val activitiesAppointmentsClient: ActivitiesAppointmentsClient = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val booking = courtBooking()
  private val appointment = appointment(
    booking = booking,
    prisonCode = BIRMINGHAM,
    prisonerNumber = "123456",
    appointmentType = "VLB_COURT_PRE",
    date = LocalDate.of(2100, 1, 1),
    startTime = LocalTime.of(11, 0),
    endTime = LocalTime.of(11, 30),
    locationKey = "",
  )
  private val service =
    ManageExternalAppointmentsService(prisonAppointmentRepository, activitiesAppointmentsClient, prisonApiClient)

  @Test
  fun `should call create appointment on activities client when appointments rolled out`() {
    whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(appointment)
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(BIRMINGHAM)) doReturn true

    service.createAppointment(1)

    verify(activitiesAppointmentsClient).createAppointment()
    verifyNoInteractions(prisonApiClient)
  }

  @Test
  fun `should call create appointment on prison api client when appointments not rolled out`() {
    whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(appointment)
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(BIRMINGHAM)) doReturn false

    service.createAppointment(1)

    verify(activitiesAppointmentsClient, never()).createAppointment()
    verify(prisonApiClient).createAppointment()
  }

  @Test
  fun `should be no-op when appointment not found`() {
    whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.empty()

    service.createAppointment(1)

    verifyNoInteractions(activitiesAppointmentsClient)
    verifyNoInteractions(prisonApiClient)
  }
}
