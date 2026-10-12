# ALL Mod Doctor — เอกสารหลัก (Handoff)

อัปเดตล่าสุด: 2026-10-12 · Repo: `D:\ALL Launcher 0.0.1` · branch `all-0.0.1`
HEAD ตอนเขียนเอกสาร: `dc52731d` (ยังไม่มี commit ของงาน Planner — ดูหัวข้อ "สถานะไฟล์")

> ใช้ไฟล์นี้เป็นจุดเริ่มเมื่อเปิดแชทใหม่ ก่อนแก้ Mod Doctor ให้อ่านหัวข้อ "สิ่งที่ห้ามทำซ้ำ" และ "NEXT ACTION" ก่อนเสมอ
> หลักฐานตามรายละเอียดอยู่ใน `ALL_CORE_CHECKPOINT.md` (อ้างด้วยหัวข้อและวันที่ ไม่คัดลอกซ้ำ)

---

## 1. เป้าหมายและวิธีทำงาน

**เป้าหมาย:** ก่อนกด "เล่น" ตรวจม็อดในอินสแตนซ์ ถ้าพบปัญหาที่ทำให้เกมเข้าไม่ได้หรือเกิดความขัดแย้ง ให้หยุดการเปิดเกมและแสดงสิ่งที่พบ พร้อมให้ผู้ใช้เลือกซ่อม

**ลำดับการทำงาน (เส้นทางจริงในโค้ด):**
1. ผู้ใช้กด Play → `LauncherElements.kt` (TryLaunch)
2. `ModDoctor.inspect(version)` (`ModDoctor.kt` ~บรรทัด 964) อ่านม็อดทั้งหมดในโฟลเดอร์ `mods`
   - อ่าน metadata ของแต่ละ jar (`AllModReader`, `readEnabledModIdentities`)
   - ตรวจ: dependency ที่ขาด, loader ผิด, เวอร์ชันเกมผิด (ทั้งจาก metadata ในเครื่องและจาก provider), ม็อดซ้ำ, ไฟล์เสีย, `breaks`/`conflicts` ที่ประกาศ, กฎชุดที่รู้ว่าเข้ากัน (known compatible set)
   - ผลเป็น `ModDoctorReport` (รายการ `ModDoctorProblem`)
3. `decideLaunchGate(report)` (`ModDoctor.kt` ~บรรทัด 296) → LAUNCH หรือ BLOCK
   - choke point: `LaunchGameOperation.RealLaunch.modCheckPassed` (`LauncherElements.kt`) ทุกทางที่จะเริ่มเกมต้องผ่านด่านนี้
4. ถ้า BLOCK แสดง dialog ของ ALL พร้อมปุ่ม **แก้ไข** / **ยกเลิก** / **เล่นต่อทั้งที่ยังมีปัญหา**
5. **แก้ไข** → `ModDoctor.fixProblems(version, report)` (~บรรทัด 1043) ลำดับ:
   - ไฟล์ที่ modpack กำหนดตัวตนแน่นอน → กู้คืนด้วย identity เดิม
   - ปิดใช้งานแบบย้อนกลับได้ (`disableModFile` → `.disabled`) ตามกฎ local disable
   - กฎชุดที่รู้ว่าเข้ากัน (`repairKnownSet`) → ติดตั้งทั้งชุดเมื่อผ่านการตรวจ
   - แก้คู่ที่ขัดกัน (`repairBreaksConflict` → `ModDoctorMultiModRepair`)
   - แทนที่ไฟล์ที่ไม่ตรงเวอร์ชันเกม/loader (`replacementFix`)
   - ติดตั้ง dependency ที่ขาด (`tryInstallDependencyCandidate`, Dependency Resolver เดิม)
6. หลังแก้ → inspect ซ้ำ (post-fix re-check) → ถ้าไม่มีปัญหาจึงเปิดเกมอัตโนมัติ

**หลักการที่ระบบตั้งใจยึดถือ:** ไม่เดาเวอร์ชัน (ถ้าหาเวอร์ชันที่ยืนยันได้ไม่ได้จะไม่แก้), ไฟล์ของ modpack ไม่ถูกแตะโดยอัตโนมัติ, และการซ่อมที่ล้มเหลวกลางทางจะพยายามย้อนกลับเอง **แต่ยังไม่ได้รับรองว่าไม่มีการลบไฟล์** — ดูข้อจำกัดด้านความปลอดภัยในหัวข้อ 8

---

## 2. ฟีเจอร์ที่ทำเสร็จแล้ว

ระดับความมั่นใจของแต่ละรายการแยกไว้ในหัวข้อ 9 — เอกสารนี้ไม่ได้รับรองว่า "ใช้ได้ทุกกรณี"

| ฟีเจอร์ | สถานะ | หลักฐานหลัก (checkpoint) |
|---|---|---|
| Mod Doctor V1 รายงานอย่างเดียว | PASS (historical) | "Mod Doctor V1 REPORT-ONLY — PASS" (~บรรทัด 563) |
| ตรวจ wrong loader บนเครื่องจริง | PASS (historical) | "wrong-loader real-device QA — PASS" (~บรรทัด 682) |
| Missing-dependency detect + Fix (Debug / Release) | PASS (historical) | "missing-dependency Fix E2E (final, real device) — PASS" (~บรรทัด 928), "NEW R8 Release build at CURSEFORGE — PASS" (~2982) |
| Wrong-loader repair E2E | PASS (historical) | "Wrong-loader repair E2E (final, real device) — PASS" (~บรรทัด 1111) |
| Wrong-MC-version repair (provider-backed) | PASS (historical): E2E เคย BLOCKED ก่อน แล้วมี PASS ภายหลังที่ "Broad/open-range provider rule device E2E (Balm fixture)" (~บรรทัด 1706) และ "WRONG_MINECRAFT_VERSION Fix device E2E" (~บรรทัด 1749) | ~1706, ~1749 |
| Duplicate / corrupt / offline-safety detection | PASS (historical) | ~บรรทัด 1792, 1832, 1872 |
| Launch gate (choke point) | PASS (historical) | "MOD_DOCTOR_REAL_LAUNCH_GATE_FIX" (~บรรทัด 5052), unit `ModDoctorLaunchGateTest` |
| Pack-safe mode (ม็อดแพ็ก) | PASS (historical) | "MODPACK_SAFE_MODE_V0" + "_SMOKE — PRACTICAL_PASS" (~4951, 5022) |
| Known compatible set (SIMPLE RULES V1) | PASS (historical) | "MOD DOCTOR SIMPLE RULES V1" (~5673) — ใช้กับ 1.21.11 Fabric เท่านั้น |
| Fabric legacy conflicts advisory | PASS (historical) | (~5754) |
| Post-crash learning (local) | PARTIAL (historical) | (~5857, 5956) |
| Physical iQOO: live world play ผ่าน gate + report | PASS (historical) | "MOD DOCTOR V1 PHYSICAL WORLD-PLAY PROOF — PASS" (~6347) |
| Phase 5.1 E2E บน LDPlayer: detect → warn → fix → recheck → launch → cancel → duplicate | PASS (current, emulator) | ~บรรทัด 7652 และ `test-artifacts/modcheck-e2e-20261011/00-SUMMARY.md` |
| Phase 5.1b Download fail/retry (UI + install target) | UX PASS; error path UNVERIFIED | ~บรรทัด 7677 |
| ModGroupPlanner (Plan-only, ไม่แก้ไฟล์) | IMPLEMENTED + unit test PASS (current) | ไฟล์ใหม่ดูหัวข้อ 4 |

---

## 3. กฎ Compatible Versions ที่มีอยู่

- ไฟล์: `ZalithLauncher/src/main/assets/mod_doctor_rules.json`
- โค้ดโหลด: `ModDoctorRules.kt` (`parseModDoctorRules`, `ModDoctorRules.load()`) — อ่านไม่ได้ก็ข้ามกฎ ไม่ทำให้เกมพัง
- ปัจจุบันมี **1 กฎ**:
  - Minecraft `1.21.11`, loader `fabric`
  - เมื่อมี `sodium`, `sodium-extra`, `reeses-sodium-options`, `iris` **ครบทั้งสี่**
  - ชุดที่ยืนยันแล้ว: sodium `0.8.12`, sodium-extra `0.9.1`, reeses-sodium-options `2.2.3`, iris `1.10.7`
- ความหมาย: กฎนี้เป็น **Known-good Set** (ชุดที่ยืนยันแล้วว่าใช้งานได้จริง) ห้ามลบหรือทับ งาน Dynamic Compatibility จะเพิ่มชุดจากข้อมูลจริงเป็นอีกระดับ ไม่แทนที่กฎนี้ ถ้าจะเพิ่มกฎต้องเพิ่มเป็นรายการใหม่ และต้องมีหลักฐานการทดสอบ
- ข้อจำกัด: ไม่มีกฎสำหรับเวอร์ชันเกมอื่น และไม่ได้ดึงข้อมูลจาก Modrinth

---

## 4. ส่วนประกอบเดิมที่ต้องใช้ซ้ำ (ห้ามสร้างซ้ำ)

| ส่วน | ไฟล์ | หน้าที่ |
|---|---|---|
| ModDoctor | `game/version/mod/ModDoctor.kt` | inspect, fixProblems, repairKnownSet, repairBreaksConflict, launch gate |
| ModDoctorRules | `game/version/mod/ModDoctorRules.kt` | โหลดกฎชุดที่รู้ว่าเข้ากัน, `matchesModVersion`, `baseModVersion` |
| ModDoctorMultiModRepair | `game/version/mod/ModDoctor.kt` (class ภายในไฟล์เดียวกัน ใช้ใน `repairBreaksConflict`) | แก้คู่ขัดแย้งแบบหลายม็อด ผ่าน stage ก่อน ค่อยเปลี่ยนจริง |
| ModUpdater | `game/version/mod/update/ModUpdater.kt` | อัปเดตม็อดเดิมทั้งชุด (`updateAll`) — มีอยู่แล้ว ยังไม่ได้เชื่อมกับ Mod Doctor |
| ModpackPolicy | `game/version/mod/ModpackPolicy.kt` | ระบุไฟล์ที่ modpack จัดการ, ไม่อนุญาตให้แก้โดยอัตโนมัติ |
| Dependency Resolver | `game/download/assets/` (`downloadSingleForVersions`, `isCompatibleWith`, `PlatformVersion.platformDependencies`) | ติดตั้งและหา dependency ตามกฎ Modrinth/CurseForge |
| Installed-mod matcher | `game/version/mod/InstalledModMatcher.kt`, `ModFingerprints.kt` | จับคู่ไฟล์ในเครื่องกับโปรเจกต์บนแพลตฟอร์มด้วย fingerprint |
| Stable-first / candidate step | `ModDoctor.kt` (`firstAcceptedCandidate`, ~บรรทัด 132) และ `ModDoctorStableFirstCandidateTest` | เลือกผู้สมัครที่เสถียรก่อน |

---

## 5. Planner ใหม่ (Phase A — Plan only)

- ไฟล์: `game/version/mod/ModGroupPlanner.kt`
- Test: `ZalithLauncher/src/test/.../game/version/mod/ModGroupPlannerTest.kt`
- **ผูกกับ `ModDoctorSetRule` ซึ่งมีเฉพาะ 1.21.11 Fabric** — ฟังก์ชันคืน NO_CHANGE เงียบ ถ้า Minecraft/loader ไม่ตรงกับกฎ
- ฟังก์ชัน: `planModGroupRepair(minecraftVersion, loader, installed, rule, candidatesByModId, isPackOwned)` เป็นฟังก์ชันบริสุทธิ์ ไม่มีการดาวน์โหลด/เปลี่ยนชื่อ/ปิด/ลบ/แทนที่
- สถานะผลลัพธ์: `NO_CHANGE` · `PLAN` · `UNVERIFIED` · `BLOCKED_PACK_OWNED`
- ระดับความมั่นใจ: `HIGH` · `MEDIUM` · `UNVERIFIED`
- **ผลทดสอบ (current):** `ModGroupPlannerTest` 13 tests — 0 failures, 0 errors
  - คำสั่ง: `:ZalithLauncher:testDebugUnitTest --tests "...ModGroupPlannerTest"` → `BUILD SUCCESSFUL in 5m 14s`
  - ครอบคลุม: กลุ่มไม่ครบ (เงียบ), เวอร์ชัน/loader อื่น (เงียบ), ตรงแล้ว (ไม่แตะ), แตะเฉพาะตัวที่ไม่ตรง, ไม่มีผู้สมัคร (UNVERIFIED), MC ผิด, loader ผิด, dependency ขาด, dependency มีอยู่แล้ว (ยอมรับ), conflict ที่ประกาศ (UNVERIFIED), ม็อดแพ็ก (BLOCKED), เทียบเวอร์ชันโดยไม่ prefix-match (`0.8.120` ≠ `0.8.12`), ความมั่นใจ MEDIUM เมื่อไม่มีข้อมูล dependency
- **ยังไม่ได้เชื่อมกับ Modrinth จริง** — ตอนนี้รับข้อมูลที่ป้อนเองเท่านั้น
- **ยังไม่ได้แสดงบน UI และยังไม่ได้ทดสอบบน LDPlayer**
- ข้อควรระวัง: `maxByOrNull { versionName }` เป็นการเรียงแบบ string ยังไม่ใช่เวอร์ชันเชิงความหมาย (ต้องแก้ก่อนใช้จริง)
- **ยังไม่ commit** (ไฟล์ใหม่ 2 ไฟล์เป็น untracked)

---

## 6. ส่วนที่ยังขาด และบั๊ก/ข้อจำกัดที่ยังไม่แก้

1. **Planner ยังไม่เชื่อมกับ Modrinth จริง** — ต้องแปลง `PlatformVersion` (`platformGameVersion`, `platformLoaders`, `platformDependencies`) เป็น `GroupPlanCandidate` และแมป dependency projectId → modId ผ่านม็อดที่ติดตั้งแล้ว (dependency ของ Modrinth ใช้ project ID ไม่ใช่ modId)
2. **ยังไม่มี UI แสดงแผน** — dialog เดิมแสดงเฉพาะปัญหา ไม่มีการแสดงเวอร์ชันเดิม/ใหม่และความเสี่ยง
3. **ยังไม่มี Backup/Rollback** — ยังไม่มีการสำรองทั้งชุดก่อนแก้ (ออกแบบไว้สำหรับ Phase B)
4. **Fix ลบ/แทนที่โดยไม่สำรองแยก** — `replacementFix` ใช้ `disableModFile` ก่อน แต่ยังไม่มี manifest ของการเปลี่ยนแปลงทั้งชุด
5. **กฎชุดที่รู้มีแค่ 1 รายการ** และผูกกับ 1.21.11 Fabric
   - `ModGroupPlanner` ยังรับ `ModDoctorSetRule` ซึ่งมีเฉพาะ 1.21.11 Fabric ดังนั้น planner ทำงานได้กับชุดนี้เท่านั้น
   - งาน **Dynamic Compatibility** ต้องรองรับ Minecraft และ loader อื่นจากข้อมูลจริงของ Modrinth โดย **รักษากฎเดิมเป็น Known-good Set** (ไม่ลบหรือทับ `mod_doctor_rules.json`) และให้ผลจากข้อมูลจริงเป็นระดับความมั่นใจแยกต่างหาก
6. **ModUpdater ไม่ได้เชื่อมกับ Mod Doctor** (ใช้แยกกัน)
7. **BLOCKED เดิม:** `INCOMPATIBLE_MOD_PAIR` (Sodium ↔ Iris) ยังแก้ด้วยปุ่มเดียวไม่ได้ เพราะหาเวอร์ชันแทนที่ที่ปลอดภัยไม่พบในกราฟจริง (checkpoint ~บรรทัด 5587)
8. **Error path ของการดาวน์โหลดระหว่าง Fix: UNVERIFIED** — ไม่มีวิธีจำลองข้อผิดพลาดที่ปลอดภัย (ห้ามเพิ่ม failure hook ตามคำสั่ง)
9. **เกมตายแบบสุ่มบน LDPlayer (`:game` SIGKILL/ANR ~30–50% ของการเปิด):** สาเหตุยังไม่รู้ ไม่ใช่ความผิดของ Mod Doctor แต่ทำให้การทดสอบ launch ต้องลองซ้ำ (สูงสุด 3 ครั้ง)
10. **Physical iQOO:** ไม่ได้ทดสอบ Phase 5.1/5.1b/Planner (ต้องรอคำสั่ง)
11. **Sort ของผู้สมัคร:** ยังไม่มี semver comparator ที่ใช้ร่วมกันสำหรับ planner

---

## 7. สิ่งที่เคยทดสอบผ่านแล้ว และสิ่งที่ห้ามทำซ้ำ

**ทดสอบผ่านแล้ว (ไม่ต้องทำซ้ำโดยไม่มีเหตุผลใหม่):**
- Missing-dependency detect + fix บนอุปกรณ์จริงและ release R8 (historical)
- Wrong-loader repair E2E บนอุปกรณ์จริง (historical)
- Physical iQOO world play ผ่าน launch gate (historical, 2026-09-25)
- Phase 5.1 E2E บน LDPlayer (current, 2026-10-11) ทั้ง detect/warn/fix/recheck/launch/cancel/duplicate

**ห้ามทำซ้ำ:**
- ห้ามสร้าง executor, installer หรือ resolver ใหม่ — ใช้ `fixProblems`, `repairKnownSet`, `repairBreaksConflict`, `tryInstallDependencyCandidate` เดิม
- ห้ามสร้างกฎเวอร์ชันแบบ hardcode ในโค้ด — เพิ่มใน `mod_doctor_rules.json` และต้องมีหลักฐาน
- ห้ามเดาว่าชุดม็อดเข้ากันได้โดยไม่มีข้อมูลจาก provider หรือกฎที่ยืนยัน
- ห้ามรีเซ็ตหรือลบค่า Provider ของผู้ใช้ และห้ามล้างข้อมูลแอปเพื่อทดสอบ
- ห้ามแตะ `D:\ALL Mod Doctor Warehouse`, Main Instance, QA backup, บัญชี Microsoft `THE_BOUY2`

---

## 8. ข้อจำกัดการซ่อมและกฎรักษาไฟล์

- **ข้อจำกัดด้านความปลอดภัยที่ยังต้องแก้ (ยืนยันจากโค้ด):**
  - `ModDoctor.kt` ใน `ModDoctorMultiModRepair` (ประมาณบรรทัด 918) เรียก `target.delete()` กับไฟล์ที่ชื่อตรงกับเวอร์ชันใหม่ก่อนคัดลอก ถ้ามีไฟล์ชื่อนั้นอยู่และไม่ได้เก็บสำเนาไว้ก่อน ไฟล์นั้นจะหายถาวร
  - ฝั่ง `replacementFix` (ประมาณบรรทัด 1159) ใช้ `disableModFile` เก็บไฟล์เดิมไว้ก่อนแล้ว จึงไม่ลบไฟล์เดิม
  - ทางเลือกการแก้ที่ยังไม่ได้ทำ: เปลี่ยน `target.delete()` เป็นการเก็บสำเนาเดิมก่อน (เช่น `disableModFile`) แล้วค่อยคัดลอกใหม่ ผ่าน unit test ก่อน
  - ระบบ **ไม่ได้** ลบไฟล์ม็อดเดิมทั้งหมดอย่างที่เคยเข้าใจ แต่จุดนี้ยังเป็นความเสี่ยงที่ต้องแก้ก่อนขยายการซ่อม
- **Rollback แยกเป็น 2 ชนิด (ห้ามปนกัน):**
  1. **Rollback อัตโนมัติระหว่างการซ่อมล้มเหลว** — `ModDoctorMultiModRepair` มี `rollback()` (ประมาณบรรทัด 893) ลบไฟล์ที่คัดลอกใหม่ในรอบนั้น คืนไฟล์ที่ปิดใช้งานไว้ และตรวจ sha1 ของต้นฉบับ ถ้าคืนไม่ได้จะส่งผล `REPAIR_ROLLBACK_FAILED`
  2. **User-triggered Rollback หลังซ่อมสำเร็จ** — **ยังไม่มี** ไม่มีสำเนาทั้งชุดก่อนแก้และไม่มีปุ่มย้อนกลับ (ต้องทำใน Phase B)
- **ห้ามแก้ไฟล์ของ modpack** โดยอัตโนมัติ (`ModpackPolicy` + `BLOCKED_PACK_OWNED`) ต้องได้รับอนุญาตจากผู้ใช้ก่อน
- **ไม่เดาเวอร์ชัน:** ถ้าไม่มีผู้สมัครที่ยืนยันได้ → `UNVERIFIED` แสดงเหตุผล ไม่แก้
- **ผ่านการ stage ก่อนเสมอ:** `ModDoctorMultiModRepair` และ `repairKnownSet` ดาวน์โหลดลงที่ staging ตรวจ metadata ของไฟล์จริง แล้วค่อยเปลี่ยน
- **การย้อนกลับ:** ดูชนิด rollback ข้างต้น ระบบ Preserve (`REPAIR_PRESERVE_FAILED`) จะหยุดถ้าเก็บไฟล์เดิมไม่ได้
- **เมื่อไม่มั่นใจ → ไม่ทำ** และต้องรายงานใน UI/log

---

## 9. สถานะการพิสูจน์ (แยกชัดเจน)

- **Current PASS** (ทดสอบในรอบ 2026-10-11/12 ด้วยหลักฐานที่บันทึกแล้ว): Phase 5.1 E2E บน LDPlayer, Phase 5.1b (UX/install target), ModGroupPlanner 13 unit tests, Download auto-discovery (ไม่เกี่ยวกับ Mod Doctor โดยตรง)
- **Historical PASS** (มีหลักฐานใน checkpoint หัวข้อที่ระบุ แต่ไม่ได้รันซ้ำในรอบนี้): Mod Doctor V1/V2 report + fix บนอุปกรณ์จริง, physical iQOO world play, release R8, modpack safe mode
- **IMPLEMENTED แต่ UNVERIFIED บนอุปกรณ์:** ModGroupPlanner (ยังไม่เชื่อม Modrinth, ยังไม่มี UI), กฎ Sodium group ใช้ได้กับ 1.21.11 Fabric เท่านั้นตามหลักฐาน
- **UNVERIFIED:** Error path ระหว่าง Fix, Physical iQOO สำหรับ Phase 5.1/5.1b/Planner, เวอร์ชันเกมอื่นนอกจาก 1.21.11 Fabric, `INCOMPATIBLE_MOD_PAIR` แก้อัตโนมัติ (BLOCKED เดิม)

---

## 10. สถานะไฟล์ (ณ เวลาเขียนเอกสาร)

- Git HEAD: `dc52731d` (ALL Download auto discovery)
- ไฟล์ใหม่ (untracked, ยังไม่ commit): `game/version/mod/ModGroupPlanner.kt`, `src/test/.../ModGroupPlannerTest.kt`, `MOD_DOCTOR_HANDOFF.md`
- ไฟล์ที่แก้ใน checkpoint (รอ commit ร่วมกับเอกสาร): `ALL_CORE_CHECKPOINT.md`
- ไม่มีการแก้ไฟล์ Mod Doctor เดิมในรอบนี้
- `test-artifacts/` และ `legacy-archive/` เป็น untracked และห้าม stage

---

## 11. NEXT ACTION (งานเดียว)

**เชื่อม `ModGroupPlanner` กับ Modrinth จริงแบบ Plan Only และแสดงผลใน dialog เดิมของ Mod Doctor**

ขอบเขต:
1. เขียนตัวแปลงจาก `PlatformVersion` ของ Modrinth เป็น `GroupPlanCandidate` ภายใน `ModDoctor` โดยใช้ `loadProjectVersions` และ `lookupInstalledMod` ที่มีอยู่
2. แมป dependency projectId → modId ผ่านม็อดที่ติดตั้งอยู่แล้ว ถ้าแมปไม่ได้ให้ใส่เป็น UNVERIFIED
3. เปลี่ยนการเรียงผู้สมัครเป็น semver comparator ก่อนเชื่อม
4. แสดงผลแผนในข้อความ dialog เดิม (เวอร์ชันเดิม → ใหม่, เหตุผล, ความมั่นใจ, UNVERIFIED) โดย **ไม่มีปุ่มที่เปลี่ยนไฟล์**
5. Unit test สำหรับตัวแปลงและการแมป, แล้วทดสอบบน LDPlayer กับ QA-MD-1 โดยไม่แตะข้อมูลม็อด
6. ห้าม Executor ใหม่ ห้ามดาวน์โหลด/ติดตั้งจากแผน

ห้ามเริ่มงานนี้จนกว่าจะได้รับอนุมัติ
