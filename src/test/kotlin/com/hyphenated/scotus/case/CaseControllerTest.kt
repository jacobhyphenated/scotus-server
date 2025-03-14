package com.hyphenated.scotus.case

import com.hyphenated.scotus.term.Term
import com.hyphenated.scotus.court.Court
import com.hyphenated.scotus.court.CourtControllerTests
import com.hyphenated.scotus.docket.Docket
import com.hyphenated.scotus.docket.DocketCaseResponse
import com.hyphenated.scotus.justice.Justice
import com.hyphenated.scotus.justice.JusticeControllerTest
import com.hyphenated.scotus.opinion.Opinion
import com.hyphenated.scotus.opinion.OpinionJusticeResponse
import com.hyphenated.scotus.opinion.OpinionResponse
import com.hyphenated.scotus.opinion.OpinionType
import com.hyphenated.scotus.search.SearchService
import com.hyphenated.scotus.tag.Tag
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(CaseController::class)
@AutoConfigureRestDocs
@AutoConfigureMockMvc(addFilters = false)
class CaseControllerTest {

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var service: CaseService

  @MockBean
  private lateinit var searchService: SearchService

  companion object{
    val caseFields = arrayOf(fieldWithPath("id").type(JsonFieldType.NUMBER).description("unique id for the case"),
        fieldWithPath("case").type(JsonFieldType.STRING).description("the case title"),
        fieldWithPath("shortSummary").type(JsonFieldType.STRING).description("A short description of the case"),
        fieldWithPath("argumentDate").type(JsonFieldType.STRING).optional().description("The date the case was argued (yyyy-MM-dd)"),
        fieldWithPath("sitting").type(JsonFieldType.STRING).optional().description("Scotus argument sitting. SCOTUS hears many cases at a time, broken up into sittings (October, November, etc.)"),
        fieldWithPath("decisionDate").type(JsonFieldType.STRING).optional().description("The date the Supreme Court ruled on the case (yyyy-MM-dd)"),
        fieldWithPath("result").type(JsonFieldType.STRING).optional().description("The high level result of the case. ex) 9-0"),
        fieldWithPath("decisionSummary").type(JsonFieldType.STRING).optional().description("At a very high level, what this ruling means"),
        fieldWithPath("status").type(JsonFieldType.STRING).description("Current status of the case"),
        fieldWithPath("important").type(JsonFieldType.BOOLEAN).description("A flag indicating this is an important case to watch"),
        fieldWithPath("term").type(JsonFieldType.OBJECT).description("The SCOTUS term the case where was granted"),
        fieldWithPath("term.id").type(JsonFieldType.NUMBER).description("Id of the term"),
        fieldWithPath("term.name").type(JsonFieldType.STRING).description("Term defined as a year range ex) 2014-2015"),
        fieldWithPath("term.otName").type(JsonFieldType.STRING).description("SCOTUS terms start in October and last until June. The \"OT\" term is the year of the October sitting ex) OT2014"),
        fieldWithPath("term.inactive").type(JsonFieldType.BOOLEAN).description("Inactive terms do not contain all cases from that term"))
  }

  private val caseResponseFull = responseFields(*caseFields,
      fieldWithPath("decisionLink").type(JsonFieldType.STRING).optional().description("A link to the official court opinion"),
      fieldWithPath("resultStatus").type(JsonFieldType.STRING).optional().description("Subset of case status that defines the result of the case (can be null)"),
      fieldWithPath("alternateTitles[]").type(JsonFieldType.ARRAY).description("A list of other titles this case can sometimes refer to this case."),
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
      .and(fieldWithPath("tags[]").optional().type(JsonFieldType.ARRAY).description("List of tags associated with this case"))
      .andWithPrefix("tags[]",
          fieldWithPath("id").optional().type(JsonFieldType.NUMBER).description("Unique Id for the tag"),
          fieldWithPath("name").optional().type(JsonFieldType.STRING).description("Name of the tag"),
          fieldWithPath("description").optional().type(JsonFieldType.STRING).description("A description of the tag"))

  @Test
  fun testGetAll() {
    val case1 = Case(100, "Spy v Spy", listOf(), "SNL makes it to the supreme court", "RESOLVED",
        LocalDate.of(2019, 11,25), "November", LocalDate.of(2020,1,10),
        "http://example.com/opinion.pdf", "5-4","It was a close one, a lot of back and forth",
        Term(2, "2019-2020", "OT2019"), false, emptyList(), emptyList(), emptyList()
    )

    val decisionCase2 = mutableListOf<Opinion>()
    val docketCase2 = mutableListOf<Docket>()
    val case2 =  Case(102, "People v Mr. Peanut", listOf(),"Mr peanut was murdered and the court needs to decide why",
        "RESOLVED", LocalDate.of(2020,2,2), "February", LocalDate.of(2020, 2,3),
        "http://example.com/opinion.pdf","9-0", "Not justiciable",
        Term(1, "2020-2021", "OT2020"), true, decisionCase2, docketCase2, emptyList()
    )
    decisionCase2.add(Opinion(5, case2, OpinionType.PER_CURIUM, emptyList(), "DIG"))
    docketCase2.add(Docket(15, case2, "People v Mr. Peanut", "16-513", Court(1, "CA11", "11th circuit"),
        "Ruled for peanut", true, "REMANDED"))

    val case3 = Case(55, "Helicopter v Kobe", listOf(), "Wrongful death estate claim", null,
        LocalDate.of(2020,10,11), "October", null, null, null, null,
        Term(1, "2020-2021", "OT2020"), false, emptyList(), emptyList(), emptyList()
    )
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
    val case1 = Case(100, "Spy v Spy", listOf(), "SNL makes it to the supreme court", "RESOLVED",
        LocalDate.of(2019, 11,25), "November", LocalDate.of(2020,1,10), null, "5-4",
        "It was a close one, a lot of back and forth", Term(50, "2019-2020", "OT2019"), false, emptyList(), emptyList(), emptyList()
    )
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
  fun testSearchCases() {
    val case1 = Case(100, "Spy v Spy", listOf(),"SNL makes it to the supreme court", "RESOLVED",
        LocalDate.of(2019, 11,25), "November", LocalDate.of(2020,1,10), null, "5-4",
        "It was a close one, a lot of back and forth", Term(50, "2019-2020", "OT2019"), false, emptyList(), emptyList(), emptyList()
    )
    val cases = listOf(case1)

    whenever(searchService.searchCases("spy")).thenReturn(cases)

    this.mockMvc.perform(RestDocumentationRequestBuilders.get("/cases/search/{searchTerm}", "spy"))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$", hasSize<Any>(1)))
        .andExpect(jsonPath("$[0].result", `is`("5-4")))
        .andDo(document("case/search",
            preprocessResponse(prettyPrint()),
            pathParameters(parameterWithName("searchTerm").description("Searches the case title, description, related dockets, and opinions")),
            responseFields(
                fieldWithPath("[]").description("A list of cases where there is a full or close match to the search term or phrase")
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

    val altTitles = listOf("Obergfell v. Hodges", "Bourke v. Beshear")

    val case = CaseResponse(200, "Obergefell v Hodges", altTitles, "A state marriage license for a same sex couple should be recognized in all states",
        "REVERSED", "REVERSED", LocalDate.of(2015,4,28), "April", LocalDate.of(2015,6,26),
        "5-4", "Right to marry is a fundamental right guaranteed by the Fourteenth Amendment. State laws prohibiting same sex marriage are invalidated",
        "https://www.supremecourt.gov/opinions/14pdf/14-556_3204.pdf", Term(33, "2014-2015", "OT2014"),
        true, listOf(majority, dissent1, dissent2, dissent3, dissent4), listOf(docket1, docket2, docket3, docket4), emptyList()
    )

    whenever(service.getCase(200)).thenReturn(case)

    this.mockMvc.perform(RestDocumentationRequestBuilders.get("/cases/{caseId}", 200))
        .andExpect(status().isOk)
        .andExpect(jsonPath("case", `is`("Obergefell v Hodges")))
        .andExpect(jsonPath("result", `is`("5-4")))
        .andExpect(jsonPath("important").value(true))
        .andExpect(jsonPath("decisionLink").value("https://www.supremecourt.gov/opinions/14pdf/14-556_3204.pdf"))
        .andExpect(jsonPath("opinions", hasSize<Any>(5)))
        .andExpect(jsonPath("opinions[0].opinionType", `is`("MAJORITY")))
        .andExpect(jsonPath("opinions[0].justices[0].isAuthor", `is`(true)))
        .andExpect(jsonPath("alternateTitles[0]").value("Obergfell v. Hodges"))
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
        "The question is: does this cover sexual orientation. \",\"termId\":50,\"important\":false,\"docketIds\":[18,22], \"alternateTitles\":[\"Bostock v. Clayton County, GA\"]}"

    whenever(service.createCase(any())).thenAnswer {
      val arg = it.arguments[0] as CreateCaseRequest
      val mockDockets = listOf(
          DocketCaseResponse(18,  "19-225", "Bostock v. Clayton County, Georgia", Court(5, "CA11", "11th Circuit"), null),
          DocketCaseResponse(22, "19-228", "R.G Funeral Holmes v. EEOC", Court(2, "CA02", "2nd Circuit"), null))
      CaseResponse(100, arg.case, listOf("Bostock v. Clayton County, GA"), arg.shortSummary, "GRANTED", null, null, null, null, null, null,
          null, Term(arg.termId, "2019-2020", "OT2019"), arg.important, emptyList(), mockDockets, emptyList()
      )
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
                fieldWithPath("termId").description("The Id of the SCOTUS term the case where was granted"),
                fieldWithPath("important").description("True if this is an important case to watch"),
                fieldWithPath("docketIds").description("Array of Id's referencing dockets this case comes from"),
                fieldWithPath("alternateTitles").description("(optional) Array of alternate case titles that can refer to this case")
            ),
            caseResponseFull
        ))
    verify(searchService).indexCase(100)
  }

  @Test
  fun testEditCase() {
    val request = "{\"argumentDate\":\"2020-03-28\"}"

    val tags = listOf(Tag(1, "Judicial Procedure", "Involving the process and procedures in which cases are handled", emptyList()))

    val dockets = listOf(
        DocketCaseResponse(18,  "19-225", "Bostock v. Clayton County, Georgia", Court(5, "CA11", "11th Circuit"), null),
        DocketCaseResponse(22, "19-228", "R.G Funeral Holmes v. EEOC", Court(2, "CA02", "2nd Circuit"), null))
    val caseResponse = CaseResponse(100, "Bostock v. Clayton County, Georgia", listOf(), "Civil rights act Title VII prohibits discrimination based on sex.",
        "ARGUMENT_SCHEDULED", null, LocalDate.of(2020,3,28), "April", null, null, null,
        null, Term(33, "2019-2020", "OT2019"), true, emptyList(), dockets, tags
    )

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
                fieldWithPath("sitting").type(JsonFieldType.STRING).optional().description("(optional) Group of cases argued before the court in the same time period (November, March, etc.)"),
                fieldWithPath("decisionDate").type(JsonFieldType.STRING).optional().description("(optional) The date the Supreme Court ruled on the case (yyyy-MM-dd)"),
                fieldWithPath("result").type(JsonFieldType.STRING).optional().description("(optional) The high level result of the case. ex) 9-0"),
                fieldWithPath("decisionSummary").type(JsonFieldType.STRING).optional().description("(optional) At a very high level, what this ruling means"),
                fieldWithPath("resultStatus").type(JsonFieldType.STRING).optional().description("(optional) Describes the result of the case"),
                fieldWithPath("termId").type(JsonFieldType.NUMBER).optional().description("(optional) The Id of the SCOTUS term the case where was granted"),
                fieldWithPath("alternateTitles").type(JsonFieldType.ARRAY).optional().description("(optional) Array of alternate case titles that can refer to this case. If this argument is present, including an empty list, it will override the current value"),
                fieldWithPath("tagIds").type(JsonFieldType.ARRAY).optional().description("(optional) List of tag ids corresponding to the tags that should be associated with the case")
            ),
            caseResponseFull
        ))
    verify(searchService).indexCase(100)
  }

  @Test
  fun testEditCaseNotFound() {
    val request = "{\"resultStatus\":\"REMANDED\"}"
    whenever(service.editCase(any(), any())).thenReturn(null)
    this.mockMvc.perform(patch("/cases/500")
      .contentType(MediaType.APPLICATION_JSON)
      .content(request))
      .andExpect(status().`is`(404))

    verify(service).editCase(eq(500), any())
  }

  @Test
  fun testRemoveArgumentDate() {
    val dockets = listOf(
      DocketCaseResponse(18,  "19-225", "Bostock v. Clayton County, Georgia", Court(5, "CA11", "11th Circuit"), null),
      DocketCaseResponse(22, "19-228", "R.G Funeral Holmes v. EEOC", Court(2, "CA02", "2nd Circuit"), null))
    val caseResponse = CaseResponse(100, "Bostock v. Clayton County, Georgia", listOf(), "Civil rights act Title VII prohibits discrimination based on sex.",
      "GRANTED", null, null, null,  null, null, null,
      null, Term(33, "2019-2020", "OT2019"), true, emptyList(), dockets, emptyList()
    )

    whenever(service.removeArgumentDate(100)).thenReturn(caseResponse)

    this.mockMvc.perform(RestDocumentationRequestBuilders.delete("/cases/{caseId}/argumentDate", 100))
      .andExpect(status().isOk)
      .andExpect(jsonPath("id").value(100))
      .andExpect(jsonPath("argumentDate").isEmpty)
      .andDo(document("case/admin/removeArgumentDate",
        preprocessResponse(prettyPrint()),
        pathParameters(parameterWithName("caseId").description("Id of the case to modify")),
        caseResponseFull
      ))
  }

  @Test
  fun testRemoveArgumentDateNotFound() {
    whenever(service.removeArgumentDate(500)).thenReturn(null)
    this.mockMvc.perform(delete("/cases/500/argumentDate"))
      .andExpect(status().`is`(404))
  }

  @Test
  fun testAssignDocket() {

    val dockets = listOf(
        DocketCaseResponse(18,  "19-225","Bostock v. Clayton County, Georgia",  Court(5, "CA11", "11th Circuit"), null),
        DocketCaseResponse(22, "19-228", "R.G Funeral Holmes v. EEOC", Court(2, "CA02", "2nd Circuit"), null))
    val caseResponse = CaseResponse(100, "Bostock v. Clayton County, Georgia", listOf(), "Civil rights act Title VII prohibits discrimination based on sex.",
        "ARGUMENT_SCHEDULED", null, LocalDate.of(2020,3,28), "April", null, null, null,
        null, Term(33, "2019-2020", "OT2019"), true, emptyList(), dockets, emptyList()
    )

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
    verify(searchService).indexCase(100)
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
    verify(searchService).indexCase(100)
  }

  @Test
  fun testTermSummary() {
    val rgb = Justice(1, "Ruth Bader Ginsburg", LocalDate.of(1970,1,1), LocalDate.of(1970,1,1), null, "D")
    val roberts = Justice(2, "John Roberts", LocalDate.of(1970,1,1), LocalDate.of(1970,1,1), null, "R")

    val c1 = Case(102, "People v Mr. Peanut", listOf(),"Mr peanut was murdered and the court needs to decide why",
      "RESOLVED", LocalDate.of(2020,2,2), "February", LocalDate.of(2020, 2,3),
      "http://example.com/opinion.pdf","9-0", "Not justiciable",
      Term(1, "2020-2021", "OT2020"), true, emptyList(), emptyList(), emptyList()
    )

    val c2 = Case(104, "Obergefell v. Hodges", emptyList(), "A state marriage license for a same sex couple should be recognized in all states",
      "REVERSED", LocalDate.of(2015,4,28), "April", LocalDate.of(2015,6,26),
      "5-4", "Right to marry is a fundamental right guaranteed by the Fourteenth Amendment. State laws prohibiting same sex marriage are invalidated",
      "https://www.supremecourt.gov/opinions/14pdf/14-556_3204.pdf", Term(33, "2014-2015", "OT2014"),
      true, emptyList(), emptyList(), emptyList()
    )

    val justiceAgreement = listOf(
      JusticeAgreementResponse(1, mapOf(1L to 1.0f, 2L to 0.5f), mapOf(1L to 1.0f, 2L to 0.5f)),
      JusticeAgreementResponse(2, mapOf(1L to 0.5f, 2L to 1.0f), mapOf(1L to 1.0f, 2L to 0.5f))
    )

    whenever(service.getTermSummary(3)).thenReturn(
        TermSummaryResponse(3, LocalDate.of(2019, 6, 30),
            listOf(
                TermJusticeSummary(rgb, 6, 2, 1, 3, 0, 50, 57),
                TermJusticeSummary(roberts, 5, 0, 0, 2, 1, 55, 57)
            ),
            listOf(
                TermCourtSummary(Court(1, "CA05", "Fifth Circuit Court of Appeals"), 5, 1, 4),
                TermCourtSummary(Court(2, "CA04", "Fourth Circuit Court of Appeals"), 3, 3, 0),
                TermCourtSummary(Court(3, "CA09", "Ninth Circuit Court of Appeals"), 7, 2, 5)
            ),
            justiceAgreement,
            listOf(c1),
            listOf(c2),
            110,
            91
        )
    )

    this.mockMvc.perform(RestDocumentationRequestBuilders.get("/cases/term/{termId}/summary", 3))
        .andExpect(status().isOk)
        .andExpect(jsonPath("termId").value(3))
        .andExpect(jsonPath("justiceSummary[0].majorityAuthor").value(6))
        .andDo(document("case/termsummary",
            preprocessResponse(prettyPrint()),
            pathParameters(parameterWithName("termId").description("Id of the term")),
            responseFields(
                fieldWithPath("termId").description("Id of the Term this summary is for"),
                fieldWithPath("termEndDate").description("Date of the last decision handed down this term"),
                fieldWithPath("justiceSummary[]").description("Summary statistics for each justice who participated in the term"),
                fieldWithPath("courtSummary[]").description("Summary statistics for each court that had a case appealed before SCOTUS this term"),
                fieldWithPath("unanimous[]").description("Cases this term with a unanimous ruling"),
                fieldWithPath("partySplit[]").description("Cases this term split along party lines"),
                fieldWithPath("averageDecisionDays").description("Average number of days between hearing arguments in the case and deciding the case"),
                fieldWithPath("medianDecisionDays").description("Median number of days between hearing arguments in the case and deciding the case"),
            ).andWithPrefix("justiceSummary[].",
                fieldWithPath("justice").description("The justice the following summary information relates to"),
                fieldWithPath("majorityAuthor").description("The number of cases for which this justice wrote the majority opinion"),
                fieldWithPath("concurringAuthor").description("The number of cases for which this justice wrote a concurring opinion"),
                fieldWithPath("concurJudgementAuthor").description("The number of cases for which this justice wrote an opinion concurring in judgement with the opinion of the court"),
                fieldWithPath("dissentAuthor").description("The number of cases for which this justice wrote a dissenting opinion"),
                fieldWithPath("dissentJudgementAuthor").description("The number of cases for which this justice wrote an opinion dissenting in judgment and concurring in part with the opinion of the court"),
                fieldWithPath("casesWithOpinion").description("The number of cases this justice participated in this term"),
                fieldWithPath("casesInMajority").description("The number of cases in which this justice was in the majority this term"),
                fieldWithPath("percentInMajority").description("The percentage describing the how often this justice was in the majority")
            ).andWithPrefix("justiceSummary[].justice.", *JusticeControllerTest.justiceFields
            ).andWithPrefix("courtSummary[].",
                fieldWithPath("court").description("The court the following summary information relates to"),
                fieldWithPath("cases").description("The total number of cases from this court that SCOTUS decided on appeal this term"),
                fieldWithPath("affirmed").description("The number of cases that SCOTUS affirmed"),
                fieldWithPath("reversedRemanded").description("The number of cases that SCOTUS overturned, either by reversing or by remanding for further orders")
            ).andWithPrefix("courtSummary[].court.", *CourtControllerTests.commonCourtFields)
            .andWithPrefix("justiceAgreement[].",
                fieldWithPath("justiceId").description("The ID of the Justice to be compared with the the other justices"),
                subsectionWithPath("opinionAgreementMap").description("A map of the justice ID being compared to the percentage of opinions the two justices both joined, represented by a decimal number"),
                subsectionWithPath("caseAgreementMap").description("A map of the justice ID being compared to the percentage of cases the two justices were aligned on, represented by a decimal number")
            )
            .andWithPrefix("unanimous[].", *caseFields)
            .andWithPrefix("partySplit[]", *caseFields)
        ))
  }

  @Test
  fun testIndexCase() {
    this.mockMvc.perform(put("/cases/100/index"))
      .andExpect(status().isOk)
    verify(searchService).indexCase(100)
  }

  @Test
  fun testIndexAll() {
    this.mockMvc.perform(put("/cases/indexAll"))
      .andExpect(status().isOk)
    verify(searchService).indexAllCases()
  }
}
