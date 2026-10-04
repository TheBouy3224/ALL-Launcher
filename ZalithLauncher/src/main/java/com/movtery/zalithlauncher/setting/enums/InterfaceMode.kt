package com.movtery.zalithlauncher.setting.enums

import com.movtery.zalithlauncher.R

/**
 * 启动器主界面风格：经典界面与 ALL Desktop 共用同一套后端，只换主屏幕外观
 */
enum class InterfaceMode(val textRes: Int) {
    CLASSIC(R.string.interface_mode_classic),
    ALL_DESKTOP(R.string.interface_mode_all_desktop)
}
