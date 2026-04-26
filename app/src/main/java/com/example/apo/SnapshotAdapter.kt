package com.example.apo

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class SnapshotAdapter(
    private val snapshotList: List<File>,
    private val onItemClick: (File) -> Unit // This was missing!
) : RecyclerView.Adapter<SnapshotAdapter.SnapshotViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SnapshotViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_snapshot, parent, false)
        return SnapshotViewHolder(view)
    }

    override fun onBindViewHolder(holder: SnapshotViewHolder, position: Int) {
        val file = snapshotList[position]

        // Bulletproof image decoding
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        if (bitmap != null) {
            holder.imgSnapshot.setImageBitmap(bitmap)
        }

        // Set the click listener to open the preview
        holder.itemView.setOnClickListener {
            onItemClick(file)
        }
    }

    override fun getItemCount(): Int = snapshotList.size

    inner class SnapshotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // This must match the ID in your item_snapshot.xml
        val imgSnapshot: ImageView = itemView.findViewById(R.id.imgSnapshot)
    }
}