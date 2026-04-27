package com.jian.nemo.core.domain.model

data class TtsVoice(
    val name: String,
    val locale: String,
    val isNetworkConnectionRequired: Boolean = false,
    val quality: String = "normal", // normal, high, very_high
    val gender: String = "unknown" // male, female, unknown
)
