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
        // Updated ID to match the new layout
        private val tvRecordingDate: TextView = itemView.findViewById(R.id.tvRecordingDate)
        // Added nullable in case we don't use it, but we will add it to the XML below!
        val checkBox: CheckBox? = itemView.findViewById(R.id.checkSelection)

        fun bind(file: File, position: Int) {
            // Format the date to match your design (e.g., "02/09/2026 at 3:38 A.M.")
            val sdf = SimpleDateFormat("MM/dd/yyyy 'at' h:mm a", Locale.getDefault())
            val dateStr = sdf.format(Date(file.lastModified()))

            tvRecordingDate.text = dateStr

            // Handle Checkbox Selection Logic safely
            checkBox?.let { cb ->
                // Remove listener before setting state to avoid side effects
                cb.setOnCheckedChangeListener(null)

                // Show checkbox ONLY if selection mode is active
                cb.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
                cb.isChecked = selectedPositions.contains(position)

                cb.setOnCheckedChangeListener { _, isChecked ->
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
            }

            // Click Listeners
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
                    // Update all items to show their checkboxes
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