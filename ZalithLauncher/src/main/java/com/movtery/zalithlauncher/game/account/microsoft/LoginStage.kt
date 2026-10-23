package com.movtery.zalithlauncher.game.account.microsoft

/** Real steps of the Microsoft sign-in, reported by [com.movtery.zalithlauncher.game.account.microsoftLogin] for the ALL login gate */
enum class LoginStage {
    REQUESTING_CODE,
    WAITING_USER,
    ACCESS_TOKEN,
    XBOX_LIVE,
    XSTS,
    MINECRAFT,
    OWNERSHIP,
    PROFILE,
    SAVING
}
