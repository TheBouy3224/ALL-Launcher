package com.movtery.zalithlauncher.all.bouy

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Profile list shared by the Studio (manage = true: new / rename / copy / delete) and the in-game Quick Menu (manage = false: switch only).
 * [onSwitched] fires after the ACTIVE profile changed, so the caller reloads the layout (which releases every held input).
 */
@Composable
fun BouyProfilesPanel(manage: Boolean, textColor: Color, onSwitched: () -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf<BouyStore.Profile?>(null) }
    val list = remember(tick) { BouyStore.profiles(context) }
    val active = remember(tick) { BouyStore.activeId(context) }
    fun switchTo(id: String) { if (BouyStore.setActive(context, id)) { tick++; onSwitched() } }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (manage) {
            OutlinedTextField(name, { name = it.take(30) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("ชื่อโปรไฟล์ (ใช้ตอนสร้าง / เปลี่ยนชื่อ / คัดลอก)") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("+ ใหม่ (D-Pad)", { val id = BouyStore.create(context, name, null, "dpad"); name = ""; switchTo(id) })
                Pill("+ Joystick กลม", { val id = BouyStore.create(context, name, null, "joy_round"); name = ""; switchTo(id) })
                Pill("+ Joystick เหลี่ยม", { val id = BouyStore.create(context, name, null, "joy_square"); name = ""; switchTo(id) })
            }
        }
        list.forEach { p ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text((if (p.id == active) "● " else "○ ") + p.name, Modifier.weight(1f).clickable { if (p.id != active) switchTo(p.id) }.padding(vertical = 10.dp), color = textColor)
                if (manage) {
                    Pill("ชื่อ", { BouyStore.rename(context, p.id, name); name = ""; tick++ }, enabled = name.isNotBlank())
                    Pill("คัดลอก", { val id = BouyStore.create(context, name.ifBlank { p.name + " สำเนา" }, p.id); name = ""; switchTo(id) })
                    Pill("ลบ", { confirmDelete = p }, enabled = list.size > 1, color = BouyTheme.danger)
                }
            }
        }
    }

    confirmDelete?.let { p ->
        androidx.compose.ui.window.Dialog({ confirmDelete = null }) {
            BouyMaterial {
                Panel {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("ลบโปรไฟล์ \"${p.name}\"?", color = BouyTheme.text)
                        Text("ปุ่มและรูปของโปรไฟล์นี้จะถูกลบถาวร ย้อนกลับไม่ได้ (โปรไฟล์อื่นไม่ได้รับผลกระทบ)", color = BouyTheme.dim)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill("ยกเลิก", { confirmDelete = null })
                            Pill("ลบ", {
                                val wasActive = p.id == active
                                BouyStore.delete(context, p.id); confirmDelete = null; tick++
                                if (wasActive) onSwitched()
                            }, color = BouyTheme.danger)
                        }
                    }
                }
            }
        }
    }
}
