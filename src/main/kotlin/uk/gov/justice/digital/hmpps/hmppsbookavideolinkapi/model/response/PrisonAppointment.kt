package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime

@Schema(description = "A representation of a prison appointment")
data class PrisonAppointment(

  @Schema(description = "The internal ID for this appointment", example = "123")
  val prisonAppointmentId: Long,

  @Schema(description = "The prison code where this appointment will take place", example = "MDI")
  val prisonCode: String,

  @Schema(description = "The prisoner number for the person attending this appointment", example = "AA1234A")
  val prisonerNumber: String,

  @Schema(description = "The appointment type", example = "VLB")
  val appointmentType: String,

  @Schema(description = "The comments for this appointment", example = "Please be on time")
  val comments: String?,

  @Schema(description = "The location of the appointment at the prison", example = "VCC-ROOM-1")
  val prisonLocKey: String,

  @Schema(description = "The date of the appointment", example = "2024-04-05")
  val appointmentDate: LocalDate,

  @Schema(description = "The start time for this appointment", example = "11:30")
  val startTime: LocalTime,

  @Schema(description = "The end time for this appointment", example = "12:30")
  val endTime: LocalTime,
)
