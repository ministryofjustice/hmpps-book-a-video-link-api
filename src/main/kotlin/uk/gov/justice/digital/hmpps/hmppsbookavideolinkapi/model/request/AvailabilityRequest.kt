package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import kotlin.reflect.KClass

@Schema(description = "The request object sent to the availability check endpoint")
data class AvailabilityRequest(
  @field:NotNull(message = "The date is mandatory")
  @Schema(
    description = "The booking type",
    example = "COURT",
    requiredMode = RequiredMode.REQUIRED,
  )
  val bookingType: BookingType?,

  @field:NotBlank(message = "Court or probation team code is mandatory")
  @Schema(
    description = "The court code or probation team code",
    example = "DRBYMC",
    requiredMode = RequiredMode.REQUIRED,
  )
  val courtOrProbationCode: String?,

  @field:NotBlank(message = "The prison code must be present")
  @Schema(
    description = "The prison code where these appointment will take place",
    example = "MDI",
    requiredMode = RequiredMode.REQUIRED,
  )
  val prisonCode: String?,

  @field:NotNull(message = "The date is mandatory")
  @Schema(
    description = "The date for the appointments on this booking (must all be on the same day)",
    example = "2024-04-05",
    requiredMode = RequiredMode.REQUIRED,
  )
  val date: LocalDate?,

  @field:Valid
  @Schema(
    description = "If present, the prison location and start/end time of the requested pre hearing, else null",
    requiredMode = RequiredMode.NOT_REQUIRED,
  )
  val preAppointment: LocationAndInterval? = null,

  @field:Valid
  @field:NotNull(message = "The main appointment is mandatory")
  @Schema(
    description = "The main appointment which is always present",
    requiredMode = RequiredMode.REQUIRED,
  )
  val mainAppointment: LocationAndInterval?,

  @field:Valid
  @Schema(
    description = "If present, the prison location and start/end time of the post hearing, else null",
    requiredMode = RequiredMode.NOT_REQUIRED,
  )
  val postAppointment: LocationAndInterval? = null,

  @Schema(
    description = "Exclude the video link booking with this ID from the availability check. Useful when checking availability during the amending of a booking.",
    requiredMode = RequiredMode.NOT_REQUIRED,
  )
  val vlbIdToExclude: Long? = null,
)

@Schema(description = "The prison location key and start/end interval for an appointment slot")
data class LocationAndInterval(

  @field:NotBlank(message = "The prison location key must be present")
  @Schema(
    description = "The location of the appointment at the prison",
    example = "VCC-ROOM-1",
    requiredMode = RequiredMode.REQUIRED,
  )
  val prisonLocKey: String?,

  @Schema(
    description = "The start and end time of a prison appointment to define the interval",
    requiredMode = RequiredMode.NOT_REQUIRED,
  )
  @field:ValidInterval
  val interval: Interval,
) {
  fun shift(duration: Duration): LocationAndInterval = copy(interval = Interval(interval.start?.plus(duration), interval.end?.plus(duration)))
}

@Schema(description = "A time interval between a start and end time")
data class Interval(
  @field:NotNull(message = "The start time is mandatory")
  @Schema(
    description = "The interval start time, inclusive. ISO-8601 format (hh:mm)",
    example = "09:00",
    requiredMode = RequiredMode.REQUIRED,
  )
  @JsonFormat(pattern = "HH:mm")
  val start: LocalTime?,

  @field:NotNull(message = "The end time is mandatory")
  @Schema(
    description = "The interval end time (inclusive). ISO-8601 format (hh:mm)",
    example = "09:30",
    requiredMode = RequiredMode.REQUIRED,
  )
  @JsonFormat(pattern = "HH:mm")
  val end: LocalTime?,
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
  override fun isValid(interval: Interval?, context: ConstraintValidatorContext?) = when (interval) {
    null -> true
    else -> interval.start?.isBefore(interval.end) ?: false
  }
}
