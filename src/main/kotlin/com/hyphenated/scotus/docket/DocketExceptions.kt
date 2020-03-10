package com.hyphenated.scotus.docket

import java.lang.RuntimeException

// Exception handler will return 404 for these errors
open class ObjectNotFoundException(id: Long, type: String): RuntimeException("$type with id '$id' does not exist")
class DocketNotFoundException(val id: Long): ObjectNotFoundException(id, "Docket")
class CourtNotFoundException(val id: Long): ObjectNotFoundException(id, "Court")
class CaseNotFoundException(val id: Long): ObjectNotFoundException(id, "Case")
class OpinionNotFoundException(val id: Long): ObjectNotFoundException(id, "Opinion")

// Exception hander will return 400 for these errors
open class NoEntityIdException(id: Long, type: String): RuntimeException("$type with id '$id' does not exist")
class NoCourtIdException(id: Long): NoEntityIdException(id, "Court")
class NoCaseIdException(id: Long): NoEntityIdException(id, "Case")
class NoDocketIdException(id: Long): NoEntityIdException(id, "Docket")
class NoJusticeIdException(id: Long): NoEntityIdException(id, "Justice")