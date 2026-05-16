package com.example.nallanudi.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.nallanudi.database.entity.TechnicalTerm
import kotlinx.coroutines.flow.Flow

@Dao
interface TechnicalTermDao {
    
    @Query("SELECT * FROM technical_terms ORDER BY term ASC")
    fun getAllTerms(): Flow<List<TechnicalTerm>>
    
    @Query("SELECT * FROM technical_terms WHERE category = :category ORDER BY term ASC")
    fun getTermsByCategory(category: String): Flow<List<TechnicalTerm>>
    
    @Query("SELECT * FROM technical_terms WHERE term LIKE '%' || :searchQuery || '%' ORDER BY term ASC")
    fun searchTerms(searchQuery: String): Flow<List<TechnicalTerm>>
    
    @Query("SELECT * FROM technical_terms WHERE id = :termId")
    suspend fun getTermById(termId: Int): TechnicalTerm?
    
    @Insert
    suspend fun insertTerm(term: TechnicalTerm)
    
    @Insert
    suspend fun insertAllTerms(terms: List<TechnicalTerm>)
    
    @Update
    suspend fun updateTerm(term: TechnicalTerm)
    
    @Delete
    suspend fun deleteTerm(term: TechnicalTerm)
    
    @Query("SELECT COUNT(*) FROM technical_terms")
    suspend fun getTermCount(): Int
}

// Author: E Thrinadh Chowdary
