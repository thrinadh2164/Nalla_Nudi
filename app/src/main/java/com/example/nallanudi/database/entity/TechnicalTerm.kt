package com.example.nallanudi.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "technical_terms")
data class TechnicalTerm(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val term: String,
    val englishMeaning: String,
    val kannadaMeaning: String,
    val pronunciation: String,
    val example: String,
    val category: String, // Science, Math, Commerce
    val difficulty: String // Easy, Medium, Hard
)

// Author: E Thrinadh Chowdary
