package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import java.time.LocalDate

interface PrisonAppointmentRepository : JpaRepository<PrisonAppointment, Long> {
  fun findByVideoBooking(booking: VideoBooking): List<PrisonAppointment>

  fun findByPrisonCodeAndPrisonLocKeyAndAppointmentDate(prisonCode: String, key: String, date: LocalDate): List<PrisonAppointment>

  fun deletePrisonAppointmentsByVideoBooking(booking: VideoBooking)
}
