package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.BookingStatus
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Schema(
  description =
  """
  Search attributes to search for a unique video link booking.
  
  The prisoner number, location key, date, start and end time combined should provide sufficient information to find a
  unique video link booking.
  """,
)
data class VideoBookingSearchRequest(
  @field:NotBlank(message = "The prisoner number is mandatory")
  @Schema(description = "The prisoner number (NOMIS ID)", example = "A1234AA")
  val prisonerNumber: String,

  // TODO this is going to replace the location key
  @Schema(
    description = "The unique UUID for the location where the appointment takes place. The id field from the locations-inside-prison service.",
    example = "a4fe3fef-34fd-4354fde-a12efe",
  )
  val dpsLocationId: UUID?,

  @Deprecated(message = "Use dpsLocationId instead")
  @Schema(description = "The location key for the appointment", example = "PVI-A-1-001")
  val locationKey: String?,

  @Schema(description = "The date for which the appointment starts", example = "2022-12-23")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  val date: LocalDate,

  @Schema(description = "Start time for the appointment on the day", example = "10:45")
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(description = "End time for the appointment on the day", example = "11:45")
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime,

  @Schema(description = "The status of the booking to match, defaults to ACTIVE", example = "ACTIVE")
  val statusCode: BookingStatus? = BookingStatus.ACTIVE,
) {
  @JsonIgnore
  @AssertTrue(message = "You must provide either a location id or a location key for search")
  private fun isInvalidLocation() = locationKey != null || dpsLocationId != null
}
