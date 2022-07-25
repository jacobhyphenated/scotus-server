package com.hyphenated.scotus.term

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.restdocs.operation.preprocess.Preprocessors
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation
import org.springframework.restdocs.request.RequestDocumentation
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@WebMvcTest(TermController::class)
@AutoConfigureRestDocs
@AutoConfigureMockMvc(addFilters = false)
class TermControllerTests {

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var service: TermService

  private val termFields = arrayOf(
      PayloadDocumentation.fieldWithPath("id").description("Id of the term"),
      PayloadDocumentation.fieldWithPath("name").description("Term defined as a year range"),
      PayloadDocumentation.fieldWithPath("otName").description("October term (\"OT\") notation"),
      PayloadDocumentation.fieldWithPath("inactive").description("Inactive terms are only partial terms. They do not contain all cases from that term")
  )

  @Test
  fun testGetAllTerms() {
    val terms = listOf(
        Term(1, "2014-2015", "OT2014"),
        Term(2, "2015-2016", "OT2015"),
        Term(3, "2016-2017", "OT2016")
    )
    whenever(service.getAllTerms()).thenReturn(terms)

    this.mockMvc.perform(MockMvcRequestBuilders.get("/terms"))
        .andExpect(MockMvcResultMatchers.jsonPath("$").value(Matchers.hasSize<Any>(3)))
        .andExpect(MockMvcResultMatchers.jsonPath("[0].otName").value("OT2014"))
        .andDo(MockMvcRestDocumentation.document("term/allterm",
            Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
            PayloadDocumentation.responseFields(
                PayloadDocumentation.fieldWithPath("[]").description("List of terms")
            ).andWithPrefix("[].", *termFields)
        ))
  }

  @Test
  fun testCreateTerm() {
    whenever(service.createTerm("2020-2021", "OT2020", true))
        .thenReturn(Term(5, "2020-2021", "OT2020", true))

    val request = "{\"name\":\"2020-2021\",\"otName\":\"OT2020\", \"inactive\":true}"

    this.mockMvc.perform(MockMvcRequestBuilders.post("/terms")
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
        .andExpect(MockMvcResultMatchers.status().isCreated)
        .andExpect(MockMvcResultMatchers.jsonPath("otName").value("OT2020"))
        .andDo(MockMvcRestDocumentation.document("term/admin/create",
            Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
            Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
            PayloadDocumentation.requestFields(
                *termFields.copyOfRange(1, termFields.size)
            ),
            PayloadDocumentation.responseFields(*termFields)
        ))
  }

  @Test
  fun testEditTerm() {
    whenever(service.editTerm(eq(2), any()))
        .thenReturn(Term(2, "2021-2022", "OT2021", false))
    val request = "{\"otName\": \"OT2021\"}"

    this.mockMvc.perform(RestDocumentationRequestBuilders.patch("/terms/{termId}", 2)
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
        .andExpect(MockMvcResultMatchers.status().isOk)
        .andExpect(MockMvcResultMatchers.jsonPath("otName").value("OT2021"))
        .andDo(MockMvcRestDocumentation.document("term/admin/edit",
            Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
            Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
            RequestDocumentation.pathParameters(RequestDocumentation.parameterWithName("termId").description("Id of the term to modify")),
            PayloadDocumentation.requestFields(
                PayloadDocumentation.fieldWithPath("name").type(JsonFieldType.STRING).optional().description("(optional) Term defined as a year range"),
                PayloadDocumentation.fieldWithPath("otName").type(JsonFieldType.STRING).optional().description("(optional) October term (\"OT\") notation"),
                PayloadDocumentation.fieldWithPath("inactive").type(JsonFieldType.BOOLEAN).optional().description("(optional) mark term as inactive")
            ),
            PayloadDocumentation.responseFields(*termFields)
        ))
  }
}