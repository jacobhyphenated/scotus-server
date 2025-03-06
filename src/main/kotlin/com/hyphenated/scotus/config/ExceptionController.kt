package com.hyphenated.scotus.config

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.hyphenated.scotus.case.DocketAlreadyAssignedException
import com.hyphenated.scotus.court.CourtDeleteConstraintException
import com.hyphenated.scotus.docket.NoEntityIdException
import com.hyphenated.scotus.docket.ObjectNotFoundException
import com.hyphenated.scotus.justice.CreateWithIdException
import com.hyphenated.scotus.opinion.MultipleOpinionAuthorException
import com.hyphenated.scotus.opinion.NoOpinionAuthorException
import com.hyphenated.scotus.tag.TagDeleteConstraintException
import com.hyphenated.scotus.user.UsernameNotAvailable
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

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
  fun handleCreateNewWithIdPresentException(e: CreateWithIdException): ErrorResponse {
    return ErrorResponse("CREATE_WITH_ID_PRESENT", e.message, null)
  }

  @ExceptionHandler(NoEntityIdException::class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  fun handleNoIdException(e: NoEntityIdException): ErrorResponse {
    return ErrorResponse("INVALID_ID", e.message, null)
  }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  fun httpMessageNotReadableHandler(e: HttpMessageNotReadableException): ErrorResponse {
    return when (val causedBy = e.cause) {
      is InvalidFormatException -> {
        ErrorResponse("INVALID_FORMAT",
          causedBy.localizedMessage,
          null)
      }
      is MismatchedInputException -> {
        ErrorResponse("MISSING_PARAMETER",
          causedBy.localizedMessage,
          null)
      }
      else -> {
        log.error("Unhandled HttpMessageNotReadableException", e)
        ErrorResponse("INVALID_REQUEST",
          "Could not parse the request",
          if (isLocal()) ExceptionUtils.getStackTrace(e) else null)
      }
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

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  fun requestNotValidHandler(e: MethodArgumentTypeMismatchException): ErrorResponse {
    return when (e.cause) {
      is NumberFormatException -> {
        ErrorResponse("INVALID_PARAMETER",
          "${e.propertyName ?: "parameter"} must be a number",
          null
        )
      }
      else -> {
        log.error("Unhandled MethodArgumentTypeMismatchException", e)
        ErrorResponse("INVALID_PARAMETER",
          e.localizedMessage,
          if (isLocal()) ExceptionUtils.getStackTrace(e) else null
        )
      }
    }
  }


  @ExceptionHandler(CourtDeleteConstraintException::class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  fun deleteCourtConstraintHandler(e: CourtDeleteConstraintException): ErrorResponse {
    return ErrorResponse("CONSTRAINT_VIOLATION", e.message, null)
  }

  @ExceptionHandler(TagDeleteConstraintException::class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  fun deleteTagConstraintHandler(e: TagDeleteConstraintException): ErrorResponse {
    return ErrorResponse("CONSTRAINT_VIOLATION", e.message, null)
  }

  @ExceptionHandler(DocketAlreadyAssignedException::class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  fun docketAssignedHandler(e: DocketAlreadyAssignedException): ErrorResponse {
    return ErrorResponse("CONSTRAINT_VIOLATION", "Cannot add this docket to the case, docket is already associated with a case", null)
  }

  @ExceptionHandler(NoOpinionAuthorException::class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  fun noOpinionAuthorHandler(e: NoOpinionAuthorException): ErrorResponse {
    return ErrorResponse("NO_AUTHOR", "Cannot create an opinion with no author", null)
  }

  @ExceptionHandler(MultipleOpinionAuthorException::class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  fun multipleOpinionAuthorHandler(e: MultipleOpinionAuthorException): ErrorResponse {
    return ErrorResponse("MULTIPLE_AUTHORS", "An opinion can have only one author", null)
  }

  @ExceptionHandler(UsernameNotAvailable::class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  fun usernameNotAvailableHandler(e: UsernameNotAvailable): ErrorResponse {
    return ErrorResponse("USERNAME_NOT_AVAILABLE", e.message, null)
  }

  @ExceptionHandler(DataIntegrityViolationException::class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  fun dataIntegrityHandler(e: DataIntegrityViolationException): ErrorResponse {
    val message = if (e.mostSpecificCause.message?.contains(("value too long for type character varying")) == true) {
       "Value is too long for the input field"
    } else {
      "Not a valid value"
    }
    return ErrorResponse("DATA_INTEGRITY_VIOLATION",
        message,
        if (isLocal()) ExceptionUtils.getStackTrace(e) else null)
  }

  @ExceptionHandler(ObjectNotFoundException::class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  fun requestNotValidHandler(e: ObjectNotFoundException): ErrorResponse {
    return ErrorResponse("INVALID_ID", e.message, null)
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
  @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
  fun handleMethodNotSupported(e: HttpRequestMethodNotSupportedException): ErrorResponse {
    return ErrorResponse("METHOD_NOT_SUPPORTED", e.message, null)
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
    return env.activeProfiles.contains("local") || env.activeProfiles.contains("dev")
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