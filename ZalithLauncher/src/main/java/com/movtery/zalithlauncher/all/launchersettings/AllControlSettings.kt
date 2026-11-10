package com.movtery.zalithlauncher.all.launchersettings

import com.movtery.zalithlauncher.all.ui.AllColors
import com.movtery.zalithlauncher.all.ui.AllButton
import com.movtery.zalithlauncher.all.ui.allStyleForColor
import com.movtery.zalithlauncher.all.ui.AllButtonStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import com.movtery.zalithlauncher.all.ui.AlertDialog
import com.movtery.zalithlauncher.all.ui.OutlinedTextField
import androidx.compose.material3.Text
import com.movtery.zalithlauncher.all.ui.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.layer_controller.data.lang.createTranslatable
import com.movtery.layer_controller.layout.EmptyControlLayout
import com.movtery.layer_controller.layout.EmptyLayoutInfo
import com.movtery.layer_controller.utils.NAME_LENGTH
import com.movtery.layer_controller.utils.newRandomFileName
import com.movtery.layer_controller.utils.saveToFile
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.all.bridge.AllSubmitError
import com.movtery.zalithlauncher.contract.extensionToMimeType
import com.movtery.zalithlauncher.game.control.ControlData
import com.movtery.zalithlauncher.game.control.ControlManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.utils.string.getMessageOrToString
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.Locale

/**
 * Controls tab of the launcher settings. The button layout is the ALL BOUY system now and is edited INSIDE the game (ALL menu -> pencil),
 * so this tab only keeps the global mouse / gesture / gyroscope / gamepad settings. Old layout files in the layouts folder are not
 * touched or deleted - they are simply no longer listed or used.
 */
@Composable
internal fun ControlSettings(onError: AllSubmitError, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("ปุ่มควบคุมในเกม: เข้าเกมแล้วเปิดเมนู ALL กดไอคอนดินสอ (Control Studio) เพื่อเพิ่ม ย้าย ปรับสี รูป และพฤติกรรมของปุ่ม",
                color = AllColors.Text, modifier = Modifier.padding(top = 8.dp))
        }
        item { Text(stringResource(R.string.all_controls_global), color = AllColors.Text, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
        item { SwitchRow(stringResource(R.string.settings_control_mouse_physical_mouse_mode_title), null, AllSettings.physicalMouseMode.state) { AllSettings.physicalMouseMode.save(it) } }
        item { SwitchRow(stringResource(R.string.settings_control_mouse_hide_title), null, AllSettings.hideMouse.state) { AllSettings.hideMouse.save(it) } }
        item { SwitchRow(stringResource(R.string.settings_control_mouse_enable_click_title), null, AllSettings.enableMouseClick.state) { AllSettings.enableMouseClick.save(it) } }
        item { StepRow(stringResource(R.string.settings_control_mouse_size_title), AllSettings.mouseSize, 1, "") }
        item { StepRow(stringResource(R.string.settings_control_mouse_sensitivity_title), AllSettings.cursorSensitivity, 5, "%") }
        item { SwitchRow(stringResource(R.string.settings_control_gesture_control_title), null, AllSettings.gestureControl.state) { AllSettings.gestureControl.save(it) } }
        item { SwitchRow(stringResource(R.string.settings_control_gyroscope_title), null, AllSettings.gyroscopeControl.state) { AllSettings.gyroscopeControl.save(it) } }
        item { StepRow(stringResource(R.string.settings_control_gyroscope_sensitivity_title), AllSettings.gyroscopeSensitivity, 5, "%") }
        finerControlRows()
        item { Text(stringResource(R.string.settings_gamepad_title), color = AllColors.Text, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
        item { SwitchRow(stringResource(R.string.settings_gamepad_title), null, AllSettings.gamepadControl.state) { AllSettings.gamepadControl.save(it) } }
        item { StepRow(stringResource(R.string.settings_gamepad_deadzone_title), AllSettings.gamepadDeadZoneScale, 5, "%") }
        item { StepRow(stringResource(R.string.settings_gamepad_cursor_sensitivity_title), AllSettings.gamepadCursorSensitivity, 5, "%") }
        item { Spacer(Modifier.padding(bottom = 8.dp)) }
    }
}
