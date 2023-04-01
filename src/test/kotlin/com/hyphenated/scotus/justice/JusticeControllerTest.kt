package com.hyphenated.scotus.justice

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.hamcrest.Matchers.*
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(JusticeController::class)
@AutoConfigureRestDocs
@AutoConfigureMockMvc(addFilters = false)
class JusticeControllerTest {

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var service: JusticeService

  companion object {
    val justiceFields = arrayOf(fieldWithPath("id").description("Unique Id for the justice"),
        fieldWithPath("name").description("The name of the Justice"),
        fieldWithPath("dateConfirmed").description("The date the justice was confirmed to the Supreme Court (yyyy-MM-dd)"),
        fieldWithPath("birthday").description("The date the justice was born (yyyy-MM-dd)"),
        fieldWithPath("dateRetired").optional().type(JsonFieldType.STRING).description("The date the justice retired (can be null) (yyyy-MM-dd)"),
        fieldWithPath("party").description("The party in power who appointed the justice")
    )

  }

  private val activeJustices = arrayOf(
      Justice(1, "John Roberts", LocalDate.of(2005, 11, 29), LocalDate.of(1954, 10, 1), null, "R"),
      Justice(2, "Clarence Thomas", LocalDate.of(1991, 10, 23), LocalDate.of(1948, 8, 1), null, "R"),
      Justice(3, "Stephen Breyer", LocalDate.of(1994, 8, 3), LocalDate.of(1938, 4, 1), null, "D"),
      Justice(4, "Ruth Bader Ginsburg", LocalDate.of(1993, 8, 10), LocalDate.of(1943, 2, 1), null, "D"),
      Justice(5, "Sonya Sotomayor", LocalDate.of(2009, 8, 8), LocalDate.of(1954, 1, 1), null, "D"),
      Justice(6, "Elena Kagan", LocalDate.of(2010, 8, 7), LocalDate.of(1960, 5, 1), null, "D"),
      Justice(7, "Samuel Alito", LocalDate.of(2006, 1, 31), LocalDate.of(1950, 10, 1), null, "R"),
      Justice(8, "Neil Gorsuch", LocalDate.of(2017, 10, 10), LocalDate.of(1967, 12, 1), null, "R"),
      Justice(9, "Brett Kavanaugh", LocalDate.of(2018, 10, 6), LocalDate.of(1965, 11, 1), null, "R")
  )

  private val otherJustices = arrayOf(
      Justice(10, "William Rehnquist", LocalDate.of(1986, 9, 17), LocalDate.of(1924, 10, 1), LocalDate.of(2005, 9, 3), "R"),
      Justice(11, "Warren E. Burger", LocalDate.of(1969, 6, 9), LocalDate.of(1907, 10, 1), LocalDate.of(1986, 9, 26), "R"),
      Justice(12, "Earl Warren", LocalDate.of(1954, 3, 1), LocalDate.of(1891, 10, 1), LocalDate.of(1969, 6, 23), "D")
  )

  @Test
  fun testGetAll() {
    whenever(service.findAll()).thenReturn(listOf(*activeJustices, *otherJustices))
    this.mockMvc.perform(get("/justices"))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$", hasSize<Any>(12)))
        .andExpect(jsonPath("$[0].name", `is`("John Roberts")))
        .andExpect(jsonPath("$[2].dateConfirmed", `is`("1994-08-03")))
        .andExpect(jsonPath("$[8].dateRetired", nullValue()))
        .andDo(document("justice/all",
            responseFields(
                fieldWithPath("[]").description("A list of all Supreme Court Justices, past and present")
            ).andWithPrefix("[].", *justiceFields)
        ))
  }

  @Test
  fun testGetActive() {
    whenever(service.findActive()).thenReturn(listOf(*activeJustices))
    this.mockMvc.perform(get("/justices/active"))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$", hasSize<Any>(9)))
        .andDo(document("justice/active",
            preprocessResponse(prettyPrint()),
            responseFields(
                fieldWithPath("[]").description("A list of all currently active Supreme Court Justices")
            ).andWithPrefix("[].", *justiceFields)
        ))
  }

  @Test
  fun testGetById() {
    whenever(service.findById(3)).thenReturn(activeJustices[2])
    this.mockMvc.perform(RestDocumentationRequestBuilders.get("/justices/{justiceId}", 3))
        .andExpect(status().isOk)
        .andExpect(jsonPath("name", `is`("Stephen Breyer")))
        .andDo(document("justice/id",
            preprocessResponse(prettyPrint()),
            pathParameters(parameterWithName("justiceId").description("Id of the Justice")),
            responseFields(*justiceFields)
        ))
  }

  @Test
  fun testSearchByName() {
    whenever(service.findByName("bur")).thenReturn(listOf(activeJustices[3], otherJustices[1]))
    this.mockMvc.perform(RestDocumentationRequestBuilders.get("/justices/name/{justiceName}", "bur"))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$", hasSize<Any>(2)))
        .andExpect(jsonPath("$[0].name", `is`("Ruth Bader Ginsburg")))
        .andDo(document("justice/name",
            preprocessResponse(prettyPrint()),
            pathParameters(parameterWithName("justiceName").description("Name to search for. This string can appear anywhere in the name, case insensitive")),
            responseFields(
                fieldWithPath("[]").description("Justices that match the name search criteria")
            ).andWithPrefix("[].", *justiceFields)
        ))
  }

  @Test
  fun testGetByIdNotFound() {
    this.mockMvc.perform(get("/justices/200"))
        .andExpect(status().isNotFound)
  }

  @Test
  fun testCreateJustice() {
    val justiceString = "{\"name\":\"New Justice\",\"dateConfirmed\":\"2011-04-21\",\"birthday\":\"1969-10-10\",\"party\":\"D\"}"

    whenever(service.createJustice(any())).thenAnswer {
      val justice = it.arguments[0] as Justice
      Justice(33, justice.name, justice.dateConfirmed, justice.birthday, justice.dateRetired, justice.party)
    }

    this.mockMvc.perform(post("/justices")
        .contentType(MediaType.APPLICATION_JSON)
        .content(justiceString))
        .andExpect(status().isCreated)
        .andExpect(jsonPath("id", `is`(33)))
        .andExpect(jsonPath("name", `is`("New Justice")))
        .andDo(document("justice/admin/create",
            preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            requestFields(*justiceFields.copyOfRange(1, justiceFields.size)),
            responseFields(*justiceFields)
        ))
  }

  @Test
  fun testCreateJusticeNoName() {
    val justiceString = "{\"name\":\"\",\"dateConfirmed\":\"2011-04-21\",\"birthday\":\"1969-10-10\",\"party\":\"D\"}"
    this.mockMvc.perform(post("/justices")
        .contentType(MediaType.APPLICATION_JSON)
        .content(justiceString))
        .andExpect(status().`is`(400))
        .andExpect(jsonPath("errorCode", `is`("INVALID_PARAMETER")))
        .andExpect(jsonPath("errorMessage", `is`("Invalid value for 'name': must not be empty")))
  }

  @Test
  fun testCreateJusticeNoDate() {
    val justiceString = "{\"name\":\"New Justice\",\"birthday\":\"1969-10-10\"}"
    this.mockMvc.perform(post("/justices")
        .contentType(MediaType.APPLICATION_JSON)
        .content(justiceString))
        .andExpect(status().`is`(400))
        .andExpect(jsonPath("errorCode", `is`("MISSING_PARAMETER")))
        .andExpect(jsonPath("errorMessage", `is`("Required parameter: dateConfirmed (java.time.LocalDate) is missing or null")))
  }

  @Test
  fun testRetireJustice() {
    whenever(service.retireJustice(4, LocalDate.of(2021, 1, 21))).thenReturn(
        Justice(4, "Ruth Bader Ginsburg", LocalDate.of(1993, 8, 10), LocalDate.of(1943, 2, 1),  LocalDate.of(2021, 1, 21), "D")
    )

    this.mockMvc.perform(RestDocumentationRequestBuilders.put("/justices/{justiceId}/retire", 4)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"retireDate\":\"2021-01-21\"}"))
        .andExpect(status().isOk)
        .andExpect(jsonPath("id", `is`(4)))
        .andExpect(jsonPath("dateRetired", `is`("2021-01-21")))
        .andDo(document("justice/admin/retire",
            preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            pathParameters(parameterWithName("justiceId").description("Id of the justice to modify")),
            requestFields(fieldWithPath("retireDate").description("Date the justice's retirement will take effect")),
            responseFields(*justiceFields)
        ))
  }

  @Test
  fun testRetireJusticeNotFound() {
    whenever(service.retireJustice(any(), any())).thenReturn(null)
    this.mockMvc.perform(put("/justices/222/retire")
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"retireDate\":\"2021-02-21\"}"))
      .andExpect(status().`is`(404))

    verify(service).retireJustice(222, LocalDate.of(2021, 2,21))
  }
}