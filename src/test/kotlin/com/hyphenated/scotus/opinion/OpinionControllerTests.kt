package com.hyphenated.scotus.opinion

import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest

import com.nhaarman.mockitokotlin2.*
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.restdocs.request.RequestDocumentation.*
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(OpinionController::class)
@AutoConfigureRestDocs
class OpinionControllerTests {

  @MockBean
  private lateinit var service: OpinionService

  @Autowired
  private lateinit var mockMvc: MockMvc

  private val opinionResponseFields = arrayOf(
      fieldWithPath("id").description("Unique Id for the Opinion"),
      fieldWithPath("caseId").description("Unique Id for the Case the opinions is for"),
      fieldWithPath("opinionType").description("Enum that Describes what type of opinion it is"),
      fieldWithPath("summary").description("A summary of the opinion highlighting the key points in some detail"),
      fieldWithPath("justices[]").description("The justices that joined this opinion"),
      fieldWithPath("justices[].justiceId").description("Unique Id for the justice"),
      fieldWithPath("justices[].isAuthor").description("Boolean flag to determine if this justice authored the opinion"),
      fieldWithPath("justices[].justiceName").description("Name of the justice")
  )

  private val obergefellOpinions = arrayOf(
      OpinionResponse(500, 10, OpinionType.MAJORITY, "Democracy and legislation are important. But individuals who are harmed should not have to wait" +
          " for legislative action. A ruling against same sex couples would leave long lasting injuries unjustified under the 14th Amendment. Same sex couples may now exercise the " +
          " fundamental right to marry in all states", listOf(
          OpinionJusticeResponse(50, true, "Anthony Kennedy"),
          OpinionJusticeResponse(51, false, "Ruth Bader Ginsburg"),
          OpinionJusticeResponse(52, false, "Elena Kagan"),
          OpinionJusticeResponse(53, false, "Sonya Sotomayor"),
          OpinionJusticeResponse(54, false, "Steven Breyer"))),
      OpinionResponse(501, 10, OpinionType.DISSENT, "Judges have the power to say what the law is, not what the law should be. The constitution does not " +
          "provide a theory on marriage, so states should be free to define it. Roberts points out that the majority applies the substantive due process test, but that is an extreme remedy. " +
          "Roberts quotes the Justice Curtis' dissent in Dred Scott",
          listOf(OpinionJusticeResponse(55, true, "John Roberts"),
              OpinionJusticeResponse(56, false, "Antonin Scalia"),
              OpinionJusticeResponse(57, false, "Clerence Thomas"))),
      OpinionResponse(502, 10, OpinionType.DISSENT, "Joins Chief Justices Roberts' dissent in full. Today's decree means that the true rulers of 320 Million " +
          "Americans are the 9 justices on the Supreme Court. Public debate over same sex marriage was American democracy at its best until the court stepped in. The Majority decision accuses " +
          "every state of violating the constitution for the last 135 years because of a fundamental right they just discovered.",
          listOf(OpinionJusticeResponse(56, true, "Anton Scalia"),
              OpinionJusticeResponse(57, false, "Clerence Thomas"))),
      OpinionResponse(503, 10, OpinionType.DISSENT, "The majority decision is at odds with the constitution and the principals the nation was build on. " +
          "The Due Process clause should not be used to create substantive rights. Thomas also does not buy that same sex couples are deprived of liberty if their marriage is not recognized.",
          listOf(OpinionJusticeResponse(57, true, "Clerence Thomas"),
              OpinionJusticeResponse(56, false, "Anton Scalia"))),
      OpinionResponse(504, 10, OpinionType.DISSENT, "Constitution does not and should not answer the questions around same sex marriage. Rights protected by " +
          "the due process clause should only be those deeply rooted in the nations history and tradition. No country allowed same sex marriage until 2000. Traditionally marriage is linked " +
          "to procreation. While that understanding may not hold true today, states should be allowed to try to hang onto that tradition. Alito muses about how anyone who disagrees will be " +
          "labeled a bigot and treated as such by employers, government, and schools.",
          listOf(OpinionJusticeResponse(58, true, "Samuel Alito"),
              OpinionJusticeResponse(57, false, "Clerence Thomas"),
              OpinionJusticeResponse(56, false, "Anton Scalia"))))

  @Test
  fun testGetAllOpinions() {
    val nantOpinion = OpinionResponse(600, 20, OpinionType.MAJORITY, "The cost of salaries of legal personnel are not recoverable as part of attorney's fees",
        listOf(OpinionJusticeResponse(60, false, "Brett Kavanaugh"),
            OpinionJusticeResponse(51, false, "Ruth Bader Ginsburg"),
            OpinionJusticeResponse(52, false, "Elena Kagan"),
            OpinionJusticeResponse(53, true, "Sonya Sotomayor"),
            OpinionJusticeResponse(54, false, "Steven Breyer"),
            OpinionJusticeResponse(55, false, "John Roberts"),
            OpinionJusticeResponse(59, false, "Niel Gorsuch"),
            OpinionJusticeResponse(57, false, "Clerence Thomas"),
            OpinionJusticeResponse(58, false, "Samuel Alito")
        ))
    val shularOpinion = OpinionResponse(601, 25, OpinionType.MAJORITY, "State level offense only has to match the description of criminality under ACCA.",
        listOf(OpinionJusticeResponse(60, false, "Brett Kavanaugh"),
            OpinionJusticeResponse(51, true, "Ruth Bader Ginsburg"),
            OpinionJusticeResponse(52, false, "Elena Kagan"),
            OpinionJusticeResponse(53, false, "Sonya Sotomayor"),
            OpinionJusticeResponse(54, false, "Steven Breyer"),
            OpinionJusticeResponse(55, false, "John Roberts"),
            OpinionJusticeResponse(59, false, "Niel Gorsuch"),
            OpinionJusticeResponse(57, false, "Clerence Thomas"),
            OpinionJusticeResponse(58, false, "Samuel Alito")))
    whenever(service.getAll()).thenReturn(listOf(*obergefellOpinions, nantOpinion, shularOpinion))

    this.mockMvc.perform(get("/opinions"))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$").value(hasSize<Any>(7)))
        .andExpect(jsonPath("[0].id").value(500))
        .andExpect(jsonPath("[0].justices").value(hasSize<Any>(5)))
        .andDo(document("opinion/all", responseFields(
            fieldWithPath("[]").description("An array with all of the opinions")
        ).andWithPrefix("[].", *opinionResponseFields)))
  }

  @Test
  fun testGetOpinionsByCase() {
    whenever(service.getByCaseId(10)).thenReturn(obergefellOpinions.toList())
    this.mockMvc.perform(RestDocumentationRequestBuilders.get("/opinions/case/{caseId}", 10))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$").value(hasSize<Any>(5)))
        .andDo(document("opinion/case",
            preprocessResponse(prettyPrint()),
            pathParameters(parameterWithName("caseId").description("Id of the Case to search for")),
            responseFields(fieldWithPath("[]").description("An array with the opinions tied to this case Id"))
                .andWithPrefix("[].", *opinionResponseFields)
        ))
  }

  @Test
  fun testGetOpinionById() {
    whenever(service.getById(500)).thenReturn(obergefellOpinions[0])
    this.mockMvc.perform(RestDocumentationRequestBuilders.get("/opinions/{id}", 500))
        .andExpect(status().isOk)
        .andExpect(jsonPath("id").value(500))
        .andExpect(jsonPath("caseId").value(10))
        .andDo(document("opinion/id",
            preprocessResponse(prettyPrint()),
            pathParameters(parameterWithName("id").description("Id of the opinion")),
            responseFields(*opinionResponseFields)
        ))
  }

  @Test
  fun testNoOpinion(){
    this.mockMvc.perform(get("/opinions/2000"))
        .andExpect(status().isNotFound)
  }

  @Test
  fun testCreateOpinion() {
    val requestString = "{\"caseId\":10,\"opinionType\":\"CONCUR_JUDGEMENT\",\"summary\":\"Agree with the premise, but it's important to come up with a better resoning than this\",\"justices\":[{\"justiceId\":53,\"isAuthor\":true}]}"

    whenever(service.createOpinion(any())).thenAnswer {
      val request = it.arguments[0] as CreateOpinionRequest
      OpinionResponse(220, request.caseId, request.opinionType, request.summary, listOf(OpinionJusticeResponse(request.justices[0].justiceId, request.justices[0].isAuthor, "Sonya Sotomayor")))
    }

    this.mockMvc.perform(post("/opinions")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestString))
        .andExpect(status().isCreated)
        .andExpect(jsonPath("caseId").value(10))
        .andExpect(jsonPath("id").value(220))
        .andDo(document("opinion/admin/create",
            preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            requestFields(
                fieldWithPath("caseId").description("Id of the case to add this opinion to"),
                fieldWithPath("opinionType").description("Enum that Describes what type of opinion it is"),
                fieldWithPath("summary").description("A summary of the opinion highlighting the key points in some detail"),
                fieldWithPath("justices").description("Array with the justices on this opinion"),
                fieldWithPath("justices[].justiceId").description("Unique Id of the justice"),
                fieldWithPath("justices[].isAuthor").optional().type(JsonFieldType.BOOLEAN).description("Flag to indicate if this justice authored the opinion. Exactly one justice must be the author")
            ),
            responseFields(*opinionResponseFields)
        ))
  }

  @Test
  fun testEditOpinionSummary() {
    val newSummary = "Kennedy creates a new fundamental constitutional right to marriage."
    val requestString = "{\"summary\":\"$newSummary\"}"
    whenever(service.editSummary(500, newSummary)).thenReturn(obergefellOpinions[0].copy(summary = newSummary))

    this.mockMvc.perform(RestDocumentationRequestBuilders.put("/opinions/{opinionId}/summary", 500)
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestString))
        .andExpect(status().isOk)
        .andExpect(jsonPath("caseId").value(10))
        .andExpect(jsonPath("justices").value(hasSize<Any>(5)))
        .andDo(document("opinion/admin/summary",
            preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            pathParameters(parameterWithName("opinionId").description("Unique Id of the opinion to be modified")),
            requestFields(fieldWithPath("summary").description("A summary of the opinion highlighting the key points in some detail")),
            responseFields(*opinionResponseFields)
        ))
  }

  @Test
  fun testDeleteOpinion() {
    this.mockMvc.perform(RestDocumentationRequestBuilders.delete("/opinions/{opinionId}", 220))
        .andExpect(status().isNoContent)
        .andDo(document("opinion/admin/delete",
            pathParameters(parameterWithName("opinionId").description("Unique Id of the opinion to delete"))
        ))
    verify(service).deleteOpinion(220)
  }


}