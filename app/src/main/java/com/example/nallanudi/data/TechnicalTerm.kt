package com.example.nallanudi.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "technical_terms",
    indices = [Index(value = ["englishWord"])]
)
data class TechnicalTerm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val englishWord: String,
    val kannadaWord: String,
    val definition: String,
    val kannadaDefinition: String = "", // New field
    val subject: String,
    val example: String = "",
    val isFavorite: Boolean = false
)

// Author: E Thrinadh Chowdary
