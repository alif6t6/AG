package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String, // e.g. "Food", "Transport", "Shopping", "Bills", "Health", "Fun", "Other"
    val date: String, // YYYY-MM-DD
    val note: String = ""
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val priority: String, // "HIGH", "MEDIUM", "LOW"
    val dueDate: String, // YYYY-MM-DD
    val tag: String = "",
    val isCompleted: Boolean = false
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val date: String, // YYYY-MM-DD
    val colorHex: String = "#FF3B30" // Devil red or other pick
)

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:MM
    val repeatType: String, // "Once", "Daily", "Weekly", "Monthly"
    val isEnabled: Boolean = true
)

@Entity(tableName = "chat_logs")
data class ChatLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // "user", "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: String = "singleton",
    val userName: String = "Hridoy",
    val monthlyBudget: Double = 10000.0,
    val currencySymbol: String = "৳",
    val themeMode: String = "dark", // "dark", "light", "system"
    val geminiApiKey: String = "",
    val defaultProvider: String = "gemini", // "gemini" is main
    val maxTokens: Int = 2048,
    val streamingEnabled: Boolean = true,
    val customSystemPrompt: String = "You are DEVIL GPT — a ruthlessly efficient, witty personal AI assistant.",
    val userMemory: String = ""
)
