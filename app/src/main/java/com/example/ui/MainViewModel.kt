package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiApi
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application, private val repository: AppRepository) : AndroidViewModel(application) {

    private val TAG = "MainViewModel"

    // --- State Observables from DB ---
    val expenses = repository.allExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val tasks = repository.allTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notes = repository.allNotes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val reminders = repository.allReminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val chatLogs = repository.allChatLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Unified AppSettings state
    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    // --- Active Chat UI State ---
    var currentPrompt = MutableStateFlow("")
    var isRecording = MutableStateFlow(false)
    var isAiGenerating = MutableStateFlow(false)
    
    // Temporary response text displayed while streaming
    var activeStreamingResponse = MutableStateFlow<String?>(null)

    init {
        // Load settings from DB with defaults
        viewModelScope.launch {
            repository.appSettings.collect { settings ->
                if (settings != null) {
                    _appSettings.value = settings
                } else {
                    // Populate defaults
                    val defaultSettings = AppSettings()
                    repository.saveSettings(defaultSettings)
                    _appSettings.value = defaultSettings
                }
            }
        }
    }

    // --- Expenses Actions ---
    fun addExpense(title: String, amount: Double, category: String, date: String, note: String) {
        viewModelScope.launch {
            repository.insertExpense(Expense(title = title, amount = amount, category = category, date = date, note = note))
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun deleteExpenseById(id: Int) {
        viewModelScope.launch {
            repository.deleteExpenseById(id)
        }
    }

    fun clearAllExpenses() {
        viewModelScope.launch {
            repository.deleteAllExpenses()
        }
    }

    // --- Tasks Actions ---
    fun addTask(title: String, priority: String, dueDate: String, tag: String = "", isCompleted: Boolean = false) {
        viewModelScope.launch {
            repository.insertTask(Task(title = title, priority = priority, dueDate = dueDate, tag = tag, isCompleted = isCompleted))
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTaskById(id: Int) {
        viewModelScope.launch {
            repository.deleteTaskById(id)
        }
    }

    // --- Notes Actions ---
    fun addNote(title: String, content: String, date: String, colorHex: String = "#FF3B30") {
        viewModelScope.launch {
            repository.insertNote(Note(title = title, content = content, date = date, colorHex = colorHex))
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun deleteNoteById(id: Int) {
        viewModelScope.launch {
            repository.deleteNoteById(id)
        }
    }

    // --- Reminders Actions ---
    fun addReminder(title: String, date: String, time: String, repeatType: String) {
        viewModelScope.launch {
            repository.insertReminder(Reminder(title = title, date = date, time = time, repeatType = repeatType))
        }
    }

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.updateReminder(reminder)
        }
    }

    fun deleteReminderById(id: Int) {
        viewModelScope.launch {
            repository.deleteReminderById(id)
        }
    }

    // --- Chat Session Actions ---
    fun saveSettings(settings: AppSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
            _appSettings.value = settings
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatLogs()
        }
    }

    /**
     * Executes AI Generation & Context Injection.
     * Integrates direct streaming and parses special platform actions.
     */
    fun sendChatMessage(text: String, overrideUserAndDoNotSaveInput: Boolean = false) {
        if (text.trim().isEmpty()) return

        viewModelScope.launch {
            if (!overrideUserAndDoNotSaveInput) {
                // 1. Add User Input to Room Logs
                repository.insertChatLog(ChatLog(sender = "user", text = text))
                currentPrompt.value = ""
            }

            isAiGenerating.value = true
            activeStreamingResponse.value = ""

            // Assemble Full context
            val contextJson = assembleContextJson()
            val prompt = text
            val systemPrompt = makeContextualSystemPrompt(contextJson)

            val currentSet = appSettings.value
            val isStreaming = currentSet.streamingEnabled

            if (isStreaming) {
                // Real REST API content streaming
                var fullResponseBuffer = ""
                GeminiApi.streamGenerateText(
                    prompt = prompt,
                    systemInstruction = systemPrompt,
                    apiKey = currentSet.geminiApiKey,
                    model = "gemini-3.5-flash", // Base task default
                    maxTokens = currentSet.maxTokens
                ).collect { chunk ->
                    fullResponseBuffer += chunk
                    activeStreamingResponse.value = fullResponseBuffer
                }

                // Stream ended, process any actions and save to DB
                val finalAnswer = activeStreamingResponse.value ?: ""
                activeStreamingResponse.value = null
                
                // Parse actions in background, strip tags from display or execute
                val cleanAnswer = handleAgentActions(finalAnswer)
                repository.insertChatLog(ChatLog(sender = "ai", text = cleanAnswer))
            } else {
                // Standard single response
                val answer = GeminiApi.generateText(
                    prompt = prompt,
                    systemInstruction = systemPrompt,
                    apiKey = currentSet.geminiApiKey,
                    model = "gemini-3.5-flash",
                    maxTokens = currentSet.maxTokens
                )
                val cleanAnswer = handleAgentActions(answer)
                repository.insertChatLog(ChatLog(sender = "ai", text = cleanAnswer))
            }

            isAiGenerating.value = false
        }
    }

    /**
     * Assembles all active lists from StateFlows securely into a compact JSON context.
     */
    private fun assembleContextJson(): String {
        return try {
            val root = JSONObject()
            
            // Expenses
            val expensesArr = JSONArray()
            expenses.value.forEach {
                val exp = JSONObject()
                exp.put("id", it.id)
                exp.put("title", it.title)
                exp.put("amount", it.amount)
                exp.put("category", it.category)
                exp.put("date", it.date)
                exp.put("note", it.note)
                expensesArr.put(exp)
            }
            root.put("expenses", expensesArr)

            // Tasks
            val tasksArr = JSONArray()
            tasks.value.forEach {
                val t = JSONObject()
                t.put("id", it.id)
                t.put("title", it.title)
                t.put("priority", it.priority)
                t.put("dueDate", it.dueDate)
                t.put("tag", it.tag)
                t.put("isCompleted", it.isCompleted)
                tasksArr.put(t)
            }
            root.put("tasks", tasksArr)

            // Notes
            val notesArr = JSONArray()
            notes.value.forEach {
                val n = JSONObject()
                n.put("id", it.id)
                n.put("title", it.title)
                n.put("content", it.content)
                n.put("date", it.date)
                notesArr.put(n)
            }
            root.put("notes", notesArr)

            // Reminders
            val remindArr = JSONArray()
            reminders.value.forEach {
                val r = JSONObject()
                r.put("id", it.id)
                r.put("title", it.title)
                r.put("date", it.date)
                r.put("time", it.time)
                r.put("repeatType", it.repeatType)
                r.put("isEnabled", it.isEnabled)
                remindArr.put(r)
            }
            root.put("reminders", remindArr)

            // Memory/Meta
            root.put("userName", appSettings.value.userName)
            root.put("monthlyBudget", appSettings.value.monthlyBudget)
            root.put("currencySymbol", appSettings.value.currencySymbol)
            root.put("userMemory", appSettings.value.userMemory)
            root.put("currentDate", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))

            root.toString(2)
        } catch (e: Exception) {
            "Empty or Error assembling local context: ${e.message}"
        }
    }

    /**
     * Injects the application's entire database state as contextual parameters in the system prompt.
     */
    private fun makeContextualSystemPrompt(contextJson: String): String {
        val settings = appSettings.value
        return """
${settings.customSystemPrompt}

You are ruthless, efficient, witty, and slightly sarcastic. You have direct read/write access to the user's local dashboard database.
Keep your text concise, smart, and beautifully styled in Markdown.

Here is the current state of the User's Life OS database in real-time JSON format:
$contextJson

Rules:
1. Speak in English or mixed English/Bengali (Banglish) if requested. Always use the specified currency symbol: ${settings.currencySymbol}.
2. Since you have local capabilities, you can execute database actions (create, delete, toggle) on behalf of the user.
To run an action, you MUST write the special instruction tag on its own separate line in your text response. You can write multiple action lines if needed.
Use the EXACT pipe-separated formatting below:

- To add a new expense:
${'$'}${'$'}ACTION:add_expense|[title]|[amount]|[category]|[date]|[note]${'$'}${'$'}
Category should be one of (Food, Transport, Shopping, Bills, Health, Fun, Other). Date format: YYYY-MM-DD.

- To delete an expense:
${'$'}${'$'}ACTION:delete_expense|[expenseId]${'$'}${'$'}

- To add a task / todo:
${'$'}${'$'}ACTION:add_task|[title]|[priority]|[dueDate]|[tag]${'$'}${'$'}
Priority must be HIGH, MEDIUM, or LOW. Date format: YYYY-MM-DD.

- To toggle/complete a task:
${'$'}${'$'}ACTION:mark_task_completed|[taskId]${'$'}${'$'}

- To add a smart note:
${'$'}${'$'}ACTION:add_note|[title]|[content]${'$'}${'$'}

- To schedule an in-app reminder:
${'$'}${'$'}ACTION:add_reminder|[title]|[date]|[time]|[repeatType]${'$'}${'$'}
repeatType must be Once, Daily, Weekly, or Monthly.

Make sure to strip any action tags from your conversational conversational tone, or explain what you are doing. Remember, when you execute an action, it updates the database immediately!
        """.trimIndent()
    }

    /**
     * Parses the response from Gemini for Action Tags (`$$ACTION:cmd|...$$`),
     * executes the queries on the repository, and returns a clean conversation text
     * with the instructions stripped out.
     */
    private suspend fun handleAgentActions(rawText: String): String {
        var cleanText = rawText
        val lines = rawText.split("\n")
        
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("\$\$ACTION:") && trimmedLine.endsWith("\$\$")) {
                // Extract inner command content
                val content = trimmedLine.substring(9, trimmedLine.length - 2) // Strips $$ACTION: and $$
                val parts = content.split("|")
                if (parts.isNotEmpty()) {
                    val cmd = parts[0]
                    try {
                        withContext(Dispatchers.Main) {
                            when (cmd) {
                                "add_expense" -> {
                                    if (parts.size >= 4) {
                                        val title = parts[1]
                                        val amount = parts[2].toDoubleOrNull() ?: 0.0
                                        val cat = parts[3]
                                        val date = parts.getOrNull(4)?.ifEmpty { "" } ?: getTodayDate()
                                        val note = parts.getOrNull(5) ?: "Added via AI Chat"
                                        addExpense(title, amount, cat, date, note)
                                    }
                                }
                                "delete_expense" -> {
                                    val id = parts.getOrNull(1)?.toIntOrNull()
                                    if (id != null) {
                                        deleteExpenseById(id)
                                    }
                                }
                                "add_task" -> {
                                    if (parts.size >= 4) {
                                        val title = parts[1]
                                        val prio = parts[2].uppercase() // HIGH, MEDIUM, LOW
                                        val dueDate = parts[3]
                                        val tag = parts.getOrNull(4) ?: "AI Task"
                                        addTask(title, prio, dueDate, tag)
                                    }
                                }
                                "mark_task_completed" -> {
                                    val id = parts.getOrNull(1)?.toIntOrNull()
                                    if (id != null) {
                                        val taskList = tasks.value
                                        val target = taskList.find { it.id == id }
                                        if (target != null) {
                                            toggleTaskCompletion(target)
                                        }
                                    }
                                }
                                "add_note" -> {
                                    if (parts.size >= 3) {
                                        val title = parts[1]
                                        val noteContent = parts[2]
                                        addNote(title, noteContent, getTodayDate())
                                    }
                                }
                                "add_reminder" -> {
                                    if (parts.size >= 5) {
                                        val title = parts[1]
                                        val rDate = parts[2]
                                        val rTime = parts[3]
                                        val rRepeat = parts[4]
                                        addReminder(title, rDate, rTime, rRepeat)
                                    }
                                }
                                else -> {
                                    Log.w(TAG, "Unknown action command: $cmd")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error executing action: $trimmedLine", e)
                    }
                }
                
                // Remove this line from response text so it doesn't clutter chat bubble!
                cleanText = cleanText.replace(line, "")
            }
        }
        
        return cleanText.trim().replace(Regex("\n{3,}"), "\n\n")
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
