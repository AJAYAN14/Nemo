package com.jian.nemo.core.domain.usecase.audio

import com.jian.nemo.core.domain.repository.AudioRepository
import javax.inject.Inject

class PlayTtsUseCase @Inject constructor(
    private val audioRepository: AudioRepository
) {
    operator fun invoke(text: String, language: String = "ja-JP", id: String? = null) {
        audioRepository.playTts(text, language, id)
    }
}
