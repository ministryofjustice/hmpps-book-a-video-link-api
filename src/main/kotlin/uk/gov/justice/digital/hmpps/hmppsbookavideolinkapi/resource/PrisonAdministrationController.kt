package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.resource

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendPrisonRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonsService

@Tag(name = "Prison Administration Controller")
@RestController
@RequestMapping(value = ["prison-admin"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class PrisonAdministrationController(private val prisonService: PrisonsService) {

  @PutMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], path = ["/{prisonCode}"])
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun amendPrison(
    @Parameter(description = "The code of the prison to be amended.")
    @PathVariable(name = "prisonCode", required = true)
    prisonCode: String,
    @Valid
    @RequestBody
    @Parameter(description = "The request with the prison amendment details", required = true)
    request: AmendPrisonRequest,
  ) = prisonService.amend(prisonCode, request)
}
