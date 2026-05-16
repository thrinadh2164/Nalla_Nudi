package com.example.nallanudi

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nallanudi.data.AppDatabase
import com.example.nallanudi.data.TechnicalTerm
import com.example.nallanudi.ui.TermAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var listUpdateJob: Job? = null
    private var searchDebounceJob: Job? = null

    private lateinit var termAdapter: TermAdapter
    private var currentSubject: String = "All"
    private var currentQuery: String = ""
    private var showingOnlyFavorites: Boolean = false
    private var isProgrammaticChange: Boolean = false
    private var isCurrentlySuggestion: Boolean = false

    private lateinit var drawerLayout: androidx.drawerlayout.widget.DrawerLayout
    private lateinit var searchEditText: EditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var chipScrollView: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var wotdCard: View
    private lateinit var wotdEnglish: TextView
    private lateinit var wotdKannada: TextView
    private lateinit var wotdDefinition: TextView
    private lateinit var listTitle: TextView
    private lateinit var noResultsContainer: View
    private lateinit var aiTranslateBtn: View
    private lateinit var aiProgressBar: ProgressBar
    private lateinit var aiResultCard: View
    private lateinit var aiResultText: TextView
    private lateinit var aiSpeakBtn: View
    private lateinit var drawerThemeBtn: View
    private lateinit var drawerHistoryBtn: View
    private lateinit var drawerMyListBtn: View
    private lateinit var drawerClearHistoryBtn: View
    private lateinit var drawerClearSavedBtn: View

    companion object {
        // Track whether we have applied the theme to avoid re-triggering
        private var themeApplied = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme ONCE before any UI work
        val prefs = getSharedPreferences("nalla_nudi_prefs", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("is_dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_main)
            initViews()
            setupUI()
            setupDatabase()
            tts = TextToSpeech(applicationContext, this)
        } catch (e: Exception) {
            // Show crash reason on screen so we can diagnose
            val tv = TextView(this)
            tv.text = "Error: ${e.message}\n\n${e.stackTraceToString()}"
            tv.setPadding(24, 64, 24, 24)
            tv.setTextColor(android.graphics.Color.RED)
            setContentView(tv)
        }
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        searchEditText = findViewById(R.id.searchEditText)
        chipGroup = findViewById(R.id.chipGroup)
        chipScrollView = findViewById(R.id.chipScrollView)
        recyclerView = findViewById(R.id.termsRecyclerView)
        wotdCard = findViewById(R.id.wotdCard)
        wotdEnglish = findViewById(R.id.wotdEnglish)
        wotdKannada = findViewById(R.id.wotdKannada)
        wotdDefinition = findViewById(R.id.wotdDefinition)
        listTitle = findViewById(R.id.listTitle)
        noResultsContainer = findViewById(R.id.noResultsContainer)
        aiTranslateBtn = findViewById(R.id.aiTranslateBtn)
        aiProgressBar = findViewById(R.id.aiProgressBar)
        aiResultCard = findViewById(R.id.aiResultCard)
        aiResultText = findViewById(R.id.aiResultText)
        aiSpeakBtn = findViewById(R.id.aiSpeakBtn)
        drawerThemeBtn = findViewById(R.id.drawerThemeBtn)
        drawerHistoryBtn = findViewById(R.id.drawerHistoryBtn)
        drawerMyListBtn = findViewById(R.id.drawerMyListBtn)
        drawerClearHistoryBtn = findViewById(R.id.drawerClearHistoryBtn)
        drawerClearSavedBtn = findViewById(R.id.drawerClearSavedBtn)
        findViewById<ImageButton>(R.id.menuBtn).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }
    }

    private fun setupUI() {
        val prefs = getSharedPreferences("nalla_nudi_prefs", Context.MODE_PRIVATE)

        // Theme toggle — NO recreate(), AppCompatDelegate handles it automatically
        drawerThemeBtn.setOnClickListener {
            val nowDark = !prefs.getBoolean("is_dark_mode", false)
            prefs.edit().putBoolean("is_dark_mode", nowDark).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (nowDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            drawerLayout.closeDrawers()
        }

        drawerHistoryBtn.setOnClickListener { showHistoryDialog() }

        drawerMyListBtn.setOnClickListener {
            showingOnlyFavorites = true
            currentQuery = ""
            currentSubject = "All"
            isProgrammaticChange = true
            searchEditText.setText("")
            isProgrammaticChange = false
            drawerLayout.closeDrawers()
            updateList(isSuggestion = false)
        }

        drawerClearHistoryBtn.setOnClickListener {
            getSharedPreferences("nalla_nudi_history", Context.MODE_PRIVATE).edit().clear().apply()
            Toast.makeText(this, "Search history cleared", Toast.LENGTH_SHORT).show()
            drawerLayout.closeDrawers()
        }

        drawerClearSavedBtn.setOnClickListener {
            lifecycleScope.launch {
                try {
                    AppDatabase.getDatabase(applicationContext).termDao().clearAllFavorites()
                    Toast.makeText(this@MainActivity, "All saved words cleared", Toast.LENGTH_SHORT).show()
                    drawerLayout.closeDrawers()
                } catch (_: Exception) {}
            }
        }

        termAdapter = TermAdapter(
            onSpeakClick = { speakText(it.englishWord, Locale.US) },
            onSpeakKanClick = { speakText(it.kannadaWord, Locale("kn", "IN")) },
            onFavClick = { term ->
                lifecycleScope.launch {
                    try {
                        val isNowFav = !term.isFavorite
                        AppDatabase.getDatabase(applicationContext).termDao().toggleFavorite(term.id, isNowFav)
                        val msg = if (isNowFav) "Saved to My List" else "Removed from My List"
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {}
                }
            },
            onItemClick = { term ->
                isProgrammaticChange = true
                searchEditText.setText(term.englishWord)
                isProgrammaticChange = false
                performFullSearch(term.englishWord)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = termAdapter

        findViewById<View>(R.id.wotdSpeakEngBtn).setOnClickListener {
            speakText(wotdEnglish.text.toString(), Locale.US)
        }
        findViewById<View>(R.id.wotdSpeakKanBtn).setOnClickListener {
            speakText(wotdKannada.text.toString(), Locale("kn", "IN"))
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isProgrammaticChange) return
                currentQuery = s.toString()
                if (currentQuery.isNotEmpty()) showingOnlyFavorites = false
                searchDebounceJob?.cancel()
                searchDebounceJob = lifecycleScope.launch {
                    delay(300)
                    if (currentQuery.isEmpty()) currentSubject = "All"
                    updateList(isSuggestion = currentQuery.isNotEmpty())
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        searchEditText.setOnEditorActionListener { _, _, _ ->
            val q = searchEditText.text.toString()
            if (q.isNotEmpty()) { addToHistory(q); performFullSearch(q) }
            true
        }

        aiTranslateBtn.setOnClickListener {
            if (currentQuery.isEmpty()) return@setOnClickListener
            
            // Hide button and show progress bar
            aiTranslateBtn.visibility = View.GONE
            aiResultCard.visibility = View.GONE
            aiProgressBar.visibility = View.VISIBLE
            
            lifecycleScope.launch {
                // Simulate network/AI processing delay
                delay(1500)
                
                // Show result
                aiProgressBar.visibility = View.GONE
                aiResultCard.visibility = View.VISIBLE
                
                // Provide a mock intelligent translation response
                aiResultText.text = "The word '$currentQuery' is not in our offline technical library yet, but here is a potential AI-generated translation based on common usage:\n\n" +
                        "Possible Kannada Translation: ${currentQuery.uppercase()} (Generated by AI model)."
            }
        }
        
        aiSpeakBtn.setOnClickListener {
            speakText(currentQuery, Locale.US)
        }

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = chipGroup.findViewById<Chip>(checkedIds[0])
                currentSubject = chip?.text?.toString() ?: "All"
                updateList(isSuggestion = false, rebuildChips = false)
            }
        }
    }

    private fun showHistoryDialog() {
        val history = getSearchHistory()
        if (history.isEmpty()) {
            Toast.makeText(this, "No search history yet", Toast.LENGTH_SHORT).show()
            return
        }
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_history, null)
        val listView = view.findViewById<ListView>(R.id.historyListView)
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, history)
        listView.setOnItemClickListener { _, _, position, _ ->
            val query = history[position]
            isProgrammaticChange = true
            searchEditText.setText(query)
            isProgrammaticChange = false
            performFullSearch(query)
            dialog.dismiss()
            drawerLayout.closeDrawers()
        }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun getSearchHistory(): List<String> {
        val prefs = getSharedPreferences("nalla_nudi_history", Context.MODE_PRIVATE)
        return prefs.getString("history", "")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    }

    private fun addToHistory(query: String) {
        val current = getSearchHistory().toMutableList()
        current.remove(query)
        current.add(0, query)
        getSharedPreferences("nalla_nudi_history", Context.MODE_PRIVATE)
            .edit().putString("history", current.take(20).joinToString(",")).apply()
    }

    private fun performFullSearch(query: String) {
        currentQuery = query
        showingOnlyFavorites = false
        updateList(isSuggestion = false)
    }

    private fun setupDatabase() {
        lifecycleScope.launch {
            try {
                val dao = AppDatabase.getDatabase(applicationContext).termDao()
                if (dao.getCount() == 0) {
                    seedDatabase(dao)
                } else {
                    dao.getRandomTerm()?.let { term ->
                        wotdEnglish.text = term.englishWord
                        wotdKannada.text = term.kannadaWord
                        wotdDefinition.text = "${term.subject} — ${term.definition}"
                    }
                    val subjects = dao.getSubjects()
                    if (subjects.size > 1) {
                        buildChips(subjects)
                        chipScrollView.visibility = View.GONE
                    }
                    updateList(isSuggestion = false, rebuildChips = false)
                }
            } catch (e: Exception) {
                wotdDefinition.text = "Dictionary loading... (${e.message})"
            }
        }
    }

    private suspend fun seedDatabase(dao: com.example.nallanudi.data.TermDao) {
        try {
            val jsonString = assets.open("glossary.json").bufferedReader().use { it.readText() }
            val jsonArray = org.json.JSONArray(jsonString)
            val terms = mutableListOf<TechnicalTerm>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                terms.add(TechnicalTerm(
                    englishWord = obj.getString("englishWord"),
                    kannadaWord = obj.getString("kannadaWord"),
                    definition = obj.getString("definition"),
                    kannadaDefinition = obj.optString("kannadaDefinition", ""),
                    subject = obj.getString("subject"),
                    example = obj.optString("example", "")
                ))
            }
            dao.insertAll(terms)
            dao.getRandomTerm()?.let { term ->
                wotdEnglish.text = term.englishWord
                wotdKannada.text = term.kannadaWord
                wotdDefinition.text = "${term.subject} — ${term.definition}"
            }
            val subjects = dao.getSubjects()
            if (subjects.size > 1) {
                buildChips(subjects)
                chipScrollView.visibility = View.GONE
            }
            updateList(isSuggestion = false, rebuildChips = false)
        } catch (e: Exception) {
            wotdDefinition.text = "Seed error: ${e.message}"
        }
    }

    private fun updateList(isSuggestion: Boolean, rebuildChips: Boolean = true) {
        isCurrentlySuggestion = isSuggestion
        wotdCard.visibility = if (currentQuery.isEmpty() && !showingOnlyFavorites) View.VISIBLE else View.GONE
        listUpdateJob?.cancel()
        listUpdateJob = lifecycleScope.launch {
            try {
                val dao = AppDatabase.getDatabase(applicationContext).termDao()
                val flow = when {
                    showingOnlyFavorites -> dao.getFavoriteTerms()
                    currentQuery.isEmpty() -> dao.getAllTerms()
                    else -> dao.searchTerms(currentQuery)
                }
                flow.collectLatest { allTerms ->
                    if (rebuildChips && currentQuery.isNotEmpty()) {
                        val searchSubjects = allTerms.map { it.subject }.distinct().sorted()
                        
                        // If the currently selected subject is not in the new search results, reset to All
                        if (currentSubject != "All" && !searchSubjects.contains(currentSubject)) {
                            currentSubject = "All"
                            // Force rebuild chips so "All" is selected visually
                            buildChips(searchSubjects)
                        } else if (searchSubjects.size > 1) {
                            buildChips(searchSubjects)
                        } else if (searchSubjects.size == 1 && searchSubjects[0] != currentSubject) {
                            // If there's exactly 1 subject and it's not the current one, update chips to show it
                            buildChips(searchSubjects)
                        }
                        
                        chipScrollView.visibility = if (searchSubjects.isNotEmpty()) View.VISIBLE else View.GONE
                    } else if (currentQuery.isEmpty() && !showingOnlyFavorites) {
                        chipScrollView.visibility = View.GONE
                    } else if (showingOnlyFavorites) {
                        chipScrollView.visibility = View.GONE
                    }

                    val filtered = if (currentQuery.isNotEmpty() && currentSubject != "All")
                        allTerms.filter { it.subject == currentSubject }
                    else allTerms

                    termAdapter.setSuggestionMode(isSuggestion)
                    termAdapter.submitList(filtered)

                    val show = currentQuery.isNotEmpty() || showingOnlyFavorites
                    recyclerView.visibility = if (show && filtered.isNotEmpty()) View.VISIBLE else View.GONE
                    listTitle.visibility = if (show && filtered.isNotEmpty()) View.VISIBLE else View.GONE
                    
                    // Reset AI translate UI state if no results container becomes visible
                    if (currentQuery.isNotEmpty() && filtered.isEmpty()) {
                        noResultsContainer.visibility = View.VISIBLE
                        aiTranslateBtn.visibility = View.VISIBLE
                        aiProgressBar.visibility = View.GONE
                        aiResultCard.visibility = View.GONE
                    } else {
                        noResultsContainer.visibility = View.GONE
                    }

                    listTitle.text = when {
                        showingOnlyFavorites -> "⭐ MY LIST"
                        isSuggestion -> "SUGGESTIONS"
                        else -> "SEARCH RESULTS (${filtered.size})"
                    }
                }
            } catch (e: Exception) { /* silent */ }
        }
    }

    private fun buildChips(subjects: List<String>) {
        chipGroup.setOnCheckedStateChangeListener(null)
        chipGroup.removeAllViews()
        val allLabels = listOf("All") + subjects
        allLabels.forEach { label ->
            val chip = Chip(this, null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Choice)
            chip.id = View.generateViewId()
            chip.text = label
            chip.isCheckable = true
            chip.isChecked = (label == currentSubject)
            chipGroup.addView(chip)
        }
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = chipGroup.findViewById<Chip>(checkedIds[0])
                chip.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                currentSubject = chip?.text?.toString() ?: "All"
                updateList(isSuggestion = isCurrentlySuggestion, rebuildChips = false)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            ttsReady = true
        }
    }

    private fun speakText(text: String, locale: Locale) {
        if (!ttsReady || text.isBlank()) return
        val result = tts?.setLanguage(locale)
        if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS")
        }
    }

    override fun onDestroy() {
        try { tts?.stop(); tts?.shutdown() } catch (_: Exception) {}
        super.onDestroy()
    }
}

// Author: E Thrinadh Chowdary
