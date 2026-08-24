package com.example.data

enum class AiEngineType(val displayName: String, val badge: String, val description: String) {
    AUTO_HYBRID("Auto-Hybrid Smart Array", "⚡ Hybrid", "Auto-selects fastest configured API (Gemini/Groq/OpenAI/Claude) with instant local fallback."),
    GEMINI_FREE("Google Gemini API", "✨ Gemini", "Google AI Studio models (Gemini 2.5 Flash, 2.0 Flash, Flash-Lite)."),
    OPENAI_GPT("OpenAI GPT Matrix", "🧠 OpenAI", "OpenAI official models (GPT-4o, GPT-4o-mini, o3-mini)."),
    GROQ("Groq Lightning LPU", "⚡ Groq", "Ultra-fast inference (Llama 3.3 70B, Llama 3.1 8B) with sub-second speeds."),
    DEEPSEEK("DeepSeek AI Core", "🐋 DeepSeek", "DeepSeek Chat & DeepSeek Reasoner models."),
    CLAUDE("Anthropic Claude", "🎭 Claude", "Anthropic Claude 3.5 Sonnet & Claude 3 Haiku."),
    OPENROUTER("OpenRouter Matrix", "🌐 OpenRouter", "Universal AI router supporting hundreds of free & premium models."),
    CUSTOM_ENDPOINT("Custom AI Endpoint", "🔌 Custom", "Custom OpenAI-compatible Base URL & Model ID."),
    LOCAL_OFFLINE("Instant Local Engine (0ms)", "🔒 Offline", "100% on-device heuristic engine with zero network latency.")
}

data class MultiApiConfig(
    val selectedEngine: AiEngineType = AiEngineType.AUTO_HYBRID,
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-2.5-flash",
    val openAiApiKey: String = "",
    val openAiModel: String = "gpt-4o-mini",
    val groqApiKey: String = "",
    val groqModel: String = "llama-3.3-70b-versatile",
    val deepSeekApiKey: String = "",
    val deepSeekModel: String = "deepseek-chat",
    val claudeApiKey: String = "",
    val claudeModel: String = "claude-3-5-sonnet-20241022",
    val openRouterApiKey: String = "",
    val openRouterModel: String = "google/gemini-2.0-flash-exp:free",
    val picovoiceAccessKey: String = "",
    val customApiBaseUrl: String = "https://api.openai.com/v1",
    val customApiKey: String = "",
    val customModelName: String = "gpt-4o-mini"
)
