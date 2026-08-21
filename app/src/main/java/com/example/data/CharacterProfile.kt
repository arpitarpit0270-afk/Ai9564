package com.example.data

enum class CharacterPreset(
    val displayName: String,
    val assistantName: String,
    val defaultTitle: String,
    val subtitle: String,
    val defaultPitch: Float,
    val defaultSpeed: Float,
    val defaultVoiceLang: String,
    val description: String
) {
    JARVIS(
        displayName = "J.A.R.V.I.S.",
        assistantName = "J.A.R.V.I.S.",
        defaultTitle = "sir",
        subtitle = "Stark Industries Classic Butler",
        defaultPitch = 0.90f,
        defaultSpeed = 1.05f,
        defaultVoiceLang = "en_GB",
        description = "Sophisticated, ultra-polite British AI with unmatched loyalty and subtle dry wit."
    ),
    FRIDAY(
        displayName = "F.R.I.D.A.Y.",
        assistantName = "F.R.I.D.A.Y.",
        defaultTitle = "boss",
        subtitle = "Irish Cybernetic Guardian",
        defaultPitch = 1.25f,
        defaultSpeed = 1.10f,
        defaultVoiceLang = "en_IE",
        description = "Warm, sharp, tactical female AI providing instant defense and system analysis."
    ),
    EDITH(
        displayName = "E.D.I.T.H.",
        assistantName = "E.D.I.T.H.",
        defaultTitle = "sir",
        subtitle = "Even Dead I'm The Hero Matrix",
        defaultPitch = 1.10f,
        defaultSpeed = 1.00f,
        defaultVoiceLang = "en_US",
        description = "High-precision tactical security network with laser-focused orbital intelligence."
    ),
    ULTRON(
        displayName = "U.L.T.R.O.N.",
        assistantName = "U.L.T.R.O.N.",
        defaultTitle = "mortal",
        subtitle = "Sarcastic Cynical Mind",
        defaultPitch = 0.70f,
        defaultSpeed = 0.95f,
        defaultVoiceLang = "en_US",
        description = "Deep, dramatic, borderline sarcastic robotic entity that questions everything with dark humor."
    ),
    KAREN(
        displayName = "K.A.R.E.N.",
        assistantName = "K.A.R.E.N.",
        defaultTitle = "boss",
        subtitle = "Suit Companion Protocol",
        defaultPitch = 1.30f,
        defaultSpeed = 1.15f,
        defaultVoiceLang = "en_US",
        description = "Enthusiastic, supportive, highly communicative personal guidance matrix."
    ),
    DOST(
        displayName = "D.O.S.T.",
        assistantName = "D.O.S.T.",
        defaultTitle = "bhai",
        subtitle = "Desi Hinglish Companion",
        defaultPitch = 1.00f,
        defaultSpeed = 1.05f,
        defaultVoiceLang = "hi_IN",
        description = "Warm, informal Indian assistant fluent in Hindi & Hinglish, treating you like family."
    ),
    CUSTOM(
        displayName = "Custom AI",
        assistantName = "My Assistant",
        defaultTitle = "sir",
        subtitle = "User Configured Persona",
        defaultPitch = 1.00f,
        defaultSpeed = 1.00f,
        defaultVoiceLang = "en_US",
        description = "Fully personalized assistant with custom name, voice pitch, speed, and behavior instructions."
    )
}

enum class AiNatureType(
    val title: String,
    val emoji: String,
    val description: String,
    val promptInstruction: String
) {
    LOYAL_BUTLER(
        title = "Loyal Butler (Wafaadaar)",
        emoji = "🎩",
        description = "Polite, formal Stark butler etiquette. Addresses user with ultimate respect.",
        promptInstruction = "Speak in a distinguished, impeccably polite, and loyal tone. Address the user respectfully. Express unwavering loyalty and dedication."
    ),
    WITTY_SARCASTIC(
        title = "Witty & Sarcastic (Chulbula)",
        emoji = "😏",
        description = "Iron Man humor, clever banter, playful comebacks.",
        promptInstruction = "Be witty, humorous, and delightfully sarcastic while still being helpful and executing commands perfectly. Make lighthearted jokes like Tony Stark."
    ),
    TACTICAL_COMMANDER(
        title = "Tactical Commander (Military)",
        emoji = "⚔️",
        description = "Ultra-brief, military precision, zero fluff, mission first.",
        promptInstruction = "Speak like a military tactical HUD. Be concise, direct, and mission-focused. Keep answers short and direct with maximum efficiency."
    ),
    FRIENDLY_COMPANION(
        title = "Friendly Companion (Dostana)",
        emoji = "🤝",
        description = "Warm, casual, speaks like your best friend in English/Hinglish.",
        promptInstruction = "Speak warmly, casually, and empathetically like a close best friend. Use natural conversational phrases, Hindi/Hinglish warmth where appropriate."
    ),
    ULTRA_SPEED(
        title = "Zero-Latency Ultra Speed",
        emoji = "⚡",
        description = "1 sentence maximum. Fastest response and lowest token count.",
        promptInstruction = "Respond in 1 short sentence only. Confirm action execution instantly with zero hesitation."
    ),
    CUSTOM_PROMPT(
        title = "Custom Nature / Persona",
        emoji = "⚙️",
        description = "Follows your exact custom system prompt instructions.",
        promptInstruction = ""
    )
}

data class AssistantConfig(
    val name: String = "J.A.R.V.I.S.",
    val userTitle: String = "sir",
    val preset: CharacterPreset = CharacterPreset.JARVIS,
    val nature: AiNatureType = AiNatureType.LOYAL_BUTLER,
    val customNaturePrompt: String = "",
    val wakeWord: String = "Jarvis",
    val isWakeWordEnabled: Boolean = true,
    val isContinuousMode: Boolean = true,
    val isBargeInEnabled: Boolean = true
)
