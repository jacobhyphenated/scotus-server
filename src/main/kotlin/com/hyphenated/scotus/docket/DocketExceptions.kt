package com.hyphenated.scotus.docket

import java.lang.RuntimeException

open class ObjectNotFoundException(id: Long, type: String): RuntimeException("$type with id '$id' does not exist")
class DocketNotFoundException(val id: Long): ObjectNotFoundException(id, "Docket")
class CourtNotFoundException(val id: Long): ObjectNotFoundException(id, "Court")

class NoCourtIdException(val id: Long): RuntimeException()

class NoCaseIdException(val id: Long): RuntimeException()