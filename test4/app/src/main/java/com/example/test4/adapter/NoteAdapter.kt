package com.example.test4.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.test4.R
import com.example.test4.data.Note
import com.example.test4.databinding.ItemNoteBinding
import java.io.File

class NoteAdapter(
    private val onItemClick: (Note) -> Unit
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(private val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root) {
        
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(note: Note) {
            binding.apply {
                tvTitle.text = note.title
                tvContent.text = note.content
                tvDate.text = note.getFormattedDate()
                chipCategory.text = note.category

                if (!note.imagePath.isNullOrEmpty() && File(note.imagePath).exists()) {
                    ivNoteImage.visibility = View.VISIBLE
                    Glide.with(itemView.context)
                        .load(note.imagePath)
                        .centerCrop()
                        .placeholder(R.drawable.ic_image)
                        .error(R.drawable.ic_image)
                        .into(ivNoteImage)
                } else {
                    ivNoteImage.visibility = View.GONE
                }

                chipCategory.setChipBackgroundColorResource(getCategoryColor(note.category))
            }
        }

        private fun getCategoryColor(category: String): Int {
            return when (category) {
                "工作" -> R.color.primary_color
                "生活" -> R.color.success_color
                "学习" -> R.color.warning_color
                "重要" -> R.color.error_color
                else -> R.color.text_secondary
            }
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
} 