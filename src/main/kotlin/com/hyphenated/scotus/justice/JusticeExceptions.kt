package com.hyphenated.scotus.justice

import java.lang.RuntimeException

open class CreateWithIdException(type: String): RuntimeException("You cannot create a new $type with an Id. Did you mean to edit an existing $type")

class JusticeCreateWithIdException: CreateWithIdException("justice")

class CourtCreateWithIdException: CreateWithIdException("court")
