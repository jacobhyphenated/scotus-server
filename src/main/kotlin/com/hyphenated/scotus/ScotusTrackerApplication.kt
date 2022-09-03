package com.hyphenated.scotus

import com.hyphenated.scotus.case.Case
import com.hyphenated.scotus.case.CaseRepo
import com.hyphenated.scotus.term.Term
import com.hyphenated.scotus.term.TermRepo
import com.hyphenated.scotus.court.Court
import com.hyphenated.scotus.court.CourtRepo
import com.hyphenated.scotus.docket.Docket
import com.hyphenated.scotus.docket.DocketRepo
import com.hyphenated.scotus.justice.Justice
import com.hyphenated.scotus.justice.JusticeRepo
import com.hyphenated.scotus.opinion.Opinion
import com.hyphenated.scotus.opinion.OpinionJustice
import com.hyphenated.scotus.opinion.OpinionRepo
import com.hyphenated.scotus.opinion.OpinionType
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.filter.CommonsRequestLoggingFilter
import java.security.SecureRandom
import java.time.LocalDate

@SpringBootApplication
class ScotusTrackerApplication {

  @Profile("local")
  @Bean
  fun localTestData(justiceRepo: JusticeRepo, opinionRepo: OpinionRepo, caseRepo: CaseRepo,
                    docketRepo: DocketRepo, courtRepo: CourtRepo, termRepo: TermRepo) = CommandLineRunner {
    val ca09 = courtRepo.save(Court(null, "CA09", "Ninth Circuit Court of Appeals"))
    val ca05 = courtRepo.save(Court(null, "CA05", "Fifth Circuit Court of Appeals"))
    courtRepo.save(Court(null, "Federal", "Federal Appeals Court"))
    courtRepo.save(Court(null, "Kansas", "Kansas Supreme Court"))
    val texas = courtRepo.save(Court(null, "Texas", "Texas Supreme Court"))

    val j1 = justiceRepo.save(Justice(null,"RBG", LocalDate.of(1995, 5, 10), LocalDate.of(1950, 10, 10), null))
    justiceRepo.save(Justice(null,"John Paul Stevens", LocalDate.of(1999, 11, 10), LocalDate.of(1950, 10, 10), LocalDate.of(2009, 3, 1)))
    val j3 = justiceRepo.save(Justice(null,"Roberts", LocalDate.of(2005, 5, 10), LocalDate.of(1975, 10, 10), null))
    justiceRepo.save(Justice(null,"Elena Kegan", LocalDate.of(2010, 5, 10), LocalDate.of(1971, 4, 20), null))

    val t1 = termRepo.save(Term(null, "2019-2020", "OT2019"))

    val case = caseRepo.save(Case(null, "People v Other People", listOf(), "Some people sue some other people", "AFFIRMED",
        LocalDate.of(2019, 11, 11), "November", LocalDate.of(2020, 1, 15), null,
        "Judges decided to rule for people", null, t1, true, emptyList(), emptyList()))
    val decisionJustices = mutableListOf<OpinionJustice>()
    val decision = Opinion(null, case, OpinionType.MAJORITY, decisionJustices, "Important ruling on the issue")
    decisionJustices.add(OpinionJustice(null, true, decision, j1))
    decisionJustices.add(OpinionJustice(null, false, decision, j3))

    opinionRepo.save(decision)

    val case2 = caseRepo.save(Case(null, "texas v bacerra", listOf(), "CA and TX showdown", null, null, null, null,
        null, null, null, t1, false, emptyList(), emptyList()))

    docketRepo.save(Docket(null, case, "People v Other People","165464",  ca09, "Other People win this round", true, "REMANDED"))
    docketRepo.save(Docket(null, case, "All the People v Other People","165465"  ,ca05, "Other People win this round", true, "REMANDED"))
    docketRepo.save(Docket(null, case2, "Texas v Bacerra", "19-501", texas, "Ruled in favor of texas. Because Texas", null, "GRANTED"))
    docketRepo.save(Docket(null, null, "Goober v Peas","20-001", texas, "Docketing so hard right now", null, "REQUEST_CERT"))
  }

  @Bean
  fun passwordEncoder(): PasswordEncoder {
    return BCryptPasswordEncoder(10, SecureRandom())
  }

  @Bean
  fun loggingFilter(): CommonsRequestLoggingFilter {
    return CommonsRequestLoggingFilter().apply {
      setIncludeQueryString(true)
    }
  }
}

fun main(args: Array<String>) {
  runApplication<ScotusTrackerApplication>(*args)
}


