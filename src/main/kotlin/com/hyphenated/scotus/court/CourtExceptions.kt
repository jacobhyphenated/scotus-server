package com.hyphenated.scotus.court

import java.lang.RuntimeException

class CourtDeleteConstraintException(id: Long): RuntimeException("Cannot delete court with id '$id'. This court is currently in use.")