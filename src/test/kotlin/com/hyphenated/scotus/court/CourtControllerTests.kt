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
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
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
        .andDo(document("court/all",
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
        .andDo(document("court/id",
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

  @Test
  fun testCreateCourt() {
    val request = "{\"shortName\":\"CA02\",\"name\":\"Second Circuit Court of Appeals\"}"
    whenever(service.createCourt(any())).thenAnswer {
      val arg = it.arguments[0] as Court
      Court(20, arg.shortName, arg.name)
    }

    this.mockMvc.perform(post("/courts")
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
        .andExpect(status().isCreated)
        .andExpect(jsonPath("id").value(20))
        .andExpect(jsonPath("shortName").value("CA02"))
        .andDo(document("court/admin/create",
            preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            requestFields(*commonCourtFields.copyOfRange(1, commonCourtFields.size)),
            responseFields(*commonCourtFields)
        ))
  }

  @Test
  fun testDeleteCourt() {
    this.mockMvc.perform(RestDocumentationRequestBuilders.delete("/courts/{courtId}", 5))
        .andExpect(status().isNoContent)
        .andDo(document("court/admin/delete",
            pathParameters(parameterWithName("courtId").description("Id of the court to delete"))
        ))
  }

  @Test
  fun testEditCourt() {
    val request = "{\"shortName\":\"CA02\",\"name\":\"Second Circuit Appeals Court\"}"
    whenever(service.edit(eq(20), any())).thenAnswer {
      val edit = it.arguments[1] as Court
      Court(20, edit.shortName, edit.name)
    }

    this.mockMvc.perform(RestDocumentationRequestBuilders.put("/courts/{courtId}", 20)
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
        .andExpect(status().isOk)
        .andExpect(jsonPath("name").value("Second Circuit Appeals Court"))
        .andExpect(jsonPath("id").value(20))
        .andDo(document("court/admin/edit",
            preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            pathParameters(parameterWithName("courtId").description("Id of the court to edit")),
            requestFields(*commonCourtFields.copyOfRange(1, commonCourtFields.size)),
            responseFields(*commonCourtFields)
        ))
  }
}