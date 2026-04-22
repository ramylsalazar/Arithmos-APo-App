package com.example.apo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingAdapter(
    private val recordingList: List<File>,
    private val onSelectionChanged: (Int) -> Unit,
    private val onItemClick: (File) -> Unit
) : RecyclerView.Adapter<RecordingAdapter.RecordingViewHolder>() {

    var isSelectionMode = false
    val selectedPositions = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recording, parent, false)
        return RecordingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        holder.bind(recordingList[position], position)
    }

    override fun getItemCount(): Int = recordingList.size

    inner class RecordingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        private val tvFileDetails: TextView = itemView.findViewById(R.id.tvFileDetails)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkSelection)

        fun bind(file: File, position: Int) {
            tvFileName.text = file.name

            val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
            val dateStr = sdf.format(Date(file.lastModified()))
            val sizeStr = "${String.format("%.1f", file.length() / 1024.0 / 1024.0)} MB"

            tvFileDetails.text = "$dateStr • $sizeStr"

            // Remove listener before setting state to avoid side effects during recycling
            checkBox.setOnCheckedChangeListener(null)
            
            // Show checkbox ONLY if selection mode is active
            checkBox.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
            checkBox.isChecked = selectedPositions.contains(position)

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                val currentPos = adapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    if (isChecked) {
                        selectedPositions.add(currentPos)
                    } else {
                        selectedPositions.remove(currentPos)
                    }
                    onSelectionChanged(selectedPositions.size)
                }
            }

            itemView.setOnClickListener {
                val currentPos = adapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    if (isSelectionMode) {
                        toggleSelection(currentPos)
                    } else {
                        onItemClick(file)
                    }
                }
            }

            itemView.setOnLongClickListener {
                val currentPos = adapterPosition
                if (currentPos != RecyclerView.NO_POSITION && !isSelectionMode) {
                    isSelectionMode = true
                    toggleSelection(currentPos)
                    notifyDataSetChanged()
                }
                true
            }
        }

        private fun toggleSelection(position: Int) {
            if (selectedPositions.contains(position)) {
                selectedPositions.remove(position)
            } else {
                selectedPositions.add(position)
            }
            notifyItemChanged(position)
            onSelectionChanged(selectedPositions.size)
        }
    }
}