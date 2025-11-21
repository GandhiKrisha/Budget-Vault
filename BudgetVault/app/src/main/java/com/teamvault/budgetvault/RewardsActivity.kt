package com.teamvault.budgetvault

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.bottomnavigation.BottomNavigationView

class RewardsActivity : BaseActivity() {

    // Quiz questions and answers
    private val quizQuestions = listOf(
        QuizQuestion(
            "1. What is a budget?",
            listOf(
                "A. A financial reward",
                "B. A spending spree",
                "C. A plan for spending and saving money",
                "D. A tax refund"
            ),
            2 // Index of correct answer (C is at index 2)
        ),
        QuizQuestion(
            "2. Which of the following is NOT typically included in a budget?",
            listOf(
                "A. Income",
                "B. Expenses",
                "C. Credit score",
                "D. Savings goals"
            ),
            2 // Index of correct answer (C)
        ),
        QuizQuestion(
            "3. What is an emergency fund?",
            listOf(
                "A. Money set aside for unexpected expenses",
                "B. A loan for emergencies",
                "C. A type of investment",
                "D. Money for entertainment"
            ),
            0 // Index of correct answer (A)
        ),
        QuizQuestion(
            "4. What is the 50/30/20 budgeting rule?",
            listOf(
                "A. 50% on food, 30% on housing, 20% on transportation",
                "B. 50% on needs, 30% on wants, 20% on savings",
                "C. 50% on debt repayment, 30% on living expenses, 20% on discretionary spending",
                "D. 50% on investments, 30% on savings, 20% on expenses"
            ),
            1 // Index of correct answer (B)
        ),
        QuizQuestion(
            "5. What is a fixed expense?",
            listOf(
                "A. An expense that changes every month",
                "B. A one-time expense",
                "C. An expense that stays the same each month",
                "D. Money spent on repairs"
            ),
            2 // Index of correct answer (C)
        ),
        QuizQuestion(
            "6. Why is it important to track your spending?",
            listOf(
                "A. To help the government collect taxes",
                "B. To know exactly where your money is going",
                "C. To increase your credit score",
                "D. It's not important to track spending"
            ),
            1 // Index of correct answer (B)
        ),
        QuizQuestion(
            "7. What is the benefit of paying yourself first?",
            listOf(
                "A. You earn more money",
                "B. You prioritize saving before spending",
                "C. You avoid paying taxes",
                "D. You get to spend more money"
            ),
            1 // Index of correct answer (B)
        ),
        QuizQuestion(
            "8. What is a zero-based budget?",
            listOf(
                "A. A budget where you spend zero money",
                "B. A budget where your income minus expenses equals zero",
                "C. A budget for people with zero income",
                "D. A budget that only includes necessary expenses"
            ),
            1 // Index of correct answer (B)
        ),
        QuizQuestion(
            "9. Which of these is a good way to save money?",
            listOf(
                "A. Using credit cards for all purchases",
                "B. Never checking your account balance",
                "C. Setting up automatic transfers to savings",
                "D. Buying things only when they're on sale"
            ),
            2 // Index of correct answer (C)
        ),
        QuizQuestion(
            "10. What is the difference between a want and a need?",
            listOf(
                "A. Wants are always more expensive than needs",
                "B. Needs are things required for survival, wants are things desired but not essential",
                "C. Needs are always included in a budget, wants are not",
                "D. There is no difference between wants and needs"
            ),
            1 // Index of correct answer (B)
        )
    )

    private var currentQuestionIndex = 0
    private var score = 0
    private var selectedAnswers = IntArray(10) { -1 } // -1 means no answer selected

    // UI elements
    private lateinit var questionText: TextView
    private lateinit var optionA: RadioButton
    private lateinit var optionB: RadioButton
    private lateinit var optionC: RadioButton
    private lateinit var optionD: RadioButton
    private lateinit var nextButton: Button
    private lateinit var resetButton: Button
    private lateinit var quizContainer: LinearLayout
    private lateinit var quizResultsContainer: LinearLayout
    private lateinit var scoreText: TextView
    private lateinit var scoreComment: TextView
    private lateinit var pointsEarned: TextView
    private lateinit var restartQuizButton: Button
    private lateinit var moneyAnimation: LottieAnimationView

    // Gamification components
    private lateinit var gamificationManager: GamificationManager
    private lateinit var badgesContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var xpProgressText: TextView
    private lateinit var levelText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_rewards)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        initializeViews()

        // Initialize gamification system
        gamificationManager = GamificationManager.getInstance(this)

        // Load and display badges
        loadBadges()

        // Update XP progress info
        updateXPProgress()

        // Initialize back button
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        // Set up the first question
        displayQuestion(currentQuestionIndex)

        // Set up the bottom navigation
        setupBottomNavigation()
    }

    private fun initializeViews() {
        // Quiz question and options
        questionText = findViewById(R.id.questionText)
        optionA = findViewById(R.id.optionA)
        optionB = findViewById(R.id.optionB)
        optionC = findViewById(R.id.optionC)
        optionD = findViewById(R.id.optionD)

        // Quiz navigation
        nextButton = findViewById(R.id.nextButton)
        resetButton = findViewById(R.id.resetButton)

        // Containers
        quizContainer = findViewById(R.id.quizContainer)
        quizResultsContainer = findViewById(R.id.quizResultsContainer)

        // Results views
        scoreText = findViewById(R.id.scoreText)
        scoreComment = findViewById(R.id.scoreComment)
        pointsEarned = findViewById(R.id.pointsEarned)
        restartQuizButton = findViewById(R.id.restartQuizButton)

        // Money animation
        moneyAnimation = findViewById(R.id.moneyAnimation)

        // Add badge container initialization
        badgesContainer = findViewById(R.id.badgesContainer)

        // Progress bar and level text
        progressBar = findViewById(R.id.xpProgressBar)
        xpProgressText = findViewById(R.id.xpProgressText)
        levelText = findViewById(R.id.levelText)

        // Set up quiz buttons
        setupQuizButtons()
    }

    // Add method to update XP progress
    private fun updateXPProgress() {
        val currentXP = gamificationManager.getCurrentXP()
        val currentLevel = gamificationManager.getCurrentLevel()
        val nextLevelXP = gamificationManager.getXPForNextLevel()
        val progress = gamificationManager.getProgressToNextLevel()

        // Update progress bar
        progressBar.progress = progress

        // Update text
        levelText.text = "Level $currentLevel"

        // If max level reached
        if (nextLevelXP == 0) {
            xpProgressText.text = "Max Level Reached!"
        } else {
            xpProgressText.text = "$currentXP XP / $nextLevelXP XP to Level ${currentLevel + 1}"
        }
    }

    // Add this method to load and display badges
    private fun loadBadges() {
        // Clear existing badges
        badgesContainer.removeAllViews()

        // Get unlocked badges
        val unlockedBadges = gamificationManager.getUnlockedBadges()

        // If no badges are unlocked, show a placeholder message
        if (unlockedBadges.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "Complete activities to unlock badges!"
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(0, 48, 0, 48)
                setTextColor(ContextCompat.getColor(context, R.color.grey))
            }
            badgesContainer.addView(emptyView)
            return
        }

        // Create a grid layout for badges (4 per row)
        val badgeRows = (unlockedBadges.size + 3) / 4 // Round up division

        for (rowIndex in 0 until badgeRows) {
            // Create a horizontal linear layout for each row
            val rowLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.HORIZONTAL
                weightSum = 4f
                gravity = Gravity.CENTER
                setPadding(0, 16, 0, 16)
            }

            // Add 4 badges (or fewer for the last row)
            for (i in 0 until 4) {
                val badgeIndex = rowIndex * 4 + i

                if (badgeIndex < unlockedBadges.size) {
                    // We have a badge to display
                    val badge = unlockedBadges[badgeIndex]

                    // Create badge view
                    val badgeView = createBadgeView(badge)
                    rowLayout.addView(badgeView)
                } else {
                    // Add an empty space to maintain the grid
                    val emptySpace = View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    }
                    rowLayout.addView(emptySpace)
                }
            }

            // Add the row to the container
            badgesContainer.addView(rowLayout)
        }
    }

    // Helper method to create a badge view
    private fun createBadgeView(badge: GamificationManager.Badge): View {
        // Create a vertical layout for the badge
        val badgeLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
        }

        // Badge icon
        val badgeIcon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(64, 64)
            setImageResource(badge.iconResourceId)
            contentDescription = badge.name
        }

        // Badge name
        val badgeName = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = badge.name
            textSize = 12f
            gravity = Gravity.CENTER
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(context, R.color.textPrimary))
        }

        // Add views to the layout
        badgeLayout.addView(badgeIcon)
        badgeLayout.addView(badgeName)

        // Make the whole badge clickable to show details
        badgeLayout.setOnClickListener {
            showBadgeDetails(badge)
        }

        return badgeLayout
    }

    // Method to show badge details
    private fun showBadgeDetails(badge: GamificationManager.Badge) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_badge_details)

        // Set up the dialog views
        val badgeIcon = dialog.findViewById<ImageView>(R.id.badgeIcon)
        val badgeTitle = dialog.findViewById<TextView>(R.id.badgeTitle)
        val badgeDescription = dialog.findViewById<TextView>(R.id.badgeDescription)
        val closeButton = dialog.findViewById<Button>(R.id.closeButton)

        // Set the data
        badgeIcon.setImageResource(badge.iconResourceId)
        badgeTitle.text = badge.name
        badgeDescription.text = badge.description

        // Close button
        closeButton.setOnClickListener { dialog.dismiss() }

        // Show the dialog
        dialog.show()
    }

    private fun setupQuizButtons() {
        // Next button click
        nextButton.setOnClickListener {
            // Save the selected answer
            val selectedOption = getSelectedOptionIndex()
            if (selectedOption != -1) {
                selectedAnswers[currentQuestionIndex] = selectedOption

                // Check if correct
                if (selectedOption == quizQuestions[currentQuestionIndex].correctAnswerIndex) {
                    score++
                }

                // Move to next question or show results
                if (currentQuestionIndex < quizQuestions.size - 1) {
                    currentQuestionIndex++
                    displayQuestion(currentQuestionIndex)
                } else {
                    // Show quiz results
                    showQuizResults()
                }
            } else {
                Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show()
            }
        }

        // Reset button click
        resetButton.setOnClickListener {
            // Clear selection for current question
            clearOptions()
        }

        // Restart quiz button click
        restartQuizButton.setOnClickListener {
            // Reset quiz and start over
            resetQuiz()
        }
    }

    private fun displayQuestion(index: Int) {
        // Reset radio buttons
        clearOptions()

        // Display the current question
        val question = quizQuestions[index]
        questionText.text = question.questionText
        optionA.text = question.options[0]
        optionB.text = question.options[1]
        optionC.text = question.options[2]
        optionD.text = question.options[3]

        // If this question was answered before, select that option
        if (selectedAnswers[index] != -1) {
            when (selectedAnswers[index]) {
                0 -> optionA.isChecked = true
                1 -> optionB.isChecked = true
                2 -> optionC.isChecked = true
                3 -> optionD.isChecked = true
            }
        }

        // Update next button text for last question
        if (index == quizQuestions.size - 1) {
            nextButton.text = "FINISH"
        } else {
            nextButton.text = "NEXT"
        }
    }

    private fun clearOptions() {
        optionA.isChecked = false
        optionB.isChecked = false
        optionC.isChecked = false
        optionD.isChecked = false
    }

    private fun getSelectedOptionIndex(): Int {
        return when {
            optionA.isChecked -> 0
            optionB.isChecked -> 1
            optionC.isChecked -> 2
            optionD.isChecked -> 3
            else -> -1
        }
    }

    private fun showQuizResults() {
        // Hide quiz container and show results
        quizContainer.visibility = View.GONE
        quizResultsContainer.visibility = View.VISIBLE

        // Update score text
        scoreText.text = "Your Score: $score/10"

        // Calculate points earned (10 points per correct answer)
        val points = score * 10
        pointsEarned.text = "You earned $points points!"

        // Set comment based on score
        val comment = when {
            score >= 9 -> "Excellent! You're a budgeting expert!"
            score >= 7 -> "Great job! You have good budgeting knowledge."
            score >= 5 -> "Good effort! Keep learning about budgeting."
            else -> "Keep studying! Budgeting skills will help your finances."
        }
        scoreComment.text = comment

        // Start money animation
        moneyAnimation.playAnimation()

        // Track quiz completion for gamification
        val unlockedBadges = gamificationManager.trackAction(
            GamificationManager.ACTION_COMPLETE_QUIZ,
            this,
            progressBar,
            levelText
        )

        // Give extra XP for correct answers
        val extraXP = score * GamificationManager.XP_QUIZ_PER_CORRECT
        if (extraXP > 0) {
            gamificationManager.addXP(extraXP, progressBar, levelText)
        }

        // Update the XP progress display
        updateXPProgress()

        // Update badges display in case new ones were unlocked
        loadBadges()
    }

    private fun resetQuiz() {
        // Reset variables
        currentQuestionIndex = 0
        score = 0
        selectedAnswers = IntArray(10) { -1 }

        // Pause money animation
        moneyAnimation.pauseAnimation()

        // Hide results and show quiz
        quizResultsContainer.visibility = View.GONE
        quizContainer.visibility = View.VISIBLE

        // Display first question
        displayQuestion(currentQuestionIndex)
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Set Dashboard as selected since Rewards isn't in the bottom nav
        bottomNavigation.selectedItemId = R.id.navigation_dashboard

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    val intent = Intent(this,DashboardActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.navigation_budget -> {
                    val intent = Intent(this, BudgetActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.navigation_add -> {
                    // Show the Add options dialog
                    showAddOptionsDialog()
                    true
                }
                R.id.navigation_advices -> {
                    val intent = Intent(this, AdvicesActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.navigation_more -> {
                    val intent = Intent(this, MoreActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    // Method to show the Add options dialog with blurred background
    private fun showAddOptionsDialog() {
        // Create the dialog
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_add_options)

        // Set dialog width to match parent and position at bottom
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(dialog.window?.attributes)
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        layoutParams.gravity = Gravity.BOTTOM
        dialog.window?.attributes = layoutParams

        // Set background to transparent for rounded corners and dim amount for blur effect
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.7f) // This creates the dim/blur effect

        // Set up button click listeners
        dialog.findViewById<Button>(R.id.addIncomeButton).setOnClickListener {
            val intent = Intent(this, AddIncomeActivity::class.java)
            startActivity(intent)
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.addExpenseButton).setOnClickListener {
            val intent = Intent(this, AddExpenseActivity::class.java)
            startActivity(intent)
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            // Just dismiss the dialog
            dialog.dismiss()
        }

        dialog.show()
    }

    // Override onResume to refresh gamification data
    override fun onResume() {
        super.onResume()

        // Update XP progress
        updateXPProgress()

        // Reload badges
        loadBadges()
    }

    // Data class for quiz questions
    data class QuizQuestion(
        val questionText: String,
        val options: List<String>,
        val correctAnswerIndex: Int
    )
}