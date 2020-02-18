package com.hyphenated.scotus.court

import com.hyphenated.scotus.docket.DocketResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.hamcrest.Matchers.*
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Test
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.restdocs.request.RequestDocumentation
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
class CourtControllerTests {

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var service: CourtService

  private val commonCourtFields = arrayOf(
      fieldWithPath("id").description("Unique Id of the appeals court"),
      fieldWithPath("shortName").description("Short name or abbreviation for the court"),
      fieldWithPath("name").description("Full name of the court")
  )

  @Test
  fun testGetAllCourts() {

    val courts = listOf(
        Court(1, "CA01", "First Circuit Court of Appeals"),
        Court(2, "CA05", "Fifth Circuit Court of Appeals"),
        Court(3, "CA09", "Ninth Circuit Court of Appeals"),
        Court(4, "Texas", "Texas Supreme Court"),
        Court(5, "Federal", "Federal Circuit Court"),
        Court(6, "Montana", "Montana Supreme Court"),
        Court(7, "DC Circuit", "Washington DC Circuit Court")
    )

    whenever(service.findAll()).thenReturn(courts)

    this.mockMvc.perform(get("/courts"))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$", hasSize<Any>(7)))
        .andExpect(jsonPath("$[0].id", `is`(1)))
        .andExpect(jsonPath("$[0].shortName", `is`("CA01")))
        .andExpect(jsonPath("$[5].name", `is`("Montana Supreme Court")))
        .andDo(MockMvcRestDocumentation.document("court/all",
            preprocessResponse(prettyPrint()),
            responseFields(
                fieldWithPath("[]").description("A list of all appeals courts")
            ).andWithPrefix("[].", *commonCourtFields)
        ))
  }

  @Test
  fun testGetCourtById() {
    whenever(service.findById(3)).thenReturn(Court(3, "CA09", "Ninth Circuit Court of Appeals"))

    this.mockMvc.perform(RestDocumentationRequestBuilders.get("/courts/{courtId}", 3))
        .andExpect(status().isOk)
        .andExpect(jsonPath("shortName", `is`("CA09")))
        .andDo(MockMvcRestDocumentation.document("court/id",
            preprocessResponse(prettyPrint()),
            pathParameters(parameterWithName("courtId").description("Id of the court")),
            responseFields(*commonCourtFields)
        ))
  }

  @Test
  fun testGetCourtByIdNotFound() {
    this.mockMvc.perform(get("/courts/100"))
        .andExpect(status().isNotFound)
  }
}