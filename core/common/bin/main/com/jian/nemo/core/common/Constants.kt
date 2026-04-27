package com.jian.nemo.core.common

object Constants {
    // App Configuration
    const val APP_NAME = "Nemo 2.0"
    const val APP_VERSION = "1.0.0"

    // Database
    const val DATABASE_NAME = "nemo_database"
    const val DATABASE_VERSION = 1

    // DataStore
    const val PREFERENCES_NAME = "nemo_preferences"

    // Network
    const val NETWORK_TIMEOUT = 30L // seconds
    const val MAX_RETRY_ATTEMPTS = 3

    // Learning Configuration (Basic constraints)
    const val DEFAULT_DAILY_GOAL = 50
    const val MIN_DAILY_GOAL = 10
    const val MAX_DAILY_GOAL = 200

    // SRS Algorithm (Core parameters)
    const val DEFAULT_EASINESS_FACTOR = 2.5
    const val MIN_EASINESS_FACTOR = 1.3
    const val MAX_EASINESS_FACTOR = 2.5

    // Date and Time
    const val MILLIS_PER_DAY = 86400000L  // 24 * 60 * 60 * 1000

    // Legacy Aliases (Deprecated)
    @Deprecated("Use domain constants")
    object TestMode {
        const val MULTIPLE_CHOICE_JP_TO_CN = "jp_to_cn"
        const val MULTIPLE_CHOICE_CN_TO_JP = "cn_to_jp"
        const val TYPING_PRACTICE = "typing"
        const val CARD_MATCHING = "matching"
        const val SENTENCE_SORTING = "sorting"
    }

    @Deprecated("Use domain constants")
    object PartOfSpeech {
        const val VERB = "verb"
        const val NOUN = "noun"
        const val ADJECTIVE = "adjective"
        const val ADVERB = "adverb"
        const val PARTICLE = "particle"
        const val CONJUNCTION = "conjunction"
        const val RENTAI = "rentai"
        const val PREFIX = "prefix"
        const val SUFFIX = "suffix"
        const val INTERJECTION = "interjection"
        const val FIXED_EXPRESSION = "fixed_expr"
    }

    @Deprecated("Use domain constants")
    object ReviewStrategy {
        const val ERROR_PRONE = "error_prone"
        const val OLDEST_REVIEW = "oldest"
        const val NEWEST_LEARNED = "newest"
    }

    @Deprecated("Use domain constants")
    val JLPT_LEVELS = listOf("n1", "n2", "n3", "n4", "n5")
}
