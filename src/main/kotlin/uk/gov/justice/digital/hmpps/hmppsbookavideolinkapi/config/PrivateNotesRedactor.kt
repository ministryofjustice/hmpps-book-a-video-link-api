package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config

import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.VideoLinkBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User

/**
 * Redacts any private comments from any VideoLinkBooking DTO's returned for non-external user requests.
 */
@ControllerAdvice
class PrivateNotesRedactor : ResponseBodyAdvice<VideoLinkBooking> {

  override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>>) = returnType.isVideoLinkBooking()

  override fun beforeBodyWrite(
    booking: VideoLinkBooking?,
    returnType: MethodParameter,
    selectedContentType: MediaType,
    selectedConverterType: Class<out HttpMessageConverter<*>>,
    request: ServerHttpRequest,
    response: ServerHttpResponse,
  ) = booking?.redactPrivateNotesForNonExternalUsers(request.getUser())

  private fun MethodParameter.isVideoLinkBooking() = genericParameterType.typeName == VideoLinkBooking::class.qualifiedName

  private fun ServerHttpRequest.getUser() = (attributes[BvlsRequestContext::class.simpleName] as BvlsRequestContext).user

  private fun VideoLinkBooking.redactPrivateNotesForNonExternalUsers(user: User) = if (user is ExternalUser) this else copy(notesForStaff = null)
}
