package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import jakarta.validation.constraints.NotEmpty
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import kotlin.reflect.KClass

@Schema(description = "The request object sent to the availability check endpoint")
data class AvailabilityRequest(
  @Schema(description = "The booking type", example = "COURT")
  val bookingType: BookingType,

  @field:NotEmpty(message = "Court or probation team code is mandatory")
  @Schema(description = "The court code or probation team code", example = "DRBYMC")
  val courtOrProbationCode: String,

  @field:NotEmpty(message = "The prison code must be present")
  @Schema(description = "The prison code where these appointment will take place", example = "MDI")
  val prisonCode: String,

  @Schema(description = "The date for the appointments related to this booking (must be on the same day)", example = "2024-04-05")
  val date: LocalDate,

  @Schema(description = "If present, the prison location and start/end time of the requested pre-conference, else null")
  val preAppointment: LocationAndInterval? = null,

  @Schema(description = "The date of these appointments", example = "2024-04-05", required = true)
  val mainAppointment: LocationAndInterval,

  @Schema(description = "If present, the prison location and start/end time of the post-conference, else null")
  val postAppointment: LocationAndInterval? = null,
)

@Schema(description = "The prison location key and start/end interval for an appointment slot")
data class LocationAndInterval(

  @field:NotEmpty(message = "The prison location key must be present")
  @Schema(description = "The location of the appointment at the prison", example = "VCC-ROOM-1", required = true)
  val prisonLocKey: String,

  @Schema(description = "", example = "MDI", required = false)
  @field:ValidInterval
  val interval: Interval,
) {
  fun shift(duration: Duration): LocationAndInterval =
    copy(interval = Interval(interval.start.plus(duration), interval.end.plus(duration)))
}

@Schema(description = "A time interval between a start and end time")
data class Interval(
  @Schema(description = "The interval start time, inclusive. ISO-8601 format (hh:mm)", example = "09:00")
  val start: LocalTime,

  @Schema(description = "The interval end time (inclusive). ISO-8601 format (hh:mm)", example = "09:30")
  val end: LocalTime,
)

@Target(AnnotationTarget.TYPE, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [IntervalValidator::class])
annotation class ValidInterval(
  val message: String = "start must precede end",
  val groups: Array<KClass<out Any>> = [],
  val payload: Array<KClass<out Payload>> = [],
)

class IntervalValidator : ConstraintValidator<ValidInterval, Interval> {
  override fun isValid(interval: Interval?, context: ConstraintValidatorContext?) =
    when (interval) {
      null -> true
      else -> interval.start.isBefore(interval.end)
    }
}
