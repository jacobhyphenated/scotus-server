package com.hyphenated.scotus.config

import com.hyphenated.scotus.case.CaseService
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@ControllerAdvice
@RestController
class ExceptionController(private val env: Environment) {

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