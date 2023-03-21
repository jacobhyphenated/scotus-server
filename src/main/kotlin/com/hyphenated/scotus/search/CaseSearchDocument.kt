package com.hyphenated.scotus.search

import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import jakarta.persistence.Id

@Document(indexName = "scotus_case")
class CaseSearchDocument (
    @Id
    @org.springframework.data.annotation.Id
    val id: Long,

    @Field(type = FieldType.Text)
    val title: String,

    @Field(type = FieldType.Text)
    val alternateTitles: List<String>,

    @Field(type = FieldType.Text)
    val shortSummary: String,

    @Field(type = FieldType.Text)
    val decision: String?,

    @Field(type = FieldType.Text)
    val docketTitles: List<String>,

    @Field(type = FieldType.Text)
    val opinions: List<String>,

    @Field(type = FieldType.Text)
    val docketSummaries: List<String>,

    @Field(type = FieldType.Keyword)
    val docketNumbers: List<String>
)