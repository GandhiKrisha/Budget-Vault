package com.teamvault.budgetvault

import android.animation.ObjectAnimator
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.teamvault.budgetvault.GeminiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatbotActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var welcomeCard: CardView
    private lateinit var typingIndicator: CardView
    private lateinit var statusText: TextView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var vibrator: Vibrator
    private lateinit var geminiService: GeminiService
    private lateinit var clearButton: ImageButton
    private val chatMessages = mutableListOf<ChatMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chatbot)

        setupWindowInsets()
        initializeViews()
        setupEnhancedFeatures()
        setupRecyclerView()
        setupInputHandling()
        setupQuickActions()
        setupBackButton()
        loadChatHistory()

        // Check if API key is configured
        if (!geminiService.isValidApiKey()) {
            updateStatus("⚠️ API key not configured - using offline mode")
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializeViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        welcomeCard = findViewById(R.id.welcomeCard)
        typingIndicator = findViewById(R.id.typingIndicator)
        statusText = findViewById(R.id.statusText)
        clearButton = findViewById(R.id.clearButton)
    }

    private fun setupEnhancedFeatures() {
        sharedPreferences = getSharedPreferences("chatbot_prefs", Context.MODE_PRIVATE)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        geminiService = GeminiService()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(chatMessages)
        chatRecyclerView.adapter = chatAdapter
        chatRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
    }

    private fun setupInputHandling() {
        // Enable/disable send button based on input
        messageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                sendButton.isEnabled = !s.isNullOrBlank()
                sendButton.alpha = if (s.isNullOrBlank()) 0.5f else 1.0f
            }
        })

        // Handle send button click
        sendButton.setOnClickListener {
            sendMessage()
        }

        // Handle enter key in EditText
        messageInput.setOnEditorActionListener { _, _, _ ->
            if (sendButton.isEnabled) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun setupQuickActions() {
        findViewById<CardView>(R.id.quickBudgetTip).setOnClickListener {
            sendQuickMessage("Give me a practical budget tip I can use today")
        }

        findViewById<CardView>(R.id.quickExpenseAnalysis).setOnClickListener {
            sendQuickMessage("How can I analyze and reduce my monthly expenses?")
        }

        // Add test connection on long press of welcome card
        welcomeCard.setOnLongClickListener {
            testApiConnection()
            true
        }
    }

    private fun testApiConnection() {
        addAiResponse("🧪 **Testing API Connection...**\n\nPlease check the Logcat for detailed debug information.")

        lifecycleScope.launch {
            try {
                updateStatus("Testing Gemini API...")
                val testResult = geminiService.testConnection()

                withContext(Dispatchers.Main) {
                    addAiResponse("🔧 **API Test Result:**\n\n$testResult")
                    updateStatus("Test completed")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    addAiResponse("❌ **API Test Exception:**\n\n${e.message}")
                    updateStatus("Test failed")
                }
            }
        }
    }

    private fun setupBackButton() {
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        clearButton.setOnClickListener {
            showClearConfirmation()
        }
    }

    private fun showClearConfirmation() {
        if (chatMessages.isEmpty()) {
            // No messages to clear
            updateStatus("No chat history to clear")
            return
        }

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Clear Chat History")
        builder.setMessage("Are you sure you want to clear all chat messages? This action cannot be undone.")
        builder.setIcon(R.drawable.ic_clear)

        builder.setPositiveButton("Clear All") { _, _ ->
            clearChatHistory()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()

        // Style the dialog buttons (optional)
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_red_dark)
        )
    }

    private fun clearChatHistory() {
        vibrateFeedback()

        // Clear messages
        chatMessages.clear()
        chatAdapter.notifyDataSetChanged()

        // Clear saved history
        sharedPreferences.edit().remove("chat_history").apply()

        // Show welcome card again with animation
        welcomeCard.visibility = View.VISIBLE
        welcomeCard.alpha = 0f
        ObjectAnimator.ofFloat(welcomeCard, "alpha", 0f, 1f).apply {
            duration = 300
            start()
        }

        // Update status
        updateStatus("Ready to help with your finances")

        // Optional: Show confirmation message
        Handler(Looper.getMainLooper()).postDelayed({
            addAiResponse("✨ Chat cleared! Ready to help you achieve your financial goals. What would you like to know? 🚀")
        }, 500)
    }

    private fun updateClearButtonState() {
        clearButton.alpha = if (chatMessages.isEmpty()) 0.3f else 1.0f
        clearButton.isEnabled = chatMessages.isNotEmpty()
    }

    private fun vibrateFeedback() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        } catch (e: Exception) {
            // Handle vibration permission issues gracefully
        }
    }

    private fun sendMessage() {
        vibrateFeedback()
        val message = messageInput.text.toString().trim()
        if (message.isNotEmpty()) {
            addUserMessage(message)
            messageInput.text.clear()
            hideWelcomeCard()
            showTypingIndicator()

            // Use Gemini AI for response
            lifecycleScope.launch {
                try {
                    updateStatus("Getting AI response...")
                    val response = geminiService.getChatResponse(message)

                    withContext(Dispatchers.Main) {
                        hideTypingIndicator()
                        addAiResponse(response)
                        saveChatHistory()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        hideTypingIndicator()
                        addAiResponse("I'm having trouble right now. Please try again in a moment. 🤖")
                        updateStatus("Ready to help")
                    }
                }
            }
        }
    }

    private fun sendQuickMessage(message: String) {
        vibrateFeedback()
        addUserMessage(message)
        hideWelcomeCard()
        showTypingIndicator()

        lifecycleScope.launch {
            try {
                updateStatus("Getting AI response...")
                val response = geminiService.getChatResponse(message)

                withContext(Dispatchers.Main) {
                    hideTypingIndicator()
                    addAiResponse(response)
                    saveChatHistory()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideTypingIndicator()
                    addAiResponse("I'm having trouble right now. Please try again in a moment. 🤖")
                    updateStatus("Ready to help")
                }
            }
        }
    }

    private fun addUserMessage(message: String) {
        chatMessages.add(ChatMessage(message, true, System.currentTimeMillis()))
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        chatRecyclerView.scrollToPosition(chatMessages.size - 1)
        updateClearButtonState() // NEW
    }

    private fun addAiResponse(response: String) {
        chatMessages.add(ChatMessage(response, false, System.currentTimeMillis()))
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        chatRecyclerView.scrollToPosition(chatMessages.size - 1)
        updateStatus("Ready to help")
        updateClearButtonState() // NEW
    }

    private fun hideWelcomeCard() {
        if (welcomeCard.visibility == View.VISIBLE) {
            val fadeOut = ObjectAnimator.ofFloat(welcomeCard, "alpha", 1f, 0f)
            fadeOut.duration = 300
            fadeOut.interpolator = AccelerateDecelerateInterpolator()
            fadeOut.start()

            Handler(Looper.getMainLooper()).postDelayed({
                welcomeCard.visibility = View.GONE
            }, 300)
        }
    }

    private fun showTypingIndicator() {
        typingIndicator.visibility = View.VISIBLE
        typingIndicator.alpha = 0f
        ObjectAnimator.ofFloat(typingIndicator, "alpha", 0f, 1f).apply {
            duration = 200
            start()
        }
        updateStatus("AI is thinking...")
    }

    private fun hideTypingIndicator() {
        ObjectAnimator.ofFloat(typingIndicator, "alpha", 1f, 0f).apply {
            duration = 200
            start()
        }
        Handler(Looper.getMainLooper()).postDelayed({
            typingIndicator.visibility = View.GONE
        }, 200)
    }

    private fun updateStatus(status: String) {
        statusText.text = status
    }

    private fun saveChatHistory() {
        try {
            val gson = Gson()
            val json = gson.toJson(chatMessages)
            sharedPreferences.edit().putString("chat_history", json).apply()
        } catch (e: Exception) {
            // Handle save errors gracefully
        }
    }

    private fun loadChatHistory() {
        try {
            val json = sharedPreferences.getString("chat_history", null)
            if (!json.isNullOrEmpty()) {
                val gson = Gson()
                val type = object : TypeToken<List<ChatMessage>>() {}.type
                val savedMessages: List<ChatMessage> = gson.fromJson(json, type)
                chatMessages.addAll(savedMessages)
                if (chatMessages.isNotEmpty()) {
                    hideWelcomeCard()
                    chatAdapter.notifyDataSetChanged()
                    chatRecyclerView.scrollToPosition(chatMessages.size - 1)
                }
            }
            updateClearButtonState() // NEW
        } catch (e: Exception) {
            // Handle load errors gracefully
            chatMessages.clear()
            updateClearButtonState() // NEW
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        saveChatHistory()
    }

    override fun onPause() {
        super.onPause()
        saveChatHistory()
    }
}

// Data class for chat messages (keep this the same)
data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long
)