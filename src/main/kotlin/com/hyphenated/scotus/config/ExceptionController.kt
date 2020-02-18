package com.hyphenated.scotus.config

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.hyphenated.scotus.docket.DocketNotFoundException
import com.hyphenated.scotus.docket.NoCaseIdException
import com.hyphenated.scotus.docket.NoCourtIdException
import com.hyphenated.scotus.docket.ObjectNotFoundException
import com.hyphenated.scotus.justice.CreateWithIdException
import com.hyphenated.scotus.justice.JusticeCreateWithIdException
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@ControllerAdvice
@RestController
class ExceptionController(private val env: Environment) {

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException::class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  fun accessDeniedHandler(e: org.springframework.security.access.AccessDeniedException): ErrorResponse {
    log.warn("Access Denied", e)
    return ErrorResponse("ACCESS_DENIED", e.message, null)
  }

  @ExceptionHandler(CreateWithIdException::class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  fun justiceCreationError(e: CreateWithIdException): ErrorResponse {
    return ErrorResponse("CREATE_WITH_ID_PRESENT", e.message, null)
  }

  @ExceptionHandler(NoCourtIdException::class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  fun handleNoCourtId(e: NoCourtIdException): ErrorResponse {
    return ErrorResponse("INVALID_ID", "Court with id '${e.id}' does not exist", null)
  }

  @ExceptionHandler(NoCaseIdException::class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  fun handleNoCaseId(e: NoCaseIdException): ErrorResponse {
    return ErrorResponse("INVALID_ID", "Case with id '${e.id}' does not exist", null)
  }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  fun missingKotlinParameterHandler(e: HttpMessageNotReadableException): ErrorResponse {
    val causedBy = e.cause
    return if (causedBy is MissingKotlinParameterException) {
      ErrorResponse("MISSING_PARAMETER",
          "Required parameter: ${causedBy.parameter.name} (${causedBy.parameter.type}) is missing or null",
          null)
    } else {
      log.error("Unhandled HttpMessageNotReadableException", e)
      ErrorResponse("INVALID_REQUEST",
          "Could not parse the request",
          if (isLocal()) ExceptionUtils.getStackTrace(e) else null)
    }
  }

  @ExceptionHandler(MethodArgumentNotValidException::class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  fun requestNotValidHandler(e: MethodArgumentNotValidException): ErrorResponse {
    return ErrorResponse("INVALID_PARAMETER",
        (e.bindingResult.allErrors.firstOrNull() as? FieldError)
            ?.let { "Invalid value for '${it.field}': ${it.defaultMessage}" }
            ?: e.message,
        null)
  }

  @ExceptionHandler(ObjectNotFoundException::class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  fun requestNotValidHandler(e: ObjectNotFoundException): ErrorResponse {
    return ErrorResponse("INVALID_ID", e.message, null)
  }

  @ExceptionHandler(Exception::class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  fun genericExceptionHandler(exception: Exception): ErrorResponse {
    log.error("Caught exception in generic handler", exception)
    return ErrorResponse(
        "GENERIC_ERROR",
        exception.message,
        if (isLocal()) ExceptionUtils.getStackTrace(exception) else null
    )
  }

  private fun isLocal(): Boolean {
    return env.activeProfiles.contains("local")
  }

  companion object {
    private val log = LoggerFactory.getLogger(ExceptionController::class.java)
  }
}

class ErrorResponse(
    val errorCode: String,
    val errorMessage: String?,
    val stackTrace: String?
)