package com.teamvault.budgetvault

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutRes = if (viewType == 1) R.layout.item_user_message else R.layout.item_ai_message
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount() = messages.size

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) 1 else 0
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timestampText: TextView = itemView.findViewById(R.id.timestampText)
        private val messageCard: CardView = itemView.findViewById(R.id.messageCard)

        fun bind(message: ChatMessage) {
            messageText.text = message.content

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            timestampText.text = timeFormat.format(Date(message.timestamp))

            // Add subtle animation
            messageCard.alpha = 0f
            messageCard.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }
}