package com.hyphenated.scotus.docket

import com.hyphenated.scotus.case.Case
import com.hyphenated.scotus.case.CaseControllerTest
import com.hyphenated.scotus.court.Court
import com.nhaarman.mockitokotlin2.whenever
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.restdocs.operation.preprocess.Preprocessors
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
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
        .andDo(MockMvcRestDocumentation.document("docket/all",
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
        .andDo(MockMvcRestDocumentation.document("docket/case",
            Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
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
        "2014-2015", emptyList(), emptyList())
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
        .andDo(MockMvcRestDocumentation.document("docket/id",
            Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
            pathParameters(parameterWithName("docketId").description("Id of the docket")),
            responseFields(*commonDocketFields)
                .and(fieldWithPath("lowerCourt").description("The lower court ruling being appealed"))
                .andWithPrefix("lowerCourt.",
                    fieldWithPath("id").description("Unique Id of the court"),
                    fieldWithPath("shortName").description("Short name of the court"),
                    fieldWithPath("name").description("Name of the court"))
                .and(fieldWithPath("case").optional().description("Supreme Court case that reviews this docket case"))
                .andWithPrefix("case.", *CaseControllerTest.caseFields)
        ))


  }

  @Test
  fun testGetDocketByIdNoResult() {
    this.mockMvc.perform(get("/dockets/25"))
        .andExpect(status().isNotFound)
  }

}