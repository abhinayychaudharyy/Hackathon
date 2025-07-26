package com.shashank.paymentappui

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.shashank.paymentappui.Receipt
import com.shashank.paymentappui.R

class ReceiptsAdapter(
    private val receipts: List<Receipt>,
    private val onItemClick: (Receipt) -> Unit,
    private val onDeleteClick: (Receipt) -> Unit
) : RecyclerView.Adapter<ReceiptsAdapter.ReceiptViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.receipt_row, parent, false)
        return ReceiptViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReceiptViewHolder, position: Int) {
        val receipt = receipts[position]
        holder.receiptText.text = receipt.extractedText
        holder.receiptTotal.text = "Total: ₹%.2f".format(receipt.total)
        holder.receiptDate.text = receipt.date
        holder.receiptThumbnail.setImageURI(receipt.photoUri)
        holder.itemView.setOnClickListener { onItemClick(receipt) }
        holder.deleteButton.setOnClickListener { onDeleteClick(receipt) }
    }

    override fun getItemCount(): Int = receipts.size

    class ReceiptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiptThumbnail: ImageView = itemView.findViewById(R.id.receiptThumbnail)
        val receiptText: TextView = itemView.findViewById(R.id.receiptText)
        val receiptTotal: TextView = itemView.findViewById(R.id.receiptTotal)
        val receiptDate: TextView = itemView.findViewById(R.id.receiptDate)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
    }
} 