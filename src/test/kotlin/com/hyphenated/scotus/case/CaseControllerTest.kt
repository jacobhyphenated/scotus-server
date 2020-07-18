package com.hyphenated.scotus.case

import com.hyphenated.scotus.case.term.Term
import com.hyphenated.scotus.court.Court
import com.hyphenated.scotus.docket.Docket
import com.hyphenated.scotus.docket.DocketCaseResponse
import com.hyphenated.scotus.opinion.Opinion
import com.hyphenated.scotus.opinion.OpinionJusticeResponse
import com.hyphenated.scotus.opinion.OpinionResponse
import com.hyphenated.scotus.opinion.OpinionType
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(CaseController::class)
@AutoConfigureRestDocs
class CaseControllerTest {

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var service: CaseService

  companion object{
    val caseFields = arrayOf(fieldWithPath("id").type(JsonFieldType.NUMBER).description("unique id for the case"),
        fieldWithPath("case").type(JsonFieldType.STRING).description("the case title"),
        fieldWithPath("shortSummary").type(JsonFieldType.STRING).description("A short description of the case"),
        fieldWithPath("argumentDate").type(JsonFieldType.STRING).optional().description("The date the case was argued (yyyy-MM-dd)"),
        fieldWithPath("decisionDate").type(JsonFieldType.STRING).optional().description("The date the Supreme Court ruled on the case (yyyy-MM-dd)"),
        fieldWithPath("result").type(JsonFieldType.STRING).optional().description("The high level result of the case. ex) 9-0"),
        fieldWithPath("decisionSummary").type(JsonFieldType.STRING).optional().description("At a very high level, what this ruling means"),
        fieldWithPath("status").type(JsonFieldType.STRING).description("Current status of the case"),
        fieldWithPath("important").type(JsonFieldType.BOOLEAN).description("A flag indicating this is an important case to watch"),
        fieldWithPath("term").type(JsonFieldType.OBJECT).description("The SCOTUS term the case where was granted"),
        fieldWithPath("term.id").type(JsonFieldType.NUMBER).description("Id of the term"),
        fieldWithPath("term.name").type(JsonFieldType.STRING).description("Term defined as a year range ex) 2014-2015"),
        fieldWithPath("term.otName").type(JsonFieldType.STRING).description("SCOTUS terms start in October and last until June. The \"OT\" term is the year of the October sitting ex) OT2014"))
  }

  private val caseResponseFull = responseFields(*caseFields,
      fieldWithPath("opinions[]").description("A list of each opinion in the case. Any case may have " +
          "multiple opinions with dissents and concurrences"))
      .andWithPrefix("opinions[].",
          fieldWithPath("id").optional().type(JsonFieldType.NUMBER).description("Id of the opinion"),
          fieldWithPath("caseId").optional().type(JsonFieldType.NUMBER).description("Id of the case associated with this opinion"),
          fieldWithPath("opinionType").optional().type(JsonFieldType.STRING).description("What kind of opinion this is (MAJORITY, CONCURRING, DISSENTING, etc."),
          fieldWithPath("summary").optional().type(JsonFieldType.STRING).description("A more detailed description of what this opinion is saying."),
          fieldWithPath("justices[]").optional().type(JsonFieldType.ARRAY).description("A list of the justices who joined this opinion"))
      .andWithPrefix("opinions[].justices[].",
          fieldWithPath("justiceId").optional().type(JsonFieldType.NUMBER).description("The Id of the justice"),
          fieldWithPath("isAuthor").optional().type(JsonFieldType.BOOLEAN).description("Flag to determine if this justice the author of this opinion"),
          fieldWithPath("justiceName").optional().type(JsonFieldType.STRING).description("Name of the justice"))
      .and(fieldWithPath("dockets[]").description("Individual cases from the lower courts seeking certiorari from the Supreme Court. Several may be combined into one SCOTUS case"))
      .andWithPrefix("dockets[].",
          fieldWithPath("docketId").description("Unique Docket Id. Can be used to look up more info on this particular lower court case"),
          fieldWithPath("docketNumber").description("Docket number identifies this case with the Supreme Court"),
          fieldWithPath("title").description("Title of the case in the lower court. May be the same as the case title"),
          fieldWithPath("lowerCourtOverruled").optional().type(JsonFieldType.BOOLEAN).description("Was the lower court decision overturned by the Supreme Court. Can be null if SCOTUS has not yet ruled on the case"),
          fieldWithPath("lowerCourt").description("Describes the lower appeals court this case came up through"))
      .andWithPrefix("dockets[].lowerCourt.",
          fieldWithPath("id").description("Unique Id for the lower court"),
          fieldWithPath("shortName").description("Short hand way of referring to the court"),
          fieldWithPath("name").description("Long form name of the appeals court"))

  private val termFields = arrayOf(
      fieldWithPath("id").description("Id of the term"),
      fieldWithPath("name").description("Term defined as a year range"),
      fieldWithPath("otName").description("October term (\"OT\") notation")
  )

  @Test
  fun testGetAll() {
    val case1 = Case(100, "Spy v Spy", "SNL makes it to the supreme court", "RESOLVED",
        LocalDate.of(2019, 11,25), LocalDate.of(2020,1,10), "5-4",
        "It was a close one, a lot of back and forth", Term(2, "2019-2020", "OT2019"), false, emptyList(), emptyList())

    val decisionCase2 = mutableListOf<Opinion>()
    val docketCase2 = mutableListOf<Docket>()
    val case2 =  Case(102, "People v Mr. Peanut","Mr peanut was murdered and the court needs to decide why",
        "RESOLVED", LocalDate.of(2020,2,2), LocalDate.of(2020, 2,3),
        "9-0", "Not justiciable", Term(1, "2020-2021", "OT2020"), true, decisionCase2, docketCase2)
    decisionCase2.add(Opinion(5, case2, OpinionType.PER_CURIUM, emptyList(), "DIG"))
    docketCase2.add(Docket(15, case2, "People v Mr. Peanut", "16-513", Court(1, "CA11", "11th circuit"),
        "Ruled for peanut", true, "REMANDED"))

    val case3 = Case(55, "Helicopter v Kobe", "Wrongful death estate claim", "ARGUED",
        LocalDate.of(2020,10,11), null, null, null,
        Term(1, "2020-2021", "OT2020"), false, emptyList(), emptyList())
    val cases = listOf(case1, case2, case3)

    whenever(service.getAllCases()).thenReturn(cases)

    this.mockMvc.perform(get("/cases"))
        .andDo(print())
        .andExpect(status().isOk)
        .andExpect(jsonPath("$", hasSize<Any>(3)))
        .andExpect(jsonPath("$[0].id", `is`(100)))
        .andExpect(jsonPath("$[0].argumentDate", `is`("2019-11-25")))
        .andExpect(jsonPath("$[0].term.id", `is`(2)))
        .andExpect(jsonPath("$[1].id", `is`(102)))
        .andExpect(jsonPath("$[1].decisionSummary", `is`("Not justiciable")))
        .andExpect(jsonPath("$[1].opinions").doesNotExist())
        .andExpect(jsonPath("$[1].dockets").doesNotExist())
        .andExpect(jsonPath("$[2].status", `is`("ARGUED")))
        .andDo(document("case/all", preprocessResponse(prettyPrint()),
            responseFields(
              fieldWithPath("[]").description("A list of cases")
            ).andWithPrefix("[].", *caseFields)
        ))
  }

  @Test
  fun testGetCaseByTerm() {
    val case1 = Case(100, "Spy v Spy", "SNL makes it to the supreme court", "RESOLVED",
        LocalDate.of(2019, 11,25), LocalDate.of(2020,1,10), "5-4",
        "It was a close one, a lot of back and forth", Term(50, "2019-2020", "OT2019"), false, emptyList(), emptyList())
    val cases = listOf(case1)

    whenever(service.getTermCases(50)).thenReturn(cases)

    this.mockMvc.perform(RestDocumentationRequestBuilders.get("/cases/term/{termId}", 50))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$", hasSize<Any>(1)))
        .andExpect(jsonPath("$[0].result", `is`("5-4")))
        .andDo(document("case/term",
            preprocessResponse(prettyPrint()),
            pathParameters(parameterWithName("termId").description("Id of SCOTUS Term to search for")),
            responseFields(
                fieldWithPath("[]").description("A list of cases in the term")
            ).andWithPrefix("[].", *caseFields)
        ))
  }

  @Test
  fun testSearchCasesByTitle() {
    val case1 = Case(100, "Spy v Spy", "SNL makes it to the supreme court", "RESOLVED",
        LocalDate.of(2019, 11,25), LocalDate.of(2020,1,10), "5-4",
        "It was a close one, a lot of back and forth", Term(50, "2019-2020", "OT2019"), false, emptyList(), emptyList())
    val cases = listOf(case1)

    whenever(service.searchByCaseTitle("spy")).thenReturn(cases)

    this.mockMvc.perform(RestDocumentationRequestBuilders.get("/cases/title/{title}", "spy"))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$", hasSize<Any>(1)))
        .andExpect(jsonPath("$[0].result", `is`("5-4")))
        .andDo(document("case/title",
            preprocessResponse(prettyPrint()),
            pathParameters(parameterWithName("title").description("Title of the case to search for, case insensitive, must match some part of the title")),
            responseFields(
                fieldWithPath("[]").description("A list of cases that match the search title")
            ).andWithPrefix("[].", *caseFields)
        ))
  }

  @Test
  fun testGetCaseById() {
    val majority = OpinionResponse(500, 200, OpinionType.MAJORITY, "Democracy and legislation are important. But individuals who are harmed should not have to wait" +
        " for legislative action. A ruling against same sex couples would leave long lasting injuries unjustified under the 14th Amendment. Same sex couples may now exercise the " +
        " fundamental right to marry in all states", listOf(
        OpinionJusticeResponse(50, true, "Anthony Kennedy"),
        OpinionJusticeResponse(51, false, "Ruth Bader Ginsburg"),
        OpinionJusticeResponse(52, false, "Elena Kagan"),
        OpinionJusticeResponse(53, false, "Sonya Sotomayor"),
        OpinionJusticeResponse(54, false, "Steven Breyer")))
    val dissent1 = OpinionResponse(501, 200,  OpinionType.DISSENT, "Judges have the power to say what the law is, not what the law should be. The constitution does not " +
        "provide a theory on marriage, so states should be free to define it. Roberts points out that the majority applies the substantive due process test, but that is an extreme remedy. " +
        "Roberts quotes the Justice Curtis' dissent in Dred Scott",
        listOf(OpinionJusticeResponse(55, true, "John Roberts"),
            OpinionJusticeResponse(56, false, "Antonin Scalia"),
            OpinionJusticeResponse(57, false, "Clerence Thomas")))
    val dissent2 = OpinionResponse(502, 200, OpinionType.DISSENT, "Joins Chief Justices Roberts' dissent in full. Today's decree means that the true rulers of 320 Million " +
        "Americans are the 9 justices on the Supreme Court. Public debate over same sex marriage was American democracy at its best until the court stepped in. The Majority decision accuses " +
        "every state of violating the constitution for the last 135 years because of a fundamental right they just discovered.",
        listOf(OpinionJusticeResponse(56, true, "Anton Scalia"),
            OpinionJusticeResponse(57, false, "Clerence Thomas")))
    val dissent3 = OpinionResponse(503, 200, OpinionType.DISSENT, "The majority decision is at odds with the constitution and the principals the nation was build on. " +
        "The Due Process clause should not be used to create substantive rights. Thomas also does not buy that same sex couples are deprived of liberty if their marriage is not recognized.",
        listOf(OpinionJusticeResponse(57, true, "Clerence Thomas"),
            OpinionJusticeResponse(56, false, "Anton Scalia")))
    val dissent4 = OpinionResponse(504, 200, OpinionType.DISSENT, "Constitution does not and should not answer the questions around same sex marriage. Rights protected by " +
        "the due process clause should only be those deeply rooted in the nations history and tradition. No country allowed same sex marriage until 2000. Traditionally marriage is linked " +
        "to procreation. While that understanding may not hold true today, states should be allowed to try to hang onto that tradition. Alito muses about how anyone who disagrees will be " +
        "labeled a bigot and treated as such by employers, government, and schools.",
        listOf(OpinionJusticeResponse(58, true, "Samuel Alito"),
            OpinionJusticeResponse(57, false, "Clerence Thomas"),
            OpinionJusticeResponse(56, false, "Anton Scalia")))

    val docket1 = DocketCaseResponse(1, "14-556", "Obergefell v. Hodges", Court(1, "CA06", "6th Circuit Court of Appeals"), true)
    val docket2 = DocketCaseResponse(2, "14-562", "Tanco v. Haslam", Court(1, "CA06", "6th Circuit Court of Appeals"), true)
    val docket3 = DocketCaseResponse(3, "14-571", "DeBoer v. Snyder", Court(1, "CA06", "6th Circuit Court of Appeals"), true)
    val docket4 = DocketCaseResponse(4, "14-574", "Bourke v. Beshear", Court(1, "CA06", "6th Circuit Court of Appeals"), true)

    val case = CaseResponse(200, "Obergefell v Hodges", "A state marriage license for a same sex couple should be recognized in all states",
        "RESOLVED", LocalDate.of(2015,4,28), LocalDate.of(2015,6,26),
        "5-4", "Right to marry is a fundamental right guaranteed by the Fourteenth Amendment. State laws prohibiting same sex marriage are invalidated",
        Term(33, "2014-2015", "OT2014"), true, listOf(majority, dissent1, dissent2, dissent3, dissent4), listOf(docket1, docket2, docket3, docket4))

    whenever(service.getCase(200)).thenReturn(case)

    this.mockMvc.perform(RestDocumentationRequestBuilders.get("/cases/{caseId}", 200))
        .andExpect(status().isOk)
        .andExpect(jsonPath("case", `is`("Obergefell v Hodges")))
        .andExpect(jsonPath("result", `is`("5-4")))
        .andExpect(jsonPath("important").value(true))
        .andExpect(jsonPath("opinions", hasSize<Any>(5)))
        .andExpect(jsonPath("opinions[0].opinionType", `is`("MAJORITY")))
        .andExpect(jsonPath("opinions[0].justices[0].isAuthor", `is`(true)))
        .andExpect(jsonPath("opinions[0].justices[0].justiceName", `is`("Anthony Kennedy")))
        .andDo(print())
        .andDo(document("case/id",
            preprocessResponse(prettyPrint()),
            pathParameters(parameterWithName("caseId").description("ID of the case")),
            caseResponseFull
        ))
  }

  @Test
  fun testNoCaseFoundById() {
    this.mockMvc.perform(get("/cases/60"))
        .andExpect(status().isNotFound)
  }

  @Test
  fun testCreateCase() {
    val request = "{\"case\":\"Bostock v. Clayton County, Georgia\",\"shortSummary\":\"Civil rights act Title VII prohibits discrimination based on sex. " +
        "The question is: does this cover sexual orientation. \",\"status\":\"PENDING\",\"termId\":50,\"important\":false,\"docketIds\":[18,22]}"

    whenever(service.createCase(any())).thenAnswer {
      val arg = it.arguments[0] as CreateCaseRequest
      val mockDockets = listOf(
          DocketCaseResponse(18,  "19-225", "Bostock v. Clayton County, Georgia", Court(5, "CA11", "11th Circuit"), null),
          DocketCaseResponse(22, "19-228", "R.G Funeral Holmes v. EEOC", Court(2, "CA02", "2nd Circuit"), null))
      CaseResponse(100, arg.case, arg.shortSummary, arg.status, null, null, null, null,
          Term(arg.termId, "2019-2020", "OT2019"), arg.important, emptyList(), mockDockets)
    }

    this.mockMvc.perform(post("/cases")
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
        .andExpect(status().isCreated)
        .andExpect(jsonPath("id").value(100))
        .andExpect(jsonPath("case").value("Bostock v. Clayton County, Georgia"))
        .andExpect(jsonPath("dockets").value(hasSize<Any>(2)))
        .andDo(document("case/admin/create",
            preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            requestFields(
                fieldWithPath("case").description("the case title"),
                fieldWithPath("shortSummary").description("A short description of the case"),
                fieldWithPath("status").description("Current status of the case"),
                fieldWithPath("termId").description("The Id of the SCOTUS term the case where was granted"),
                fieldWithPath("important").description("True if this is an important case to watch"),
                fieldWithPath("docketIds").description("Array of Id's referencing dockets this case comes from")
            ),
            caseResponseFull
        ))
  }

  @Test
  fun testEditCase() {
    val request = "{\"status\":\"ARGUMENT_SCHEDULED\",\"argumentDate\":\"2020-03-28\"}"

    val dockets = listOf(
        DocketCaseResponse(18,  "19-225", "Bostock v. Clayton County, Georgia", Court(5, "CA11", "11th Circuit"), null),
        DocketCaseResponse(22, "19-228", "R.G Funeral Holmes v. EEOC", Court(2, "CA02", "2nd Circuit"), null))
    val caseResponse = CaseResponse(100, "Bostock v. Clayton County, Georgia", "Civil rights act Title VII prohibits discrimination based on sex.",
        "ARGUMENT_SCHEDULED", LocalDate.of(2020,3,28), null, null, null,
        Term(33, "2019-2020", "OT2019"), true, emptyList(), dockets)

    whenever(service.editCase(eq(100), any())).thenReturn(caseResponse)

    this.mockMvc.perform(RestDocumentationRequestBuilders.patch("/cases/{caseId}", 100)
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
        .andExpect(status().isOk)
        .andExpect(jsonPath("id").value(100))
        .andDo(document("case/admin/edit",
            preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            pathParameters(parameterWithName("caseId").description("Id of the case to modify")),
            requestFields(
                fieldWithPath("case").type(JsonFieldType.STRING).optional().description("(optional) the case title"),
                fieldWithPath("shortSummary").type(JsonFieldType.STRING).optional().description("(optional) A short description of the case"),
                fieldWithPath("argumentDate").type(JsonFieldType.STRING).optional().description("(optional) The date the case was argued (yyyy-MM-dd)"),
                fieldWithPath("decisionDate").type(JsonFieldType.STRING).optional().description("(optional) The date the Supreme Court ruled on the case (yyyy-MM-dd)"),
                fieldWithPath("result").type(JsonFieldType.STRING).optional().description("(optional) The high level result of the case. ex) 9-0"),
                fieldWithPath("decisionSummary").type(JsonFieldType.STRING).optional().description("(optional) At a very high level, what this ruling means"),
                fieldWithPath("status").type(JsonFieldType.STRING).optional().description("(optional) Current status of the case"),
                fieldWithPath("termId").type(JsonFieldType.NUMBER).optional().description("(optional) The Id of the SCOTUS term the case where was granted")
            ),
            caseResponseFull
        ))
  }

  @Test
  fun testAssignDocket() {

    val dockets = listOf(
        DocketCaseResponse(18,  "19-225","Bostock v. Clayton County, Georgia",  Court(5, "CA11", "11th Circuit"), null),
        DocketCaseResponse(22, "19-228", "R.G Funeral Holmes v. EEOC", Court(2, "CA02", "2nd Circuit"), null))
    val caseResponse = CaseResponse(100, "Bostock v. Clayton County, Georgia", "Civil rights act Title VII prohibits discrimination based on sex.",
        "ARGUMENT_SCHEDULED", LocalDate.of(2020,3,28), null, null, null,
        Term(33, "2019-2020", "OT2019"), true, emptyList(), dockets)

    whenever(service.assignDocket(eq(100), eq(22))).thenReturn(caseResponse)

    this.mockMvc.perform(RestDocumentationRequestBuilders.put("/cases/{caseId}/dockets/{docketId}", 100, 22))
        .andExpect(status().isOk)
        .andExpect(jsonPath("id").value(100))
        .andDo(document("case/admin/assign",
            preprocessResponse(prettyPrint()),
            pathParameters(
                parameterWithName("caseId").description("Id of the case"),
                parameterWithName("docketId").description("Id of the docket to add to the case")
            ),
            caseResponseFull
        ))
  }

  @Test
  fun testRemoveDocket() {
    this.mockMvc.perform(RestDocumentationRequestBuilders.delete("/cases/{caseId}/dockets/{docketId}", 100, 18))
        .andExpect(status().isNoContent)
        .andExpect(jsonPath("id").doesNotExist())
        .andDo(document("case/admin/remove",
            pathParameters(
                parameterWithName("caseId").description("Id of the case"),
                parameterWithName("docketId").description("Id of the docket to remove from the case")
            )
        ))
    verify(service).removeDocket(100, 18)
  }

  @Test
  fun testGetAllTerms() {
    val terms = listOf(
        Term(1, "2014-2015", "OT2014"),
        Term(2, "2015-2016", "OT2015"),
        Term(3, "2016-2017", "OT2016")
    )
    whenever(service.getAllTerms()).thenReturn(terms)

    this.mockMvc.perform(get("/cases/term"))
        .andExpect(jsonPath("$").value(hasSize<Any>(3)))
        .andExpect(jsonPath("[0].otName").value("OT2014"))
        .andDo(document("case/allterm",
            preprocessResponse(prettyPrint()),
            responseFields(
                fieldWithPath("[]").description("List of terms")
            ).andWithPrefix("[].", *termFields)
        ))
  }

  @Test
  fun testCreateTerm() {
    whenever(service.createTerm("2020-2021", "OT2020"))
        .thenReturn(Term(5, "2020-2021", "OT2020"))

    val request = "{\"name\":\"2020-2021\",\"otName\":\"OT2020\"}"

    this.mockMvc.perform(post("/cases/term")
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
        .andExpect(status().isCreated)
        .andExpect(jsonPath("otName").value("OT2020"))
        .andDo(document("case/admin/term",
            preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            requestFields(
                *termFields.copyOfRange(1, termFields.size)
            ),
            responseFields(*termFields)
        ))
  }
}
