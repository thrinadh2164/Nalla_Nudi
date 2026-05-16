package com.example.nallanudi.repository

import com.example.nallanudi.database.dao.TechnicalTermDao
import com.example.nallanudi.database.entity.TechnicalTerm
import kotlinx.coroutines.flow.Flow

class TermRepository(private val termDao: TechnicalTermDao) {
    
    val allTerms: Flow<List<TechnicalTerm>> = termDao.getAllTerms()
    
    fun getTermsByCategory(category: String): Flow<List<TechnicalTerm>> {
        return termDao.getTermsByCategory(category)
    }
    
    fun searchTerms(query: String): Flow<List<TechnicalTerm>> {
        return termDao.searchTerms(query)
    }
    
    suspend fun getTermById(id: Int): TechnicalTerm? {
        return termDao.getTermById(id)
    }
    
    suspend fun insertTerm(term: TechnicalTerm) {
        termDao.insertTerm(term)
    }
    
    suspend fun insertAllTerms(terms: List<TechnicalTerm>) {
        termDao.insertAllTerms(terms)
    }
    
    suspend fun updateTerm(term: TechnicalTerm) {
        termDao.updateTerm(term)
    }
    
    suspend fun deleteTerm(term: TechnicalTerm) {
        termDao.deleteTerm(term)
    }
    
    suspend fun getTermCount(): Int {
        return termDao.getTermCount()
    }
}

// Author: E Thrinadh Chowdary
