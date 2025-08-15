package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue

data class AmendPrisonRequest(
  @Schema(
    description = """
    Represents the number of minutes to pick-up prisoners prior to bookings starting. For example, if a booking starts
     at 10am and the pick-up time is 15 minutes, the prisoner will be picked up at 9:45am the day of the booking.
     
    Must be between 1 to 60 minutes or null.
  """,
    example = "15",
  )
  val pickUpTime: Int? = null,
) {
  @JsonIgnore
  @AssertTrue(message = "The pick-up time must be between 1 to 60 minutes")
  private fun isInvalidPickUpTime() = pickUpTime == null || pickUpTime in 1..60
}
