package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import software.amazon.awssdk.http.HttpStatusCode.FORBIDDEN
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.VideoBookingAccessException

@RestControllerAdvice
class HmppsBookAVideoLinkApiExceptionHandler : ResponseEntityExceptionHandler() {
  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: ValidationException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Validation exception: {}", e.message) }

  @ExceptionHandler(EntityNotFoundException::class)
  fun handleEntityNotFoundException(e: EntityNotFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = "Entity not found : ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Entity not found exception: {}", e.message) }

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(INTERNAL_SERVER_ERROR)
    .body(
      ErrorResponse(
        status = INTERNAL_SERVER_ERROR,
        userMessage = "Unexpected error: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.error("Unexpected exception", e) }

  @ExceptionHandler(IllegalArgumentException::class)
  fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
    log.info("Exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Exception: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(CaseloadAccessException::class)
  fun handleCaseLoadAccessException(e: CaseloadAccessException): ResponseEntity<ErrorResponse> {
    log.info("Case load access exception: {}", e.message)
    return ResponseEntity
      .status(FORBIDDEN)
      .body(
        ErrorResponse(
          status = FORBIDDEN,
          userMessage = "Not found: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(VideoBookingAccessException::class)
  fun handleCaseLoadAccessException(e: VideoBookingAccessException): ResponseEntity<ErrorResponse> {
    log.info("Case load access exception: {}", e.message)
    return ResponseEntity
      .status(FORBIDDEN)
      .body(
        ErrorResponse(
          status = FORBIDDEN,
          userMessage = "Not found: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  override fun handleHttpMessageNotReadable(
    ex: HttpMessageNotReadableException,
    headers: HttpHeaders,
    status: HttpStatusCode,
    request: WebRequest,
  ): ResponseEntity<Any>? {
    log.info("Exception not readable: {}", ex.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = ex.localizedMessage,
          developerMessage = ex.message,
        ),
      )
  }

  override fun handleMethodArgumentNotValid(
    ex: MethodArgumentNotValidException,
    headers: HttpHeaders,
    status: HttpStatusCode,
    request: WebRequest,
  ): ResponseEntity<Any>? {
    val errors = ex.bindingResult.allErrors.map { it.defaultMessage }

    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = errors.joinToString(", "),
          developerMessage = ex.toString(),
        ),
      )
  }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class ErrorResponse(
  val status: Int,
  val errorCode: Int? = null,
  val userMessage: String? = null,
  val developerMessage: String? = null,
  val moreInfo: String? = null,
) {
  constructor(
    status: HttpStatus,
    errorCode: Int? = null,
    userMessage: String? = null,
    developerMessage: String? = null,
    moreInfo: String? = null,
  ) :
    this(status.value(), errorCode, userMessage, developerMessage, moreInfo)
}
