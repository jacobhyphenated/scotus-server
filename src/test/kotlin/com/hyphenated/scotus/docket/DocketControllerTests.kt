package com.hyphenated.scotus.docket

import com.hyphenated.scotus.case.Case
import com.hyphenated.scotus.case.CaseControllerTest
import com.hyphenated.scotus.case.term.Term
import com.hyphenated.scotus.court.Court
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import org.hamcrest.Matchers.*
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(DocketController::class)
@AutoConfigureRestDocs
class DocketControllerTests {

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var docketService: DocketService

  private val docketResponseFields = arrayOf(
      fieldWithPath("caseId").optional().description("Unique Id for the case the docket is tied to (can be null)"),
      fieldWithPath("lowerCourtId").description("Unique Id for the appeals court that decided the case"))


  private val commonDocketFields = arrayOf(
      fieldWithPath("id").description("Unique Id for the docket"),
      fieldWithPath("title").description("Docket title. Can be the same as the case title, but sometimes multiple dockets are combined into one case"),
      fieldWithPath("docketNumber").description("A unique number given to each docket submitted for certiorari to the Supreme Court."),
      fieldWithPath("lowerCourtRuling").description("Short summary of how the lower court ruled on the case"),
      fieldWithPath("lowerCourtOverruled").optional().description("Flag for if the lower court overruled by SCOTUS (can be null)"),
      fieldWithPath("status").description("Current status of this docket"))

  private val docketFullSnippet =  responseFields(*commonDocketFields)
      .and(fieldWithPath("lowerCourt").description("The lower court ruling being appealed"))
      .andWithPrefix("lowerCourt.",
          fieldWithPath("id").description("Unique Id of the court"),
          fieldWithPath("shortName").description("Short name of the court"),
          fieldWithPath("name").description("Name of the court"))
      .and(fieldWithPath("case").type(JsonFieldType.OBJECT).optional().description("Supreme Court case that reviews this docket case (can be null)"))
      .andWithPrefix("case.", *CaseControllerTest.caseFields)


  private val obergefellDockets = arrayOf(DocketResponse(1, 43, "Obergefell v. Hodges", "14-556", 6,
  "Something as big as gay marriage should not be decided by a three judge panel", true, "RESOLVED"),
      DocketResponse(2, 43, "Tanco v. Haslam", "14-562", 6,
      "Something as big as gay marriage should not be decided by a three judge panel", true, "RESOLVED"),
      DocketResponse(3, 43, "DeBoer v. Snyder", "14-571", 6,
      "Something as big as gay marriage should not be decided by a three judge panel", true, "RESOLVED"),
      DocketResponse(4, 43, "Bourke v. Beshear", "14-571", 6,
      "Something as big as gay marriage should not be decided by a three judge panel", true, "RESOLVED"))

  @Test
  fun testGetAllDockets() {
    val d5 = DocketResponse(5, 98, "Sharp V. Murphy", "17-1107", 10,
        "Congress did not revoke the native territory, so native territory law applies", null, "CERT_GRANTED")
    val d6 = DocketResponse(6, null, "Patterson v. Walgreen Co.", "18-349", 11,
        "Accomodation for religious practice is reasonable and not a jury question (CA02, CA07, CA09 hold differently)", null, "RELIST")

    whenever(docketService.findAll()).thenReturn(listOf(d5, d6, *obergefellDockets))

    this.mockMvc.perform(get("/dockets"))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$", hasSize<Any>(6)))
        .andExpect(jsonPath("$[0].id", `is`(5)))
        .andExpect(jsonPath("$[0].title", `is`("Sharp V. Murphy")))
        .andExpect(jsonPath("$[5].docketNumber", `is`("14-571")))
        .andDo(document("docket/all",
            responseFields(
                fieldWithPath("[]").description("A list of all dockets")
            ).andWithPrefix("[].", *commonDocketFields, *docketResponseFields)
        ))
  }

  @Test
  fun testGetDocketsByCase() {
    whenever(docketService.findByCaseId(43)).thenReturn(listOf(*obergefellDockets))

    this.mockMvc.perform(RestDocumentationRequestBuilders.get("/dockets/case/{caseId}", 43))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$", hasSize<Any>(4)))
        .andDo(document("docket/case",
            preprocessResponse(prettyPrint()),
            pathParameters(parameterWithName("caseId").description("Id of the case")),
            responseFields(
                fieldWithPath("[]").description("A list of all dockets associated with the case Id")
            ).andWithPrefix("[].", *commonDocketFields, *docketResponseFields)))
  }

  @Test
  fun testGetDocketsByCaseNoResult() {
    this.mockMvc.perform(get("/dockets/case/15"))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$", hasSize<Any>(0)))
  }

  @Test
  fun testGetDocketById() {
    val case = Case(43, "Obergefell v. Hodges", "A state marriage license for a same sex couple should be recognized in all states",
        "RESOLVED", LocalDate.of(2015,4,28), LocalDate.of(2015,6,26), "5-4",
        "Right to marry is a fundamental right guaranteed by the Fourteenth Amendment. State laws prohibiting same sex marriage are invalidated",
        Term(30,"2014-2015", "OT2014"), emptyList(), emptyList())
    val docket = Docket(2, case, "Tanco v. Haslam", "14-562", Court(6, "CA06", "Sixth Circuit Court of Appeals"),
        "Something as big as gay marriage should not be decided by a three judge panel", true, "RESOLVED")

    whenever(docketService.findById(2)).thenReturn(docket)

    this.mockMvc.perform(RestDocumentationRequestBuilders.get("/dockets/{docketId}", 2))
        .andExpect(status().isOk)
        .andExpect(jsonPath("title", `is`("Tanco v. Haslam")))
        .andExpect(jsonPath("lowerCourtOverruled", `is`(true)))
        .andExpect(jsonPath("case.case", `is`("Obergefell v. Hodges")))
        .andExpect(jsonPath("case.opinions").doesNotExist())
        .andExpect(jsonPath("case.dockets").doesNotExist())
        .andExpect(jsonPath("lowerCourt.shortName", `is`("CA06")))
        .andDo(document("docket/id",
            preprocessResponse(prettyPrint()),
            pathParameters(parameterWithName("docketId").description("Id of the docket")),
            docketFullSnippet
        ))
  }

  @Test
  fun testGetDocketByIdNoResult() {
    this.mockMvc.perform(get("/dockets/25"))
        .andExpect(status().isNotFound)
  }

  @Test
  fun testCreateDocket() {
    val request = "{\"title\":\"Moore vs Moar\",\"docketNumber\":\"20-661\",\"lowerCourtId\":\"1\",\"lowerCourtRuling\":\"More's the pity when we want moar from moore\",\"status\":\"REQUEST_CERT\"}"

    whenever(docketService.createDocket(any())).thenAnswer {
      val docketRequest = it.arguments[0] as CreateDocketRequest
      Docket(55, null, docketRequest.title, docketRequest.docketNumber, Court(1, "CA09", "Ninth Circuit"),
           docketRequest.lowerCourtRuling, null, docketRequest.status)
    }

    this.mockMvc.perform(post("/dockets")
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
        .andExpect(status().isCreated)
        .andExpect(jsonPath("id", `is`(55)))
        .andExpect(jsonPath("docketNumber", `is`("20-661")))
        .andExpect(jsonPath("lowerCourt.shortName", `is`("CA09")))
        .andDo(document("docket/admin/create",
            preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            requestFields(
                fieldWithPath("title").description("Docket title or common case reference identifier"),
                fieldWithPath("docketNumber").description("A unique number given to each docket submitted for certiorari to the Supreme Court."),
                fieldWithPath("lowerCourtId").description("The unique Id for the lower court that ruled on the case"),
                fieldWithPath("lowerCourtRuling").description("Short summary of how the lower court ruled on the case"),
                fieldWithPath("status").description("Current status of this docket")),
            docketFullSnippet
        ))
  }

  @Test
  fun testCreateDocketNoTitle() {
    val request = "{\"title\":\"\",\"docketNumber\":\"20-661\",\"lowerCourtId\":\"1\",\"lowerCourtRuling\":\"More's the pity when we want moar from moore\",\"status\":\"REQUEST_CERT\"}"

    this.mockMvc.perform(post("/dockets")
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
        .andExpect(status().isBadRequest)
        .andExpect(jsonPath("errorCode", `is`("INVALID_PARAMETER")))
  }

  @Test
  fun testEditDocket() {
    val request = "{\"status\":\"ARGUMENT_SCHEDULED\",\"caseId\":14}"

    whenever(docketService.editDocket(eq(19), any())).thenReturn(
        Docket(19, Case(14, "Moore vs Moar", "Defining the Moore Doctrine", "ARGUMENT_SCHEDULED",
            null, null, null, null, Term(20, "2019-2020", "OT2019"), emptyList(), emptyList()),
            "Moore v. Moar", "20-661", Court(1, "CA09", "Ninth Circuit"),
            "More's the pity when we want moar from moore", null,"ARGUMENT_SCHEDULED")
    )

    this.mockMvc.perform(RestDocumentationRequestBuilders.patch("/dockets/{docketId}", 19)
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
        .andExpect(status().isOk)
        .andExpect(jsonPath("title", `is`("Moore v. Moar")))
        .andExpect(jsonPath("lowerCourtOverruled", nullValue()))
        .andExpect(jsonPath("case.term.id", `is`(20)))
        .andDo(document("docket/admin/edit",
            preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            pathParameters(parameterWithName("docketId").description("Id of the docket to edit")),
            requestFields(
                fieldWithPath("title").optional().type(JsonFieldType.STRING).description("(optional) Docket title or common case reference identifier"),
                fieldWithPath("docketNumber").optional().type(JsonFieldType.STRING).description("(optional) A unique number given to each docket submitted for certiorari to the Supreme Court."),
                fieldWithPath("lowerCourtRuling").optional().type(JsonFieldType.STRING).description("(optional) Short summary of how the lower court ruled on the case"),
                fieldWithPath("lowerCourtOverruled").optional().type(JsonFieldType.BOOLEAN).description("(optional) Flag if the ruling by the lower court was overrulled by SCOTUS"),
                fieldWithPath("caseId").optional().type(JsonFieldType.NUMBER).description("(optional) Case to link this docket to"),
                fieldWithPath("status").optional().type(JsonFieldType.STRING).description("(optional) Current status of this docket")),
            docketFullSnippet
        ))
  }

}