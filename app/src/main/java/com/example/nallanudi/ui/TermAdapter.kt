package com.example.nallanudi.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nallanudi.R
import com.example.nallanudi.data.TechnicalTerm

class TermAdapter(
    private val onSpeakClick: (TechnicalTerm) -> Unit,
    private val onSpeakKanClick: (TechnicalTerm) -> Unit,
    private val onFavClick: (TechnicalTerm) -> Unit,
    private val onItemClick: (TechnicalTerm) -> Unit
) : ListAdapter<TechnicalTerm, TermAdapter.TermViewHolder>(TermDiffCallback()) {

    private var isSuggestionMode: Boolean = false

    fun setSuggestionMode(enabled: Boolean) {
        isSuggestionMode = enabled
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TermViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_term, parent, false)
        return TermViewHolder(view)
    }

    override fun onBindViewHolder(holder: TermViewHolder, position: Int) {
        val term = getItem(position)
        holder.bind(term, onSpeakClick, onSpeakKanClick, onFavClick, onItemClick, isSuggestionMode)
    }

    class TermViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val englishText: TextView = itemView.findViewById(R.id.termEnglish)
        private val kannadaText: TextView = itemView.findViewById(R.id.termKannada)
        private val subjectText: TextView = itemView.findViewById(R.id.termSubject)
        private val definitionText: TextView = itemView.findViewById(R.id.termDefinition)
        private val kannadaDefinitionText: TextView = itemView.findViewById(R.id.termKannadaDefinition)
        private val exampleText: TextView = itemView.findViewById(R.id.termExample)
        private val speakBtn: ImageButton = itemView.findViewById(R.id.speakBtn)
        private val speakKanBtn: ImageButton = itemView.findViewById(R.id.speakKanBtn)
        private val favBtn: ImageButton = itemView.findViewById(R.id.favBtn)

        fun bind(
            term: TechnicalTerm,
            onSpeakClick: (TechnicalTerm) -> Unit,
            onSpeakKanClick: (TechnicalTerm) -> Unit,
            onFavClick: (TechnicalTerm) -> Unit,
            onItemClick: (TechnicalTerm) -> Unit,
            isSuggestion: Boolean
        ) {
            englishText.text = term.englishWord
            kannadaText.text = term.kannadaWord
            
            if (isSuggestion) {
                subjectText.visibility = View.GONE
                definitionText.visibility = View.GONE
                kannadaDefinitionText.visibility = View.GONE
                exampleText.visibility = View.GONE
                speakBtn.visibility = View.GONE
                speakKanBtn.visibility = View.GONE
                favBtn.visibility = View.GONE
            } else {
                subjectText.text = term.subject
                subjectText.visibility = View.VISIBLE
                definitionText.text = term.definition
                definitionText.visibility = View.VISIBLE
                kannadaDefinitionText.text = term.kannadaDefinition
                kannadaDefinitionText.visibility = View.VISIBLE
                exampleText.text = if (term.example.isNotEmpty()) "Example: ${term.example}" else ""
                exampleText.visibility = if (term.example.isNotEmpty()) View.VISIBLE else View.GONE
                speakBtn.visibility = View.VISIBLE
                speakKanBtn.visibility = View.VISIBLE
                favBtn.visibility = View.VISIBLE
                
                speakBtn.setOnClickListener { onSpeakClick(term) }
                speakKanBtn.setOnClickListener { onSpeakKanClick(term) }
                favBtn.setOnClickListener { onFavClick(term) }
                
                favBtn.setImageResource(
                    if (term.isFavorite) android.R.drawable.btn_star_big_on 
                    else android.R.drawable.btn_star_big_off
                )
            }
            
            itemView.setOnClickListener { onItemClick(term) }
        }
    }

    class TermDiffCallback : DiffUtil.ItemCallback<TechnicalTerm>() {
        override fun areItemsTheSame(oldItem: TechnicalTerm, newItem: TechnicalTerm): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TechnicalTerm, newItem: TechnicalTerm): Boolean {
            return oldItem == newItem
        }
    }
}

// Author: E Thrinadh Chowdary
