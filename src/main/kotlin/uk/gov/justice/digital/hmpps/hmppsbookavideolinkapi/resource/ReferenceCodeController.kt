package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ReferenceCodeService

@Tag(name = "Reference Codes Controller")
@RestController
@RequestMapping(value = ["reference-codes"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class ReferenceCodeController(private val referenceCodeService: ReferenceCodeService) {

  @Operation(summary = "Endpoint to return reference data for a provided group key")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "List of reference data codes/values",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ReferenceCode::class)),
          ),
        ],
      ),
    ],
  )
  @GetMapping(value = ["/group/{groupCode}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN', 'BVLS_ACCESS__RW')")
  fun getReferenceDataByGroup(
    @Parameter(description = "EnabledOnly true or false. Defaults to false if not supplied.")
    @PathVariable("groupCode", required = true) groupCode: String,
    @Parameter(description = "Enabled only, true or false. When true only enabled values will be returned. Defaults to true if not supplied.")
    @RequestParam(name = "enabledOnly", required = false)
    enabledOnly: Boolean = true,
  ): List<ReferenceCode> = referenceCodeService.getReferenceDataByGroup(groupCode, enabledOnly)
}
