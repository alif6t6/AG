package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class AppRepository(private val dao: AppDao) {

    // --- Expenses ---
    val allExpenses: Flow<List<Expense>> = dao.getAllExpenses()
    suspend fun insertExpense(expense: Expense) = dao.insertExpense(expense)
    suspend fun updateExpense(expense: Expense) = dao.updateExpense(expense)
    suspend fun deleteExpense(expense: Expense) = dao.deleteExpense(expense)
    suspend fun deleteExpenseById(id: Int) = dao.deleteExpenseById(id)
    suspend fun deleteAllExpenses() = dao.deleteAllExpenses()

    // --- Tasks ---
    val allTasks: Flow<List<Task>> = dao.getAllTasks()
    suspend fun insertTask(task: Task) = dao.insertTask(task)
    suspend fun updateTask(task: Task) = dao.updateTask(task)
    suspend fun deleteTask(task: Task) = dao.deleteTask(task)
    suspend fun deleteTaskById(id: Int) = dao.deleteTaskById(id)

    // --- Notes ---
    val allNotes: Flow<List<Note>> = dao.getAllNotes()
    suspend fun insertNote(note: Note) = dao.insertNote(note)
    suspend fun updateNote(note: Note) = dao.updateNote(note)
    suspend fun deleteNote(note: Note) = dao.deleteNote(note)
    suspend fun deleteNoteById(id: Int) = dao.deleteNoteById(id)

    // --- Reminders ---
    val allReminders: Flow<List<Reminder>> = dao.getAllReminders()
    suspend fun insertReminder(reminder: Reminder) = dao.insertReminder(reminder)
    suspend fun updateReminder(reminder: Reminder) = dao.updateReminder(reminder)
    suspend fun deleteReminder(reminder: Reminder) = dao.deleteReminder(reminder)
    suspend fun deleteReminderById(id: Int) = dao.deleteReminderById(id)

    // --- Chat Logs ---
    val allChatLogs: Flow<List<ChatLog>> = dao.getAllChatLogs()
    suspend fun insertChatLog(chatLog: ChatLog) = dao.insertChatLog(chatLog)
    suspend fun clearChatLogs() = dao.clearChatLogs()

    // --- App Settings ---
    val appSettings: Flow<AppSettings?> = dao.getSettingsFlow()

    suspend fun getSettingsDirect(): AppSettings {
        var settings = dao.getSettingsDirect()
        if (settings == null) {
            settings = AppSettings()
            dao.saveSettings(settings)
        }
        return settings
    }

    suspend fun saveSettings(settings: AppSettings) {
        dao.saveSettings(settings)
    }
}
