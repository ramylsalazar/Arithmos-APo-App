package com.example.apo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File

class SnapshotAdapter(
    private var snapshotList: List<File>,
    private val onSelectionChanged: (Int) -> Unit,
    private val onImageClick: (File) -> Unit
) : RecyclerView.Adapter<SnapshotAdapter.SnapshotViewHolder>() {

    var isSelectionMode = false
    val selectedPositions = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SnapshotViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_snapshot_grid, parent, false)
        return SnapshotViewHolder(view)
    }

    override fun onBindViewHolder(holder: SnapshotViewHolder, position: Int) {
        holder.bind(snapshotList[position], position)
    }

    override fun getItemCount(): Int = snapshotList.size

    inner class SnapshotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgPreview: ImageView = itemView.findViewById(R.id.imgSnapshotThumb)
        private val tvName: TextView = itemView.findViewById(R.id.tvSnapshotTileName)
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkSelection)

        fun bind(file: File, position: Int) {
            tvName.text = file.name

            Glide.with(itemView.context)
                .load(file)
                .centerCrop()
                .into(imgPreview)

            // 1. CRITICAL BUG FIX: Remove the listener FIRST to prevent the recycling bug
            checkBox.setOnCheckedChangeListener(null)

            // 2. Set the visibility and checked state
            checkBox.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
            checkBox.isChecked = selectedPositions.contains(position)

            // 3. Re-attach the listener using adapterPosition
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
                if (isSelectionMode) {
                    toggleSelection(position)
                } else {
                    // This launches the enlarged preview
                    onImageClick(file)
                }
            }

            // Still allow long-press to enter selection mode automatically
            itemView.setOnLongClickListener {
                if (!isSelectionMode) {
                    enterSelectionMode(position)
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

        private fun enterSelectionMode(startPosition: Int) {
            isSelectionMode = true
            selectedPositions.add(startPosition)
            notifyDataSetChanged()
            onSelectionChanged(selectedPositions.size)
        }
    }
}
