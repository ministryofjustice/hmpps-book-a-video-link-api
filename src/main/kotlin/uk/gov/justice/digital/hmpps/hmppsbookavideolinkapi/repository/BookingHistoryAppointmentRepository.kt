package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment

interface BookingHistoryAppointmentRepository : JpaRepository<BookingHistoryAppointment, Long> {

  fun countByPrisonerNumber(prisonerNumber: String): Long

  @Query(value = "UPDATE BookingHistoryAppointment bha SET bha.prisonerNumber = :newNumber WHERE bha.prisonerNumber = :oldNumber")
  @Modifying
  fun mergeOldPrisonerNumberToNew(oldNumber: String, newNumber: String)
}
