package com.hyphenated.scotus.tag

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.restdocs.request.RequestDocumentation.*
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(TagController::class)
@AutoConfigureRestDocs
@AutoConfigureMockMvc(addFilters = false)
class TagControllerTests {

  @MockBean
  private lateinit var tagService: TagService

  @Autowired
  private lateinit var mockMvc: MockMvc

  private val tagResponseFields = arrayOf(
    fieldWithPath("id").description("Unique Id for the tag"),
    fieldWithPath("name").description("The name of the Tag"),
    fieldWithPath("description").description("A brief description of the tag")
  )

  private val testTags = listOf(
    Tag(1, "tag1", "The first test tag", emptyList()),
    Tag(2, "tag2", "The second test tag", emptyList()),
    Tag(3, "LGBTQ", "Cases related to Gay, Lesbian, Trans, and Queer rights", emptyList())
  )

  @Test
  fun testGetAllTags() {
    whenever(tagService.getAllTags()).thenReturn(testTags)
    mockMvc.perform(MockMvcRequestBuilders.get("/tags"))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$").value(Matchers.hasSize<Any>(3)))
      .andDo(document("tags/all",
        preprocessResponse(prettyPrint()),
        responseFields(fieldWithPath("[]").description("An array of all available tags"))
          .andWithPrefix("[].", *tagResponseFields)
      ))
  }

  @Test
  fun testGetTagDetails() {
    whenever(tagService.getCasesForTag(3)).thenReturn(testTags[2].toDetailResponse())
    mockMvc.perform(RestDocumentationRequestBuilders.get("/tags/{id}", 3))
      .andExpect(status().isOk)
      .andExpect(jsonPath("id").value(3))
      .andExpect(jsonPath("name").value("LGBTQ"))
      .andDo(document("tags/details",
        preprocessResponse(prettyPrint()),
        pathParameters(
          parameterWithName("id").description("Id of the tag")
        ),
        responseFields(
          *tagResponseFields,
          fieldWithPath("cases").description("List of cases associated with the tag (see case documentation)")
        )
      ))
  }

  @Test
  fun testCreateTag() {
    val request = "{\"name\": \"Judicial Procedure\", \"description\": \"Cases related to judicial procedures or jurisdiction\"}"
    whenever(tagService.createTag(any())).thenAnswer {
      val arg = it.arguments[0] as CreateTagRequest
      Tag(4, arg.name, arg.description, emptyList())
    }

    mockMvc.perform(MockMvcRequestBuilders.post("/tags")
      .contentType(MediaType.APPLICATION_JSON)
      .content(request))
      .andExpect(status().`is`(201))
      .andExpect(jsonPath("id").value(4))
      .andExpect(jsonPath("name").value("Judicial Procedure"))
      .andDo(document("tags/admin/create",
        preprocessRequest(prettyPrint()),
        preprocessResponse(prettyPrint()),
        requestFields(
          fieldWithPath("name").description("Name of the tag to be created"),
          fieldWithPath("description").description("A brief description of the tag")
        ),
        responseFields(*tagResponseFields)
      ))
  }

  @Test
  fun testEditTag() {
    val request = "{\"name\": \"Tag 2\", \"description\": \"Modified description\"}"
    whenever(tagService.editTag(eq(2), any())).thenReturn(
      testTags[1].copy(name = "Tag 2", description = "Modified description")
    )

    mockMvc.perform(RestDocumentationRequestBuilders.patch("/tags/{id}", 2)
      .contentType(MediaType.APPLICATION_JSON)
      .content(request))
      .andExpect(status().isOk)
      .andExpect(jsonPath("name").value("Tag 2"))
      .andDo(document("tags/admin/edit",
        preprocessRequest(prettyPrint()),
        preprocessResponse(prettyPrint()),
        pathParameters(
          parameterWithName("id").description("Id of the tag to edit")
        ),
        requestFields(
          fieldWithPath("name").description("(Optional) Name of the tag to be created"),
          fieldWithPath("description").description("(Optional) A brief description of the tag")
        ),
        responseFields(*tagResponseFields)
      ))
  }

  @Test
  fun testDeleteTag() {
    mockMvc.perform(RestDocumentationRequestBuilders.delete("/tags/{id}", 2))
      .andExpect(status().`is`(204))
      .andDo(document("tags/admin/delete",
        pathParameters(
          parameterWithName("id").description("Id of the tag to delete")
        )
      ))
    verify(tagService).deleteTag(2)
  }
}