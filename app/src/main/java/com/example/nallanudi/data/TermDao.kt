package com.example.nallanudi.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TermDao {
    @Query("SELECT * FROM technical_terms ORDER BY englishWord ASC")
    fun getAllTerms(): Flow<List<TechnicalTerm>>

    @Query("SELECT * FROM technical_terms WHERE LOWER(englishWord) LIKE '%' || LOWER(:query) || '%' OR kannadaWord LIKE '%' || :query || '%'")
    fun searchTerms(query: String): Flow<List<TechnicalTerm>>

    @Query("SELECT * FROM technical_terms WHERE subject = :subject")
    fun getTermsBySubject(subject: String): Flow<List<TechnicalTerm>>

    @Query("SELECT * FROM technical_terms WHERE isFavorite = 1")
    fun getFavoriteTerms(): Flow<List<TechnicalTerm>>

    @Query("SELECT DISTINCT subject FROM technical_terms ORDER BY subject ASC")
    suspend fun getSubjects(): List<String>

    @Query("UPDATE technical_terms SET isFavorite = 0")
    suspend fun clearAllFavorites()

    @Query("UPDATE technical_terms SET isFavorite = :favorite WHERE id = :termId")
    suspend fun toggleFavorite(termId: Int, favorite: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(terms: List<TechnicalTerm>)

    @Update
    suspend fun updateTerm(term: TechnicalTerm)

    @Query("SELECT COUNT(*) FROM technical_terms")
    suspend fun getCount(): Int

    @Query("SELECT * FROM technical_terms ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomTerm(): TechnicalTerm?
}

// Author: E Thrinadh Chowdary
