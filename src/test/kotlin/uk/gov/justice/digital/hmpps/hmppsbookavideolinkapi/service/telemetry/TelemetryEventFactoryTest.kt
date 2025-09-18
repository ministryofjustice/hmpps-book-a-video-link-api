package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsEntry
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isInstanceOf
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingAction
import java.time.LocalDateTime

class TelemetryEventFactoryTest {

  @Nested
  inner class CourtEventTypes {
    private val newCourtBooking = courtBooking().withMainCourtPrisonAppointment()
    private val amendedCourtBooking = courtBooking().withMainCourtPrisonAppointment().apply {
      amendedBy = COURT_USER.username
      amendedTime = LocalDateTime.now()
    }
    private val cancelledCourtBooking = courtBooking().withMainCourtPrisonAppointment().cancel(COURT_USER)

    @Test
    fun `should be create event `() {
      val event = TelemetryEventFactory.event(BookingAction.CREATE, newCourtBooking, COURT_USER)
      event isInstanceOf CourtBookingCreatedTelemetryEvent::class.java
    }

    @Test
    fun `should be amend event `() {
      val event = TelemetryEventFactory.event(BookingAction.AMEND, amendedCourtBooking, COURT_USER)
      event isInstanceOf CourtBookingAmendedTelemetryEvent::class.java
    }

    @Test
    fun `should be cancel event `() {
      val event = TelemetryEventFactory.event(BookingAction.CANCEL, cancelledCourtBooking, COURT_USER)!!
      event isInstanceOf CourtBookingCancelledTelemetryEvent::class.java
      event.properties() containsEntry Pair("cancelled_by", "court")
    }

    @Test
    fun `should be transfer event `() {
      val event = TelemetryEventFactory.event(BookingAction.TRANSFERRED, cancelledCourtBooking, COURT_USER)!!
      event isInstanceOf CourtBookingCancelledTelemetryEvent::class.java
      event.properties() containsEntry Pair("cancelled_by", "transfer")
    }

    @Test
    fun `should be release event `() {
      val event = TelemetryEventFactory.event(BookingAction.RELEASED, cancelledCourtBooking, COURT_USER)!!
      event isInstanceOf CourtBookingCancelledTelemetryEvent::class.java
      event.properties() containsEntry Pair("cancelled_by", "release")
    }
  }

  @Nested
  inner class ProbationEventTypes {
    private val newProbationBooking = probationBooking().withProbationPrisonAppointment()
    private val amendedProbationBooking = probationBooking().withProbationPrisonAppointment().apply {
      amendedBy = PROBATION_USER.username
      amendedTime = LocalDateTime.now()
    }
    private val cancelledProbationBooking = probationBooking().withProbationPrisonAppointment().cancel(PROBATION_USER)

    @Test
    fun `should be create event `() {
      val event = TelemetryEventFactory.event(BookingAction.CREATE, newProbationBooking, PROBATION_USER)
      event isInstanceOf ProbationBookingCreatedTelemetryEvent::class.java
    }

    @Test
    fun `should be amend event `() {
      val event = TelemetryEventFactory.event(BookingAction.AMEND, amendedProbationBooking, PROBATION_USER)
      event isInstanceOf ProbationBookingAmendedTelemetryEvent::class.java
    }

    @Test
    fun `should be cancel event `() {
      val event = TelemetryEventFactory.event(BookingAction.CANCEL, cancelledProbationBooking, PROBATION_USER)!!
      event isInstanceOf ProbationBookingCancelledTelemetryEvent::class.java
      event.properties() containsEntry Pair("cancelled_by", "probation")
    }

    @Test
    fun `should be transfer event `() {
      val event = TelemetryEventFactory.event(BookingAction.TRANSFERRED, cancelledProbationBooking, PROBATION_USER)!!
      event isInstanceOf ProbationBookingCancelledTelemetryEvent::class.java
      event.properties() containsEntry Pair("cancelled_by", "transfer")
    }

    @Test
    fun `should be release event `() {
      val event = TelemetryEventFactory.event(BookingAction.RELEASED, cancelledProbationBooking, PROBATION_USER)!!
      event isInstanceOf ProbationBookingCancelledTelemetryEvent::class.java
      event.properties() containsEntry Pair("cancelled_by", "release")
    }
  }
}
