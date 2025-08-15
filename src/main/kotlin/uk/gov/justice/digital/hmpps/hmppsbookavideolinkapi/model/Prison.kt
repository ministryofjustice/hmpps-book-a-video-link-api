package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes the details of a prison")
data class Prison(

  @Schema(description = "An internally-generated unique identifier for this prison.", example = "12345")
  val prisonId: Long,

  @Schema(description = "A short code for this prison.", example = "BMI")
  val code: String,

  @Schema(description = "A fuller description for this prison", example = "HMP Birmingham")
  val name: String,

  @Schema(
    description = "A boolean value to show whether the prison is enabled for self-service video link bookings by court/probation.",
    example = "true",
  )
  val enabled: Boolean,

  @Schema(
    description = "Notes relating to this prison, e.g. number of video-enabled rooms, address.",
    example = "Free form notes",
  )
  val notes: String?,

  @Schema(
    description = """
    Represents the number of minutes to pick-up prisoners prior to bookings starting. For example, if a booking starts
     at 10am and the pick-up time is 15 minutes, the prisoner will be picked up at 9:45am the day of the booking.
  """,
    example = "15",
  )
  val pickUpTime: Int? = null,
)
