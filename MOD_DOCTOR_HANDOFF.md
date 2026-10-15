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
- **commit แล้วใน `9866e19e`** (เดิมเป็นไฟล์ untracked ก่อน commit)

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

- **[แก้แล้วใน Phase B — ดูหัวข้อ 15, commit `c4e228aa`]** ข้อจำกัดด้านความปลอดภัยเดิม (ยืนยันจากโค้ดก่อนแก้):
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
- ไฟล์ใหม่ (commit `9866e19e`): `game/version/mod/ModGroupPlanner.kt`, `src/test/.../ModGroupPlannerTest.kt`, `MOD_DOCTOR_HANDOFF.md`
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

## 12. Phase A — ผลล่าสุด (2026-10-12)

**ทำแล้ว (current):**
- `ModGroupPlanner.kt`: เพิ่มโหมด **discovery** (`planModGroupDiscovery`) สำหรับ Minecraft/loader ใดก็ได้ที่ไม่มีกฎ known-good ตรงกัน เสนอเฉพาะสมาชิกที่เวอร์ชันปัจจุบันไม่รองรับเป้าหมาย, ผลสูงสุด MEDIUM และระบุว่าไม่ใช่ known-good
- `planModGroupRepair` (known-good) ยังทำงานเดิม ไม่แก้กฎ `mod_doctor_rules.json`
- dependency ของ Modrinth ที่แมปกับม็อดในเครื่องไม่ได้ → UNVERIFIED (ไม่เดาว่ามีหรือไม่มี)
- `ModDoctor.kt`: `ModDoctorReport.groupPlan` + `withGroupPlan` / `buildGroupPlan` / `toGroupCandidate` อ่านข้อมูลจาก Modrinth อย่างเดียว ทำงานเฉพาะเมื่อปัญหาเกี่ยวกับม็อดในกลุ่ม Sodium
- `AllLaunchDialogs.kt`: แสดงส่วน "แผนซ่อมกลุ่ม (แสดงอย่างเดียว)" ใน dialog เดิมของ ALL พร้อมระบุว่าปุ่มแก้ไขยังทำงานแบบเดิม
- strings EN/TH สำหรับส่วนแผน

**ทดสอบ:**
- Unit test ทั้ง package `game.version.mod`: **266 tests, 0 failures, 0 errors** (รวม ModGroupPlannerTest 20 tests)
- Build `assembleDebug -Darch=x86_64`: PASS (APK SHA-256 `39b37d5bfec383f1f9aab3b8279702ad229a8f9d949928aecfbdce7cabec2de7`)
- LDPlayer 960×540 density 160, QA-MD-1: ย้าย `sodium` ออกไปยังโฟลเดอร์สำรอง → กดเล่น → Mod Doctor พบ MISSING_REQUIRED_DEPENDENCY และ dialog แสดงส่วนแผน: **UNVERIFIED** เพราะ Sodium Extra ต้องการโปรเจกต์ AANobbMI ที่ไม่ได้ติดตั้ง (ผลถูกต้องตามข้อมูล) → กดยกเลิก → คืน `sodium` กลับตำแหน่งเดิม (ไม่ลบ)

**ยังไม่ได้ทดสอบ (UNVERIFIED):**
- การแสดงแผน PLAN / NO_CHANGE บนอุปกรณ์จริง (ต้องมีกรณีที่ข้อมูลครบ)
- Minecraft/loader อื่นนอกจาก 1.21.11 Fabric บนอุปกรณ์
- Known-good path ผ่านแผนบนอุปกรณ์
- Physical iQOO
- ปุ่มแก้ไขยังคงเป็นกลไกเดิม ไม่ได้ใช้แผนนี้ (ตามที่ออกแบบ)

**ข้อจำกัดที่ค้างอยู่:**
- การค้นหาใช้ `lookupInstalledMod` ทุกม็อดในเครื่องเมื่อมีปัญหาในกลุ่ม → อาจช้า
- การดึงเวอร์ชันของผู้สมัครทำเมื่อเปิดเกมเท่านั้น ไม่มี cache
- ยังไม่มี Backup/Rollback (Phase B)
- ความเสี่ยงการลบไฟล์ใน `ModDoctorMultiModRepair` (ประมาณบรรทัด 918) ยังไม่ได้แก้

## 13. Phase A — QA หลัง commit `bf7d3ed9` (2026-10-12) — ผลจริง: พบข้อบกพร่อง

**สถานะ Phase A = PARTIAL (ไม่ใช่ PASS):** UI แสดงแผนได้และ Planner ไม่แก้ไฟล์ แต่ **การจับคู่เวอร์ชันกับข้อมูล Modrinth จริงผิด** จึงห้ามอ้างว่า Dynamic Compatibility ผ่าน E2E

**หลักฐาน:** `test-artifacts/mod-plan-qa-20261012/01-nochange-dialog.png` — QA-MD-1 + ไฟล์สำเนา `sodium` เพื่อให้เกิดปัญหาม็อดซ้ำ (DUPLICATE ที่เกี่ยวกับกลุ่ม Sodium) → dialog แสดงแผนว่า:
- `sodium: 0.8.14+mc1.21.11 → mc1.21.11-0.8.15-beta.1-fabric` เหตุผล "เวอร์ชันเดิมไม่รองรับ fabric 1.21.11" — **ผิด** (0.8.14 รองรับ 1.21.11 อยู่แล้ว)
- `sodium-extra: 0.9.4+mc1.21.11 → mc1.21.11-0.9.4+fabric` — **ผิด** (เป็นเวอร์ชันเดียวกัน)

**สาเหตุ (จากโค้ด):**
1. `planModGroupDiscovery` เทียบเวอร์ชันด้วย `versionName.baseModVersion() == ...` แต่ `PlatformVersion.platformVersion()` ของ Modrinth คืนชื่ออย่าง `mc1.21.11-0.8.14-fabric` ส่วนเวอร์ชันใน jar เป็น `0.8.14+mc1.21.11` จึงไม่เคยเท่ากัน → ทุกสมาชิกถูกมองว่า "เวอร์ชันเดิมไม่รองรับ"
2. `planModGroupRepair` (เส้นทาง known-good) กรองด้วย `baseModVersion()` เท่ากันเช่นกัน → กับข้อมูล Modrinth จริงจะไม่พบผู้สมัครและกลายเป็น UNVERIFIED เสมอ (ยังไม่ได้ทดสอบบนเครื่อง แต่ตรรกะเดียวกัน) ขณะที่ Fix เดิมใช้ `matchesModVersion` ซึ่งทนต่อรูปแบบนี้
3. discovery เลือก "ใหม่สุด" ด้วยการเรียงตัวเลข โดยไม่สนใจประเภทรีลีส จึงเสนอ **beta** (`0.8.15-beta.1`) แทน release ที่เสถียร

**ทำไม unit test ไม่เจอ:** test ป้อนชื่อเวอร์ชันแบบง่าย (`0.8.12`) ที่ตรงกันตรง ๆ ไม่เคยใช้รูปแบบจริงของ Modrinth

**ผลทดสอบ QA ที่ต้องการ:**
- PLAN บนเครื่อง: แสดงได้ แต่เป็น **false positive** (ข้อบกพร่องข้างต้น) → ไม่นับเป็น PASS
- NO_CHANGE บนเครื่อง: **ยังไม่ได้เห็น** เพราะข้อบกพร่องเดียวกันทำให้ discovery ไม่คืน NO_CHANGE
- ไม่มีการแก้ source ในรอบนี้ ไม่มีการรัน test/build ซ้ำ (source ไม่เปลี่ยน)
- ความปลอดภัย: ไม่แตะ main instance, ไม่ลบม็อด (ไฟล์ fixture ถูกย้ายไปโฟลเดอร์สำรอง), ไม่แตะ Warehouse/มือถือจริง

**NEXT ACTION (งานเดียว):** แก้ Planner ให้จับคู่เวอร์ชันด้วย `matchesModVersion` (ชื่อ Modrinth ↔ เวอร์ชันใน jar), เพิ่มประเภทรีลีสให้ผู้สมัครและเลือก release ก่อน beta, เพิ่ม unit test ด้วยรูปแบบชื่อจริงของ Modrinth (`mc1.21.11-0.8.14-fabric`, `0.8.14+mc1.21.11`), แล้วทดสอบ NO_CHANGE และ PLAN ซ้ำบน QA-MD-1 แบบ Plan only ห้ามแตะระบบซ่อมจริง

## 14. Planner Bugfix — ผลล่าสุด (2026-10-12) — commit `a87f198d`

**สถานะ:** แก้บั๊กจากหัวข้อ 13 แล้ว ผล **PASS** ในขอบเขตที่ทดสอบ (NO_CHANGE และ PLAN บน QA-MD-1) แต่ Dynamic Compatibility ยัง **PARTIAL** ทั้งระบบ (ดูข้อจำกัดด้านล่าง)

**แก้อะไร (`ModGroupPlanner.kt`, `ModDoctor.kt`):**
1. `candidateMatchesVersion(ชื่อ Modrinth, เวอร์ชันใน jar)`: ตัดเมตาดาต้า `+build`, หาเวอร์ชันเป็น "ชิ้นเต็ม" (0.8.14 ≠ 0.8.140 ≠ 1.0.8.14), และ **ไม่จับ prerelease เป็น stable** (`0.8.14` ไม่ตรง `…0.8.14-beta.1…`)
2. `GroupPlanReleaseType` จาก `platformReleaseType()` ของ Modrinth จริง; `bestCandidate` เลือก RELEASE ก่อน BETA ก่อน ALPHA ก่อน UNKNOWN แล้วค่อยเลือกเวอร์ชันสูงสุด; ถ้ามีแต่ beta จะเสนอพร้อมโน้ตและความเสี่ยง "ไม่ใช่ stable"
3. `GroupPlanInstalled.projectId`: ผู้สมัครที่มาจากโปรเจกต์อื่นถูกทิ้ง ตรวจ Minecraft/loader เดิมต่อไป
4. สมาชิกที่เวอร์ชันปัจจุบันรองรับอยู่แล้วจะไม่ถูกเสนอเปลี่ยน แม้ชื่อเวอร์ชันของ Modrinth จะรูปแบบต่างกันหรือมี beta ใหม่กว่า
5. กฎ known-good ใช้ตัวจับคู่เดียวกัน (เดิมจะ UNVERIFIED เสมอกับข้อมูลจริง)

**ทดสอบ:**
- Unit test package `game.version.mod`: **274 tests, 0 failures, 0 errors** (ModGroupPlannerTest 28 tests รวม regression ด้วยชื่อจริง `mc1.21.11-0.8.14-fabric` ↔ `0.8.14+mc1.21.11`, stable/beta, prerelease-ไม่ใช่-stable, prefix, project คนละตัว, known-good เลือก stable)
- Build x86_64: PASS (APK SHA-256 `4a11f88fc154bba39221f33821cbbfe3f2a312cac8675c7400fbd5db7fb8182b`)
- LDPlayer 960×540 density 160, QA-MD-1:
  - **NO_CHANGE ✓** (`10-bugfix-nochange.png`): sodium ซ้ำ (DUPLICATE) → แผนบอก "กลุ่มนี้ไม่ต้องเปลี่ยนอะไร" ไม่เสนอเปลี่ยน 0.8.14 อีก
  - **PLAN ✓** (`21-bugfix-plan.png`, `22-bugfix-plan-scrolled.png`): ใส่ Sodium 0.7.3 (release สำหรับ 1.21.10, SHA-1 ตรง Modrinth `1c3ffa86…`) → แผน `sodium 0.7.3+mc1.21.10 → mc1.21.11-0.8.14-fabric` (stable ไม่ใช่ beta 0.8.15) เหตุผล "เวอร์ชันเดิมไม่รองรับ… เลือก release ที่ใหม่สุดที่รองรับ" ความมั่นใจ ปานกลาง ไม่ใช่ Known-good Set และเตือนว่ายังไม่ได้ทดสอบเปิดเกม
  - ยกเลิก dialog ทุกครั้ง ไม่กดแก้ไข; ย้ายไฟล์ fixture กลับ/ออก ไม่ลบ; sodium ต้นฉบับ SHA-1 `4a8733fe…` ตรงเดิม; main instance = fabric-api เท่านั้น; FATAL=0

**ยัง UNVERIFIED / ข้อจำกัด:**
- เส้นทาง known-good ผ่านข้อมูล Modrinth จริงบนเครื่อง (ต้องมีครบสี่ม็อด)
- Minecraft/loader อื่นนอกจาก 1.21.11 Fabric บนเครื่อง
- ผลลัพธ์ PLAN ยังไม่ได้ทดสอบว่าเปิดเกมจริงได้ (Plan only; ไม่มี executor)
- Physical iQOO
- ปุ่มแก้ไขยังเป็นกลไกเดิม ไม่ได้ใช้แผนนี้
- ความเสี่ยงการลบไฟล์ใน `ModDoctorMultiModRepair` (~บรรทัด 918) ยังไม่ได้แก้
- ค่าใช้จ่ายตอนเปิดเกม: จับคู่ลายนิ้วมือทุกม็อดเมื่อมีปัญหาในกลุ่ม

**NEXT ACTION (งานเดียว):** ขออนุมัติ commit งานแก้บั๊กนี้ (source 3 ไฟล์ + เอกสาร 2 ไฟล์) แล้วตัดสินใจว่าจะเริ่ม Phase B (แก้ความเสี่ยง `delete()` และ Backup/Rollback) หรือทดสอบ known-good path บนเครื่องก่อน

## 15. Phase B — File Safety + Persistent Backup/Rollback (2026-10-13) — commit `c4e228aa` (PARTIAL)

**สถานะ: PARTIAL.** ระบบ snapshot/rollback ผ่าน unit test และผ่านเส้นทาง success + restart + user rollback บนเครื่อง แต่เส้นทาง collision/partial failure/ขนาดเกินกำหนดบนเครื่อง และ `ModDoctorMultiModRepair.apply` ผ่านเครื่องมือจริง ยัง UNVERIFIED

**แก้อะไร:**
1. `ModRepairSnapshot.kt` (ใหม่, JVM-testable): `ModRepairSnapshots.create/finalize/latestRollbackable/rollback` เก็บใน `<instance>/ALLLauncher/repair-backups/<id>/` (`manifest.json` + `files/` + `quarantine/`)
   - **ก่อนซ่อม:** คัดลอกไฟล์ระดับบนทุกไฟล์ของโฟลเดอร์ `mods` (รวม `.disabled`) แล้วตรวจ SHA-1 ของสำเนา ถ้าคัดลอกไม่ครบ/ไม่ตรง/ขนาดรวมเกิน 1 GiB → **ไม่เริ่มซ่อม** (ไม่แตะไฟล์)
   - **manifest:** id, เวลา, เส้นทาง, รายการก่อนแก้ (ชื่อ+SHA-1+ขนาด), สถานะ (PENDING/APPLIED/PARTIAL/NO_CHANGE/INTERRUPTED/ROLLED_BACK/ROLLED_BACK_PARTIAL), รายการหลังแก้, conflicts, quarantined; เขียนแบบ tmp→move
   - **เปิดแอปใหม่:** manifest ที่ยังเป็น PENDING (แอปถูกปิดกลางซ่อม) ถูกอ่านเป็น INTERRUPTED แล้วย้อนกลับได้
   - **rollback:** ไม่ลบ ไม่เขียนทับ — ไฟล์ที่ต้องย้ายออกถูกย้ายไป `quarantine/` เท่านั้นเมื่อ SHA-1 ปัจจุบันตรงกับที่บันทึกไว้หลังซ่อม; ไฟล์ที่ผู้ใช้เปลี่ยนภายหลังถูกข้ามและรายงานเป็น conflict; ไฟล์เดิมคืนจากสำเนาพร้อมตรวจ SHA-1 และไม่คืนทับไฟล์ที่มีอยู่
2. `ModDoctor.kt`:
   - `fixProblems` ครอบด้วย snapshot (`fixProblemsUnprotected` คือโค้ดเดิม) และ finalize ใน `finally`
   - `preserveSafely`: ปฏิเสธเมื่อ `.disabled` ถูกใช้อยู่แล้ว (เดิม `disableModFile` ใช้ REPLACE_EXISTING เขียนทับเงียบ ๆ) แทนการเรียก `disableModFile()` ตรง ๆ ที่ 3 จุดซ่อม
   - `ModDoctorMultiModRepair.apply`: **แทน `target.delete()`** ด้วยการเก็บไฟล์ที่ชื่อชนกันเป็น `.disabled` (ถ้าเก็บไม่ได้หยุด) และเพิ่มเข้า list ของ rollback อัตโนมัติเดิม
   - `rollbackLastRepair`, `hasRollbackableRepair` สำหรับ UI
3. `all/mods/AllModsWindow.kt`: ปุ่ม "ย้อนกลับการซ่อมล่าสุด" (แสดงเฉพาะเมื่อมีการซ่อมที่ย้อนกลับได้) → dialog ยืนยัน → dialog ผลลัพธ์; strings EN/TH
4. **ไม่ได้** เชื่อม Dynamic Group PLAN เข้ากับปุ่ม Fix (ตามคำสั่ง) และไม่ได้สร้าง executor ใหม่

**ทดสอบ:**
- Unit test package `game.version.mod`: **287 tests, 0 failures, 0 errors** (`ModRepairSnapshotTest` 13 tests: สร้าง+manifest, ปฏิเสธเมื่อเกินขนาด, NO_CHANGE, rollback ไม่ลบ/ย้ายไป quarantine, ข้ามไฟล์ที่ผู้ใช้แก้, ไม่เขียนทับไฟล์ชื่อซ้ำเนื้อหาต่าง, partial failure ยังย้อนกลับได้, interrupted หลังเปิดแอปใหม่, อ่านจากดิสก์, ไม่เสนอซ้ำหลังย้อนกลับ, collision ยังกู้ไบต์เดิมได้, `preserveSafely` ปฏิเสธ/ทำงานตามคาด)
- Build x86_64 PASS, APK SHA-256 `c8fe2e4094f4c9185ab3edf4875e676a7cd736d7c451a1c330e0599594e9575b`
- LDPlayer 960×540 density 160 กับ QA-MD-1 (`test-artifacts/mod-backup-qa-20261013/`):
  - **success:** ย้าย sodium ออก → Play → พบ MISSING_REQUIRED_DEPENDENCY → กดแก้ไข (กลไกเดิม) → พบโฟลเดอร์ snapshot ถูกสร้างก่อนแก้ → ติดตั้ง sodium 0.8.14 (SHA-1 `4a8733fe…` ตรง Modrinth) → re-check 0 problems → manifest `APPLIED` (before 2 ไฟล์, after 3 ไฟล์)
  - **restart:** `force-stop` แอป เปิดใหม่ → ปุ่มย้อนกลับปรากฏในหน้าต่าง Mods
  - **rollback:** ยืนยัน → ผล "คืนไฟล์ 0 ย้ายไปเก็บ 1 ข้าม 0" → `mods` กลับเป็น 2 ไฟล์; sodium ที่ซ่อมเพิ่มอยู่ใน `quarantine/` (SHA-1 ตรง, ไม่ถูกลบ); manifest `ROLLED_BACK`, conflicts `[]`
  - **คืนสภาพ:** ใส่ sodium ต้นฉบับกลับ → รายการ SHA-1 ของ QA-MD-1 ตรงกับก่อนทดสอบทุกไฟล์; main instance ไม่ถูกแตะ; FATAL=0; density 160

**ยัง UNVERIFIED / ความเสี่ยงที่เหลือ:**
- เส้นทาง **collision/partial failure จริงใน `ModDoctorMultiModRepair.apply`** (ต้องใช้ Android + staged ไฟล์) — มีเฉพาะ unit test ของ snapshot/rollback และ `preserveSafely`; ยังไม่มี unit test ของ `apply()` เอง
- การปฏิเสธเมื่อเกิน 1 GiB และ conflict (ผู้ใช้แก้หลังซ่อม) บนเครื่องจริง — ผ่าน unit test เท่านั้น
- instance ที่ mods เกิน 1 GiB จะซ่อมอัตโนมัติไม่ได้ (ปฏิเสธอย่างปลอดภัย)
- ไฟล์ที่ pipeline ดาวน์โหลดเดิมของ Zalith เขียน (ติดตั้ง dependency / replacementFix ใช้ `downloadSingleForVersions`) อาจเขียนทับชื่อซ้ำเอง — ไม่ได้แก้ (ห้ามแตะ backend) แต่ snapshot ครอบคลุมการย้อนกลับได้
- `latestRollbackable` ถือ manifest PENDING เป็นถูกขัดจังหวะ — ถ้า UI อ่านระหว่างซ่อมกำลังทำงานจะถูกมองเป็น INTERRUPTED (ยังไม่มีล็อกกันซ้อน)
- ปุ่มย้อนกลับอยู่ในหน้าต่าง Mods ของ ALL เท่านั้น (ไม่มีใน dialog ปัญหา)
- Physical iQOO: ไม่ได้ทดสอบ

**NEXT ACTION (งานเดียว):** ขออนุมัติ commit Phase B นี้ แล้วเขียน unit test ให้ `ModDoctorMultiModRepair.apply` (collision + partial failure จริง) เพื่อพิสูจน์เส้นทางซ่อมกับ snapshot ก่อนจะเชื่อม Dynamic Group PLAN เข้าปุ่ม Fix

## 16. Phase B Safety Closure (2026-10-13) — commit `7a424199`

**Phase B commit:** `c4e228aa` (สถานะ PARTIAL) — งานด้านล่างเป็นงานเพิ่มหลัง commit นั้น และ commit ใน `7a424199`

**แก้/เพิ่มอะไร:**
1. **ตัวล็อกต่ออินสแตนซ์** `ModRepairLock` (ในหน่วยความจำ): `fixProblems` และ `rollbackLastRepair` ต้องได้ล็อกก่อน — ซ้อนกันไม่ได้ (Fix ซ้อน → `REPAIR_BUSY`; Rollback ซ้อน → ไม่เริ่ม)
2. `latestRollbackable` คืน null ขณะล็อกถูกถือ → **ไม่ตีความ PENDING ของการซ่อมที่กำลังทำงานเป็น INTERRUPTED** และไม่เขียน manifest; ปุ่มย้อนกลับถูกซ่อนระหว่างซ่อม; `isRepairBusy` สำหรับ UI (ข้อความ "กำลังมีการซ่อม…")
3. `rollbackLastRepair` ใช้ `latestRollbackableLocked` (เพราะถือล็อกเอง)
4. **Unit test ของ `apply()` จริง** (ใน `ModDoctorMultiModRepairTest`, ใช้ harness เดิม): ไฟล์ชื่อชนกับเป้าหมายถูกเก็บเป็น `.disabled` ไม่ถูกลบ (SHA-1 ตรง); `.disabled` ของต้นฉบับถูกใช้อยู่แล้ว → ปฏิเสธและไม่มีไฟล์เปลี่ยน; ล้มหลังเก็บไฟล์ชนกัน → rollback คืนทุกไฟล์ byte-for-byte (partial failure ผ่านเดิมจาก `applyFailureRollsBackCompletely`)
5. Unit test ล็อก + กู้หลังแอปถูกปิดกลางซ่อมโดยไม่ทับไฟล์ที่ผู้ใช้แก้ (ใน `ModRepairSnapshotTest`)
6. ไม่มี executor ใหม่ ไม่ได้เชื่อม Dynamic PLAN กับปุ่ม Fix

**ผลทดสอบ:**
- Unit test package `game.version.mod`: **293 tests, 0 failures, 0 errors** (`ModRepairSnapshotTest` 16, `ModDoctorMultiModRepairTest` 17)
- Build x86_64 PASS, APK SHA-256 ดูที่ `test-artifacts/mod-safety-qa-20261013/apk.txt`
- LDPlayer 960×540 density 160, QA-MD-1 (`test-artifacts/mod-safety-qa-20261013/`):
  - **แอปถูกปิดกลางซ่อม:** กดแก้ไข → snapshot สร้างทันที (PENDING) → `force-stop` ที่ ~4 วินาที → เปิดแอปใหม่ → manifest อ่านเป็น `INTERRUPTED` และปุ่มย้อนกลับปรากฏ → กดย้อนกลับ ผล "คืน 0 / ย้ายไปเก็บ 0 / ข้าม 0" ไฟล์ทุกไฟล์คงเดิม (SHA-1 ตรง) **ข้อจำกัดของการทดสอบ:** ครั้งนี้การปิดเกิดก่อนที่ไฟล์ใดจะถูกเปลี่ยน จึงยังไม่ได้พิสูจน์ "ปิดหลังไฟล์เปลี่ยนไปแล้ว" บนเครื่อง (ครอบคลุมโดย unit test `interruptedRepairCanBeRolledBackAfterRestart`)
  - **ไฟล์ที่ผู้ใช้แก้หลังซ่อม:** ซ่อมจนเสร็จ (`APPLIED`) → ผมต่อท้ายไฟล์ sodium ที่ติดตั้ง (SHA-1 เปลี่ยนเป็น `22637f5e…`) → ย้อนกลับ → "ข้าม 1 ไฟล์เพราะมีการเปลี่ยนหลังซ่อม", ไฟล์ยังอยู่และไม่เปลี่ยน, manifest `ROLLED_BACK_PARTIAL` พร้อมชื่อไฟล์ใน `conflicts`
  - **คืนสภาพ:** QA-MD-1 กลับเป็นสถานะเดิม (รายการ SHA-1 ตรงกับก่อนทดสอบทุกไฟล์); main instance ไม่ถูกแตะ; FATAL=0; density 160

**ยัง UNVERIFIED / ความเสี่ยงที่เหลือ:**
- การซ่อมซ้อนกันจริงบนเครื่อง (ล็อกผ่าน unit test เท่านั้น — ไม่มีทางกด Fix สองครั้งพร้อมกันได้ง่ายใน UI)
- ปิดแอปหลังไฟล์ถูกเปลี่ยนแล้วบนเครื่อง (ดูข้อจำกัดข้างต้น)
- ปฏิเสธเมื่อ mods เกิน 1 GiB บนเครื่อง
- ล็อกอยู่ในหน่วยความจำของโปรเซสหลักเท่านั้น (ไม่กันสองโปรเซส)
- pipeline ดาวน์โหลดเดิมของ Zalith (ติดตั้ง dependency / replacementFix) ยังอาจเขียนทับชื่อซ้ำเอง (ไม่ได้แตะ backend; snapshot ย้อนกลับได้)
- ปุ่มย้อนกลับมีเฉพาะในหน้าต่าง Mods ของ ALL
- Physical iQOO ไม่ได้ทดสอบ

**NEXT ACTION (งานเดียว):** ขออนุมัติ commit งานรอบนี้ (7 ไฟล์ + เอกสาร) แล้วค่อยตัดสินใจเชื่อม Dynamic Group PLAN เข้ากับปุ่ม Fix ผ่าน executor เดิม โดยมี snapshot/rollback คุ้มครอง

## 17. Rule Engine First (2026-10-13) — commit `04b3dec2`

**Safety QA commit:** `7a424199` (ไม่ทำซ้ำ) — งานด้านล่างเป็นงานรอบใหม่ และ commit ใน `04b3dec2`

**สถานะ:** Rule Engine รองรับหลายชุด/หลาย loader/กฎซ้อนกันแล้ว ผ่าน unit test และ build แต่ **ยังไม่ได้ทดสอบบนอุปกรณ์** (ไม่ได้ติดตั้ง APK รอบนี้ เพราะกฎเดิมพิสูจน์ด้วย test เดิม และไม่มีกฎใหม่ที่ยืนยันแล้วเพิ่ม) และ **ไม่มีกฎ Known-good ใหม่** — มีเฉพาะชุด 1.21.11 Fabric เดิม

**reuse ได้ (ไม่สร้างซ้ำ):** `ModDoctorSetRule`/`parseModDoctorRules`/`ModDoctorRules.load`/`evaluateSetRules` (ModDoctorRules.kt), `validateModDoctorMetadata(setRules=…)`, `matchesModVersion`/`baseModVersion`, `ModDoctorSetFix` + `repairKnownSet` (ตัวซ่อมเดิม), `planModGroupRepair` (Planner เดิม), `ModDoctorSetRulesTest` เดิม 10 ตัว (ผ่านโดยไม่แก้)

**เปลี่ยนอะไร:**
1. **โมเดลกฎ** (`ModDoctorSetRule`) เพิ่มฟิลด์ที่มีค่าเริ่มต้นเข้ากันได้: `id`, `mode` (`ENFORCE` | `KNOWN_GOOD`), `verified`, `priority`, `evidence`
2. **Known-good ≠ ใช้ชุดอื่นไม่ได้:** กฎโหมด `KNOWN_GOOD` ถ้าไม่ตรง → **ไม่รายงานปัญหา ไม่บังคับเปลี่ยน/ลดรุ่น** (เป็นแค่แหล่งคำแนะนำให้ Planner, `GroupPlan.advisoryOnly=true`) กฎโหมด `ENFORCE` (พฤติกรรมเดิมของชุด Sodium 4 ตัว ซึ่งมีหลักฐานว่าเกินกฎแล้วถูกโหลดเดอร์ปฏิเสธ) ยังรายงานและซ่อมเหมือนเดิม
3. **ข้อมูลที่ยืนยันแล้ว vs คำแนะนำ:** เฉพาะ `verified` เท่านั้นที่ถูกใช้ (engine, family ของ Planner, Planner) กฎที่ไม่มี `verified:true` เป็น "candidate" ไม่มีผลและไม่เป็น Known-good ผลที่มาจาก Modrinth (discovery) ยังคงเป็นอีกชั้นแยก (`fromKnownGoodRule=false`, ความมั่นใจสูงสุด MEDIUM)
4. **ความเข้ากันได้ย้อนหลังของ JSON:** `rules_version` ≤ 1 → ทุกกฎ = verified + ENFORCE (ไฟล์เดิมไม่ต้องแก้ ไม่เปลี่ยนพฤติกรรม) `rules_version` ≥ 2 → ต้องเขียน `"verified": true` เอง (ไม่เขียน = candidate) และ `mode` ไม่เขียน/ไม่รู้จัก = `KNOWN_GOOD` (ไม่บังคับ)
5. **กฎซ้อนกัน — วิธีเลือกที่แน่นอน** (`selectApplicableRules`): (a) เฉพาะ verified ที่ Minecraft + loader ตรง และ `when_present` ติดตั้งครบ (b) ขอบเขตกว้างกว่า (superset จริง) ชนะและบังกฎแคบกว่า (shadowed) (c) ขอบเขตเท่ากัน: `priority` สูงกว่าชนะ; priority เท่ากันแต่ค่าต่างกัน → ambiguous ไม่ใช้ทั้งคู่ (d) ขอบเขตต่างแต่กำหนดโมดเดียวกันคนละเวอร์ชัน → ambiguous ไม่ใช้ทั้งคู่ (ไม่เดา) กฎซ้ำที่เนื้อหาเหมือนกันเหลือหนึ่ง
6. **Planner** ใช้ตัวเลือกกฎเดียวกัน (เลือกชุดที่ครอบคลุมมากสุด) และ family ใช้เฉพาะ verified
7. `mod_doctor_rules.json`: เพิ่มเฉพาะ `id`, `mode:"enforce"`, `evidence` ให้กฎเดิม **ค่าเวอร์ชันทั้งสี่ไม่เปลี่ยน** (ตรวจด้วยสคริปต์) `rules_version` ยังเป็น 1

**ทดสอบ:**
- Unit test package `game.version.mod`: **309 tests, 0 failures, 0 errors** (`ModDoctorRuleEngineTest` ใหม่ 16: ไฟล์เดิมคงค่าและ semantics, v1 ไม่มีฟิลด์ใหม่, v2 ต้อง verified และ mode ไม่รู้จัก=KNOWN_GOOD, แยกตาม Minecraft/loader (fabric/forge/neoforge), ชุดอิสระใช้พร้อมกัน, superset บังกฎแคบ, priority, ambiguous, ซ้ำเหมือนกัน, overlap ที่ไม่ขัดกัน, unverified ไม่มีผลและไม่บังกฎอื่น, ข้อมูลไม่ครบเงียบ, KNOWN_GOOD ไม่รายงาน/ไม่บังคับ, ENFORCE ยังรายงานเฉพาะสมาชิกที่ต่าง, overlap ไม่รายงานซ้ำ, Planner ละ unverified และตั้ง advisoryOnly) `ModDoctorSetRulesTest` เดิม 10 ตัว และ `ModGroupPlannerTest` 28 ตัวผ่านไม่แก้
- Build x86_64: PASS (APK SHA-256 `c506310dddfb889f59ac5c39cdbd4ecd1fa900a510131c3360b63da777e5a496`) — **ไม่ได้ติดตั้ง/ทดสอบบนเครื่อง**

**ความเสี่ยงและส่วนที่ยังไม่ทำ:**
- ไม่มีกฎใหม่ (ไม่มีหลักฐานการทดสอบจริง) — Known-good สำหรับ Minecraft/loader อื่นยังไม่มีจนกว่าจะมี fixture ที่ทดสอบจริง
- ยังไม่มีตัวกำหนด Known-good จากผลทดสอบจริงอัตโนมัติ (ตั้งใจ: ต้องเขียนกฎเองพร้อมหลักฐาน)
- Planner ใช้ได้ทีละกฎ (เลือกชุดที่ใหญ่สุด); หลายกฎอิสระยังไม่ถูกรวมเป็นแผนเดียว
- `minecraft` ในกฎเป็นเวอร์ชันเดียว (ไม่มี range/wildcard)
- กฎ `KNOWN_GOOD` ยังไม่ปรากฏใน UI (เป็นแค่ข้อมูลให้ Planner)
- Dynamic PLAN ยังไม่เชื่อมกับปุ่ม Fix; ไม่มี AI; ไม่มี executor ใหม่; ไม่แตะ Download UI

**NEXT ACTION (งานเดียว):** ขออนุมัติ commit งาน Rule Engine นี้ (4 ไฟล์ที่แก้ + test ใหม่ + เอกสาร) แล้วตัดสินใจว่าจะหา fixture จริงสำหรับ Minecraft/loader อื่นเพื่อเพิ่ม Known-good หรือจะเชื่อม PLAN เข้ากับ Fix ผ่าน executor เดิมก่อน

## 18. Final Rule Safety Check (2026-10-13) — ผลการตรวจหลักฐานกฎ Sodium 4 ตัว

**ข้อสรุป:** หลักฐานของกฎนี้คือ "**ชุดนี้เปิดเกมได้จริง**" (ถึง MAIN_MENU บน LDPlayer: ทดสอบ B และ C-confirm ใน checkpoint "MOD DOCTOR SIMPLE RULES V1") **ไม่ใช่** "ชุดอื่นที่ตรงเงื่อนไขจะล้มเหลว" จึงเปลี่ยนจาก `ENFORCE` เป็น `KNOWN_GOOD`

**หลักฐานที่ตรวจ (ไม่ขัดแย้งกัน):**
- การปฏิเสธจริงของ Fabric Loader ที่มี log: `NEG_HARD_DEP sodium 0.8.14+mc1.21.11 {breaks iris @ [<=1.10.7]}` กับ Iris 1.10.7 (checkpoint ~บรรทัด 5440-5500, "MOD_DOCTOR_BREAKS_AND_THAI_V1") — เป็นคู่ที่ jar ประกาศเอง ตรวจด้วย `breaks` ของ metadata (`INCOMPATIBLE_MOD_PAIR`) ซึ่งทำงานแยกจากกฎชุดอยู่แล้ว
- กฎชุด 4 ตัวถูกเพิ่มเป็นทางเลือกซ่อม (option A) ของกราฟที่ BLOCKED ไม่ได้มาจากหลักฐานว่าชุดอื่นถูกปฏิเสธ; ความเห็น "加载器拒绝" ในโค้ดเป็นสมมติฐาน ("可能") ไม่มีเคสที่ metadata ผ่านแต่ loader ปฏิเสธ
- การทดสอบ C (ชุดใหม่สุดถูกตรวจพบทั้ง PAIR และ SET) — ชุดนั้นมีคู่ sodium 0.8.14 + iris 1.10.7 ที่ถูกปฏิเสธอยู่แล้วโดย `breaks`

**ทำอะไร:**
1. `mod_doctor_rules.json`: `rules_version` 2, `verified: true`, `mode: "known_good"`, `evidence` ระบุว่ายืนยันแค่ว่าชุดเปิดเกมได้ และการปฏิเสธที่พิสูจน์แล้วอยู่ที่ `breaks` **ค่าเวอร์ชันทั้งสี่ไม่เปลี่ยน** (sodium 0.8.12, sodium-extra 0.9.1, reeses-sodium-options 2.2.3, iris 1.10.7; ตรวจด้วยสคริปต์ก่อนเขียน)
2. ไม่มีการแก้โค้ด engine/Planner เพิ่ม (ใช้ความสามารถที่ทำไว้ในหัวข้อ 17)
3. Regression test ใหม่ (`ModDoctorRuleEngineTest`): ไฟล์ที่ส่งมอบเป็น KNOWN_GOOD+verified และคงค่าเดิม; ชุดที่ไม่เคยยืนยัน (sodium 0.8.15 / extra 0.9.4 / reese 2.2.4 / iris 1.10.8 ฯลฯ) **ไม่ถูกรายงานว่าใช้ไม่ได้** เพราะไม่ตรง Known-good; ชุด Known-good เองสะอาด; คู่ `breaks` ที่ loader ปฏิเสธจริงยัง **บล็อกอยู่** (INCOMPATIBLE_MOD_PAIR) และกฎชุดไม่เพิ่มคำตัดสินที่ไม่มีหลักฐานซ้ำ

**ผลกระทบพฤติกรรม (ต้องรู้):**
- Mod Doctor **จะไม่รายงาน `INCOMPATIBLE_MOD_SET`** สำหรับชุด Sodium 4 ตัวอีกต่อไป และไม่เสนอ "ซ่อมให้เป็นชุด 0.8.12/0.9.1/2.2.3/1.10.7" อัตโนมัติ เส้นทาง `repairKnownSet` ยังอยู่ แต่จะทำงานเฉพาะเมื่อมีกฎ `ENFORCE` ที่มีหลักฐานการปฏิเสธ (ปัจจุบันไม่มี)
- ผลทดสอบเดิมบนเครื่องที่อาศัยปุ่มซ่อมตามกฎชุด (SIMPLE RULES V1 tests C และ D) เป็นหลักฐานเชิงประวัติ ตอนนี้กลไกนั้นไม่ถูกเรียกกับไฟล์ที่ส่งมอบ; ชุดที่มีคู่ `breaks` ยังถูกจับและซ่อมผ่านเส้นทาง breaks เดิม (`repairBreaksConflict`) — **ยังไม่ได้ทดสอบซ้ำบนเครื่อง** ว่าเส้นทางนี้ซ่อมกราฟเดียวกันได้ครบ

**ทดสอบ:** Unit test package `game.version.mod` **312 tests, 0 failures, 0 errors** (`ModDoctorRuleEngineTest` 19, `ModDoctorSetRulesTest` เดิม 10 และ `ModDoctorInstalledBreaksTest` เดิม 6 ผ่านโดยไม่แก้) ไม่ได้ build/ติดตั้งใหม่ (เปลี่ยนเฉพาะไฟล์ asset และ test; build ก่อนหน้านี้ผ่านแล้ว)

**NEXT ACTION (งานเดียว):** ตัดสินใจระหว่าง (ก) ทดสอบบน QA-MD-1 ว่ากราฟ Sodium/Iris ที่เคยซ่อมด้วยกฎชุดถูกซ่อมครบด้วยเส้นทาง `breaks` เดิมหรือไม่ หรือ (ข) หาหลักฐานทดสอบจริงของ Minecraft/loader อื่นเพื่อเพิ่ม Known-good

## 19. Targeted Breaks E2E QA (2026-10-13) — หยุดก่อนกด Fix: พบปัญหาในแผนที่แสดง (ไม่ได้แก้ source)

**APK ที่ใช้:** build ใหม่จาก HEAD `04b3dec2` (APK เดิมบนเครื่องยังเป็นกฎ ENFORCE แบบเก่า) — ยืนยันว่า APK มี `mod_doctor_rules.json` แบบ `rules_version 2`, `known_good`; SHA-256 อยู่ใน `test-artifacts/mod-breaks-e2e-20261013/apk.txt`

**fixture (LDPlayer, QA-MD-1):** sodium 0.8.14 (เดิม, `4a8733fe…`), sodium-extra 0.9.4 (เดิม), เพิ่ม iris 1.10.7 (`aae8567b…`) และ reeses-sodium-options 2.2.4 (`295623b7…`) จาก Modrinth (SHA-1 ตรง) metadata จริงยืนยัน sodium 0.8.14 ประกาศ `breaks iris <=1.10.7`

**ผล:**
| รายการ | ผล |
|---|---|
| ตรวจพบคู่ breaks | **PASS** — `inspect finished: 1 problem(s) [INCOMPATIBLE_MOD_PAIR]`; dialog ระบุ Sodium 0.8.14 ↔ Iris 1.10.7 และชื่อไฟล์ครบ |
| ไม่มี INCOMPATIBLE_MOD_SET | **PASS** — กฎชุดเป็น KNOWN_GOOD จึงไม่ตัดสินเพิ่ม |
| แผนซ่อมที่แสดงก่อนกด Fix | **FAIL** — เสนอลดรุ่น sodium 0.8.14→0.8.12, sodium-extra 0.9.4→0.9.1, reeses 2.2.4→2.2.3 พร้อมเหตุผล "ตรงกับกฎที่ยืนยันแล้ว" ความมั่นใจปานกลาง (`03-dialog.png`, `04/05-plan-scrolled*.png`) — ทั้งที่ปัญหาจริงคือคู่ sodium/iris และ Iris 1.10.8 (release) ที่แก้ได้โดยไม่ลดรุ่นมีอยู่บน Modrinth |
| Fix / Snapshot / Rescan / Launch / Rollback | **UNVERIFIED** — หยุดก่อนกด Fix ตามคำสั่ง (ข้อ 8) |
| คืนสภาพ | **PASS** — ย้าย fixture ออก (ไม่ลบ) SHA-1 ทั้ง 80 ไฟล์ของ QA-MD-1 ตรงกับก่อนทดสอบ; main instance ไม่เปลี่ยน; เกมไม่ถูกเปิด; FATAL=0 |

**Root cause (จากโค้ด, ยังไม่ได้แก้):**
1. `buildGroupPlan` (ModDoctor.kt) เลือกกฎ Known-good ทุกครั้งที่กฎใช้ได้ โดยไม่ดูว่าเป็น `KNOWN_GOOD` (คำแนะนำ) แล้วส่งให้ `planModGroupRepair` ซึ่งเสนอเปลี่ยน **ทุกสมาชิกที่ไม่ตรงกฎ** ไปเป็นเวอร์ชันของกฎ — ในโหมด KNOWN_GOOD ผลคือข้อเสนอลดรุ่นที่ไม่มีเหตุผล
2. Planner ไม่รู้จัก `breaks` ที่ jar ประกาศ (ดูเฉพาะ dependency/incompatible จาก Modrinth) จึงไม่เสนอทางแก้ขั้นต่ำของคู่ที่ขัดกันจริง (อัปเกรด iris → 1.10.8)
3. `GroupPlanPreview` (AllLaunchDialogs.kt) ไม่แสดง `notes` และไม่แสดง `advisoryOnly` ข้อความ "เป็นคำแนะนำเท่านั้น" จึงไม่ปรากฏ ผู้ใช้เห็นเป็นเหมือนแผนที่ต้องทำ

**ความเสี่ยงต่อผู้ใช้ตอนนี้:** แผนเป็น Plan only ปุ่ม Fix **ไม่ได้ใช้แผนนี้** (ใช้ `repairBreaksConflict` เดิม) จึงยังไม่มีการลดรุ่นอัตโนมัติจากแผน แต่ข้อความใน dialog ชี้นำผิด

**NEXT ACTION (งานเดียว):** แก้ Planner ไม่ให้เสนอเปลี่ยนตามกฎ KNOWN_GOOD (หรือเสนอเป็นทางเลือกพร้อมป้ายชัด) ให้ dialog แสดงสถานะคำแนะนำ แล้วกลับมาทดสอบ Fix เดิม/Snapshot/Rescan/Launch/Rollback ของคู่ breaks นี้บน QA-MD-1

## 20. Breaks E2E QA ต่อเนื่อง (2026-10-13) — ผล: PARTIAL (source commit `b3b71b38`)

แก้เฉพาะ Planner และ Dialog ตาม §19 แล้วทดสอบ QA-MD-1 ซ้ำ:

- Source: `04b3dec2` + `b3b71b38` (การแก้ Planner/Dialog) (`ModGroupPlanner.kt`, `ModDoctor.kt`, `AllLaunchDialogs.kt`, strings EN/TH, tests).
- Unit test `:ZalithLauncher:testDebugUnitTest --tests "...game.version.mod.*"`: BUILD SUCCESSFUL.
- APK `ALLLauncher-Debug-0.0.2-beta1.apk` build สำเร็จ, SHA-256 `82f6b8e0…a927f1` (หลักฐาน `test-artifacts/mod-breaks-e2e-complete-20261013/apk-sha256.txt`).
- ติดตั้ง: ครั้งแรกค้างที่ `Performing Streamed Install` → reboot LDPlayer (`ldconsole reboot --index 0`) → ติดตั้งสำเร็จ (lastUpdateTime 11:58:50).

### ผลที่ตรวจแล้ว (PASS)
- **Preview (Step 3):** แสดงสาเหตุจริง "sodium 0.8.14 ประกาศ breaks iris <=1.10.7 แต่ติดตั้ง 1.10.7" แผนแก้เฉพาะ `iris 1.10.7 → 1.10.8+1.21.11-fabric`, ความมั่นใจปานกลาง, หมายเหตุว่า Modrinth ไม่เปิดเผย breaks ของ jar ใหม่ และ Known-good แยกเป็น "ข้อเสนอ (ไม่บังคับ ไม่ใช่การซ่อม)" ไม่มีการเสนอ downgrade ของ Sodium Extra หรือ Reese's ในแผนหลัก (`06-plan-footer.png`, `05-plan-scrolled-text.png`).
- **ตรวจ Modrinth (Step 6 ก่อน Fix):** Iris 1.10.8 = release, MC 1.21.11, fabric, dependency required = Sodium, file sha1 `acfbe6d0…` ตรงกับไฟล์ที่ติดตั้งจริง (ข้อมูลจาก API สาธารณะ, `iris-versions.json`). `breaks` ของ jar 1.10.8 ยืนยันได้เฉพาะตอน stage.
- **Fix (Step 5):** กดปุ่ม "แก้ไข" → `iris-fabric-1.10.8+mc1.21.11.jar` ถูกติดตั้ง (sha1 ตรง Modrinth), `iris-1.10.7` ถูกเก็บเป็น `.disabled` (ไม่ลบ), Sodium 0.8.14 / Sodium Extra 0.9.4 / Reese's 2.2.4 ไม่ถูกแตะ (sha1 ไม่เปลี่ยน). Snapshot `1791867661854` สถานะ APPLIED.
- **Launch:** เกมเปิดถึง Main Menu (`10-launch-wait.png`, Fabric 0.19.5). Error ในล็อกเป็นสภาพแวดล้อม Android (JNA, narrator, Realms auth) ไม่เกี่ยวกับม็อด.
- **Rollback (Step 5):** กด "ย้อนกลับการซ่อมล่าสุด" → ยืนยัน "ตกลง" → Iris 1.10.7 กลับมา, Iris 1.10.8 ถูกย้ายออก (ไม่ลบ), mods ตรงกับก่อนซ่อม (`20-mods-after-rollback2.txt`).

### ยังไม่ตรงกับ baseline (ต้องรู้)
- `options.txt` ถูกเกมเขียนทับตอนเปิดเกม: hash `3cbe6a…` → `1e1cd7d8…` (ขนาด 4870 → 5055). ไม่มีสำเนาต้นฉบับ จึงกู้ bytes เดิมไม่ได้.
- `logs/latest.log`, `ALLLauncher/latest_game.log` เปลี่ยนจากการเปิดเกม (ปกติ).
- ตอนทดสอบมี Iris สร้าง `config/iris.properties`, `config/iris-excluded.json` → ลบแล้ว.
- Snapshot ที่ระบบสร้างใน QA-MD-1 (`1791867661854`) ย้ายไปเก็บที่ `/sdcard/Download/qa-md-backup/breaks-e2e/snapshot-1791867661854/`.
- ไฟล์ fixture Iris 1.10.7 และ Reese's 2.2.4 ถูกลบออกจาก mods (ตรงกับ backup `removed-after-test/` ที่ SHA-1 ตรง).

### สรุปสถานะ
- Preview: PASS. Fix (การเลือก Iris 1.10.8 + ไม่แตะ Sodium/Extra/Reese's + snapshot): PASS. Launch ถึง Main Menu: PASS. Rollback: PASS.
- Restore ทุกไฟล์ให้ SHA-1 ตรง: PARTIAL (`options.txt` และ log ไม่ตรง ดูด้านบน).
- source commit `b3b71b38`. ไม่ได้แตะ Main Instance, Warehouse, QA Backup เดิม หรือมือถือจริง.

## 21. Download File Safety (2026-10-13) — commit `67624e67` (ไม่เกี่ยวกับ Mod Doctor โดยตรง แต่ใช้ backend เดียวกัน)

แก้ `game/download/assets/_Download.Single.Tasks.kt` (ตัวกลางที่ Mod Doctor และ Download UI ทุกประเภทเรียกใช้):
- ก่อนหน้านี้: ลบไฟล์เดิมก่อน copy (copy ล้มกลางทาง = ไฟล์หาย) และ `onCancel` ลบไฟล์ปลายทางแม้ไฟล์นั้นมีอยู่ก่อนเริ่มงาน
- ตอนนี้: ติดตั้งผ่านไฟล์ชั่วคราวในโฟลเดอร์เดียวกัน, ตรวจ sha1 ให้ตรงกับไฟล์ที่ดาวน์โหลด, ถ้ามีไฟล์เดิมเปลี่ยนชื่อเป็น `.bak` (ไม่ลบ, ชื่อไม่ชนกัน), แล้วค่อยย้ายไฟล์ใหม่เข้าที่
- หลายเวอร์ชัน (หลาย instance): ถ้าเวอร์ชันใดล้มเหลว จะคืนทุกเวอร์ชันที่เขียนไปแล้วในงานนี้
- `onCancel`/ล้มเหลว คืนเฉพาะไฟล์ที่งานนี้เขียนเอง (`StagedInstall.written`) ไฟล์เดิมที่เนื้อหาเหมือนกันจะไม่ถูกเขียนทับ
- `DownloadSavesScreen.kt`: ลบ `onFileCancelled` ที่ลบโฟลเดอร์โลกชื่อเดียวกันตอนยกเลิก (อาจลบเซฟที่มีอยู่แล้ว)

หลักฐาน:
- Unit tests: `DownloadSingleInstallTest` 9/9 PASS; `:ZalithLauncher:testDebugUnitTest --tests "...game.download.assets.*" --tests "...game.version.mod.*"` BUILD SUCCESSFUL
- APK `ALLLauncher-Debug-0.0.2-beta1.apk` build สำเร็จ SHA-256 `3abbc066…41d0` (`test-artifacts/download-file-safety-20261013/apk-sha256.txt`)
- ติดตั้งสำเร็จหลัง reboot LDPlayer 2 รอบ (รอบแรกและรอบสองค้างที่ `Performing Streamed Install`) lastUpdateTime 15:09:01
- เปิด launcher แล้วหน้าหลักปกติ (`01-launcher-after-install.png`)
- QA-MD-1: jar/config/options.txt SHA-1 ตรง Baseline 2 (`mod-breaks-e2e-complete-20261013/30-baseline-2-qa-md-1.txt`), ไม่ได้เปิดเกมซ้ำเพื่อไม่ให้ options.txt เปลี่ยนอีก

ผล QA:
- Unit + Build + Install + Launcher start: PASS
- การดาวน์โหลดจริงกับ Cancel/ชื่อชน/copy ล้มกลางทาง: UNVERIFIED (ไม่ได้ทดสอบบน QA-MD-1 ด้วยการดาวน์โหลดจริง)

ความเสี่ยงที่ยังเหลือ (ยังไม่แก้):
- `AllDownloadService.installAsset` (world/saves): เมื่อ unpack ล้มเหลว ลบ zip ปลายทางด้วย แม้ zip นั้นเป็นไฟล์เดิมที่งานนี้ไม่ได้เขียน (กรณี Unchanged) — ต้องเปลี่ยน callback ให้รู้ว่างานนี้เขียนไฟล์หรือไม่ก่อน
- `unpackSaveZip` เขียนทับโฟลเดอร์โลกเดิมได้; `DownloadSavesScreen` ไม่มี guard เหมือน `AllDownloadService` (ต้องเพิ่ม guard เอง)

## 22. Finish Download Safety (2026-10-13) — ผลจริง, commit `67624e67`

แก้:
- `_Download.Single.Tasks.kt`: ถ้า callback หลังติดตั้ง (เช่นแตก ZIP) ล้มเหลว จะคืนเฉพาะไฟล์ที่งานนี้เขียน
- `AllDownloadService.kt`: ไม่ลบ zip/โฟลเดอร์ด้วยชื่อแล้ว ตรวจโลกซ้ำด้วย `findExistingWorldTarget` ก่อนดาวน์โหลด
- `_Install.Saves.kt`: `unpackSaveZip` จองโฟลเดอร์โลกด้วย `mkdir` (atomic) ถ้ามีอยู่แล้วปฏิเสธและไม่แตะ; ล้มเหลวลบเฉพาะโฟลเดอร์ที่ตัวเองสร้าง
- `DownloadSavesScreen.kt`: ปฏิเสธโลกชื่อซ้ำก่อนดาวน์โหลด (ข้อความ EN/TH `download_world_name_taken`); ลบ callback ยกเลิกที่ลบโลกชื่อเดียวกัน; แก้ `return@doInstall` เป็น if/else
- `AllWorldsWindow.kt`: ไม่ลบโฟลเดอร์ตามชื่อเมื่อการแตกล้มเหลว (อาจลบโลกที่มีอยู่แล้ว)

Unit tests:
- `DownloadSingleInstallTest` 9/9 PASS
- `SaveWorldUnpackSafetyTest` 7/7 PASS (โลกซ้ำไม่แตะ, ZIP เสีย/ไม่มี level.dat ไม่เหลือโฟลเดอร์, ไม่ลบโลกที่เกิดระหว่างงาน, ค้นหาโลกซ้ำ, rollback คืน ZIP เดิมจาก .bak)
- ชุด `game.download.*` + `game.version.mod.*`: `AdaptiveSourceSelectionTest` FAIL 1/3 (`mirror hits should be bounded, got 13`) — ไม่มีไฟล์ engine ถูกแก้; ยังไม่ได้เทียบกับ HEAD (ห้าม stash)

Build/Install:
- APK SHA-256 `497726de…5042` (ตรงกับที่ติดตั้งจริง) — build ผ่าน
- ติดตั้งครั้งแรก/สอง `adb install` ค้าง; push แล้ว `pm install` (ผ่าน `MSYS_NO_PATHCONV=1`) ค้างเกิน 900 วินาที และ package manager ของ LDPlayer ไม่ตอบสนอง (`pm list packages` ว่าง) → reboot LDPlayer index 0 เท่านั้น → `lastUpdateTime 15:56:32` ตรงกับ SHA-256 ของ APK ที่ build แล้ว

QA บน QA-MD-1 (ไม่เปิด Minecraft):
- เปิด launcher ได้ปกติ
- Mod download collision: **PASS** — มี `iris-fabric-1.10.8+mc1.21.11.jar` (fixture 34 bytes ที่ชื่อชนกัน) อยู่ก่อน; หลังติดตั้ง ไฟล์ใหม่ sha1 `acfbe6d0…` ตรง Modrinth, fixture ถูกเก็บเป็น `.bak` sha1 `136ee669…` ตรงต้นฉบับ
- Reinstall เวอร์ชันเดียวกัน: **PASS** — ไม่มีการเขียนทับ ไม่มี `.bak` เพิ่ม (log `DownloadSingle` 16:44:26 ยืนยันว่างานทำงาน)
- Cancel ระหว่างดาวน์โหลดจริง: **UNVERIFIED** — ดาวน์โหลดเสร็จเร็วกว่าจะกดยกเลิกได้ทัน
- World download บนโลกชั่วคราว: **UNVERIFIED** — ยังไม่ได้ทดสอบบน UI (ทดสอบเฉพาะระดับ unit)
- Rollback หลังแตก ZIP ล้มเหลว: **PASS ระดับ unit** เท่านั้น ยังไม่ได้ทดสอบสด

การคืนสภาพ QA: ย้าย `iris-1.10.8` และ fixture `.bak` ไปที่ `/sdcard/Download/qa-md-backup/download-safety-20261013/` (ไม่ลบ) — mods/config/jar/json/options.txt/log4j2 ตรง Baseline 2 (`16-qa-after-download-test.txt`); options.txt ไม่เปลี่ยนเพราะไม่ได้เปิดเกม

ความเสี่ยงที่ยังเหลือ:
- `AdaptiveSourceSelectionTest` ล้มเหลวอยู่ก่อนหรือไม่ ยังไม่ยืนยัน
- เส้นทาง Cancel สดและโลกสดยังไม่ได้ทดสอบ
- เมื่อหลายเวอร์ชันและเวอร์ชันหลังล้มเหลว โลกเวอร์ชันแรกที่แตกสำเร็จแล้วจะไม่ถูกย้อน (ต้องถือเป็นข้อจำกัด)

## 23. Final World ZIP Safety (2026-10-13) — commit `67624e67`, ไม่มี Iris E2E ซ้ำ

ปัญหาที่แก้: `installStagedFile` คืน `written=false` เมื่อ ZIP เดิมมี SHA-1 เหมือนกัน แต่ `onFileCopied` ยังถูกเรียก และ `unpackSaveZip` ลบ ZIP หลังแตกสำเร็จเสมอ → ZIP ที่ผู้ใช้มีอยู่ก่อนถูกลบได้

แก้ (ใช้ระบบ `StagedInstall` เดิม ไม่สร้าง Downloader ใหม่):
- `StagedInstall` เป็น public และ `backup`/`written` เป็น `var`; callback `onFileCopied` ได้รับ `install: StagedInstall` (ownership)
- `installStagedToTargets(...)` รวมลำดับ "ติดตั้งทุกปลายทาง → เรียก callback" และคืนค่าเมื่อล้มเหลวด้วย `rollbackStagedInstalls` (คืนเฉพาะที่ `written`)
- `StagedInstall.restoreKeptFile()`: หลังแตกสำเร็จ ถ้าเคยมี ZIP เดิม จะเอาไฟล์เดิมกลับมาใช้ชื่อเดิม และเปลี่ยนเป็น "ไม่เป็นเจ้าของ" เพื่อไม่ให้ rollback รอบถัดไปลบของผู้ใช้
- `unpackSaveZip(zip, folder, removeZipWhenDone = true): File` — ดาวน์โหลดส่ง `removeZipWhenDone = install.written`; ค่าเดิมยังใช้กับ `AllWorldsWindow`/`SavesManagerScreen`
- `SaveWorldBatch`: จำโลกที่ตัวเองสร้าง (ผ่าน mkdir จอง) ถ้าปลายทางถัดไปล้มเหลว จะลบเฉพาะโลกที่สร้างในงานนี้; ถ้าลบไม่ได้จะโยน `IOException` ระบุว่าเป็น partial success และไม่แตะโลกเดิม
- `AllDownloadService`, `DownloadSavesScreen`: ใช้ `SaveWorldBatch` แทนการลบตามชื่อ; `ModDoctor` เปลี่ยน callback ให้รับ 3 พารามิเตอร์

ผลการทดสอบ:
- `WorldZipOwnershipTest` 8/8 PASS: ZIP SHA-1 เดียวกันสำเร็จ, ZIP ต่างเนื้อหาสำเร็จ (คืนไฟล์เดิม), ZIP ต่างแตกผิดพลาด, ZIP SHA-1 เดียวกันแตกผิดพลาด, Cancel หลังติดตั้ง (rollback คืนไฟล์เดิม), หลายปลายทางสำเร็จทั้งคู่, หลายปลายทางล้มเหลวที่ปลายทางหลัง (โลกที่ตัวเองสร้างถูกลบ, โลกเดิมและ ZIP เดิมไม่ถูกแตะ), `SaveWorldBatch.undo` ไม่ลบอะไรเมื่อสำเร็จ
- `SaveWorldUnpackSafetyTest` 7/7 PASS, `DownloadSingleInstallTest` 9/9 PASS
- Build `:ZalithLauncher:assembleDebug` BUILD SUCCESSFUL, APK SHA-256 `492432033b11fcad908099a73897f5d85c731bbe238b2e33d7e117ccbd9bbda0` (ยังไม่ได้ติดตั้งบน LDPlayer รอบนี้)
- `AdaptiveSourceSelectionTest` รันแยก 3 ครั้ง ล้มเหลวทุกครั้ง (`mirror hits should be bounded, got 13/13/14`) — ไฟล์ใน `game/download/engine` และ test ไม่เปลี่ยนจาก HEAD (`git diff` ว่าง) และ test ไม่ import คลาสที่แก้ และยังไม่ทราบต้นเหตุ — **Known Failure** ไม่ได้พิสูจน์ว่าเป็นปัญหาเดิม (ยังไม่ได้รันบน HEAD)

ความเสี่ยง/ข้อจำกัดที่ยังเหลือ (รายงานตรงๆ):
- Partial success ของโลกหลายปลายทาง: ทางที่ลบไม่ได้ถูกเขียนรายงานใน `IOException` แต่ยังไม่มี unit test ครอบคลุม (ทดสอบแบบจำลองให้ลบไม่ได้บน Windows ไม่ได้)
- Cancel หลังเริ่มแตกโลกแล้ว: โค้ดคืน ZIP แต่ไม่ลบโลกที่แตกไปแล้ว (ความเป็นไปได้ต่ำ เพราะการแตกทำแบบ synchronous ภายใน callback)
- `SavesManagerScreen` (นำเข้า ZIP จากหน้าจัดการโลก) เรียก `unpackSaveZip` เดิม: ตอนนี้ปฏิเสธโลกที่มีอยู่แทนการ merge แต่สำเนา ZIP ที่นำเข้ายังคงอยู่เมื่อล้มเหลว (พฤติกรรมเดิม ไม่เปลี่ยน)
- QA สดของ world และ cancel ยังไม่ได้ทำ ตามข้อกำหนดห้ามเปิด Minecraft/ห้าม E2E ซ้ำ

### สถานะ Commit Download Safety (2026-10-13)\r\n- commit: `67624e67` (สถานะด้านล่างบันทึกไว้ก่อน commit)
- สถานะ: **PARTIAL**
- Unit Tests ที่เกี่ยวข้อง PASS: `DownloadSingleInstallTest` 9/9, `SaveWorldUnpackSafetyTest` 7/7, `WorldZipOwnershipTest` 8/8
- Build `assembleDebug` PASS; APK ล่าสุด `492432…bbda0` ยังไม่ได้ QA บนอุปกรณ์
- `AdaptiveSourceSelectionTest`: **Known Failure** ล้มเหลว 3 รอบ (`mirror hits should be bounded, got 13/13/14`) ต้นเหตุยังไม่ทราบ ไม่ได้พิสูจน์ว่าเป็นปัญหาเดิม และไม่ได้แก้ engine (อยู่นอกขอบเขต)

## 24. Universal Mod Doctor V1 (Plan-only) — commit `fd06cd77`

สิ่งที่ทำได้จริง:
- การชนจาก `breaks` ของ jar ม็อดใดก็ได้ในอินสแตนซ์ ได้รับการวางแผน (ไม่จำกัดกลุ่ม Sodium) โดยใช้ `findBreaksConflicts` เดิม อ่านจากเครื่องทั้งหมด ไม่ยิงเครือข่าย
- ค้นหาบนแพลตฟอร์มเฉพาะม็อดที่มีส่วนในการชน (ต้นทาง + เป้าหมาย) ไม่เกิน `MAX_PLAN_LOOKUPS = 8`; เกินขีดจำกัดแสดง UNVERIFIED ไม่ค้นหาบางส่วนแล้วเดา
- แผนแก้เฉพาะ "เป้าหมาย" ที่ชน โดยเลือกเวอร์ชันใหม่กว่าที่ประกาศพ้นเงื่อนไข `breaks` และรองรับ MC/Loader; ไม่ downgrade
- ตรวจ dependency/incompatible ของผู้สมัครกับม็อดที่รู้จักเท่านั้น; ถ้าต้องการม็อดนอกชุดที่ค้นหาไม่ได้ → UNVERIFIED
- `ModpackPolicy`: เป้าหมายที่เป็นไฟล์ของ Modpack → BLOCKED_PACK_OWNED
- Known-good ไม่ถูกใช้เป็นแผนในเส้นทาง breaks; เส้นทางกลุ่มเดิม (Known-good/discovery) ยังทำงานเฉพาะเมื่อปัญหาแตะสมาชิกกลุ่ม และค้นหาเฉพาะสมาชิกกลุ่ม
- `withGroupPlan`: ไม่แสดงแผนว่างเมื่อไม่มีอะไรต้องวางแผน
- ไม่เพิ่ม Fix/Executor ใหม่ Preview ยัง plan-only; Fix/Snapshot/Rollback เดิมไม่เปลี่ยน

ข้อจำกัด (สำคัญ):
- Plan-only: ยังไม่มีการแก้ไฟล์จากแผนนี้; ปุ่ม Fix เดิมยังทำงานตามกลไกเดิม
- `breaks` ของ jar ผู้สมัครแพลตฟอร์มไม่เปิดเผย ต้องรอ staging ตอน Fix
- ปัญหาที่ไม่ใช่ `breaks` (เช่น dependency ที่ขาด) ยังไม่ถูกวางแผนโดย Universal V1
- ความขึ้นกับม็อดที่อยู่นอกชุดที่ค้นหา → UNVERIFIED
- Known-good ไม่แสดงเป็นข้อเสนอในเส้นทาง breaks (ยังไม่ได้ทำ)
- ยังไม่มีการ QA บนอุปกรณ์รอบนี้

ผลทดสอบ:
- `:ZalithLauncher:testDebugUnitTest --tests "...game.version.mod.*"` BUILD SUCCESSFUL; `UniversalBreaksPlanTest` 10/10 PASS (breaks นอกกลุ่ม, dependency ข้ามกลุ่มที่ขาด/มี, incompatible ของต้นทาง, ไม่มี breaks, เวอร์ชันเป้าหมายไม่ครบ, ชื่อเวอร์ชันผู้สมัครอ่านไม่ได้, pack-owned, ขีดจำกัดการค้นหา, ไม่มี Known-good downgrade); `ModGroupPlannerTest` 34/34
- `:ZalithLauncher:assembleDebug` BUILD SUCCESSFUL, APK SHA-256 `09cfa35f7e817071b80badbd7365c4fe610d7429a1383ca3ea71e2ea18944039`
- `AdaptiveSourceSelectionTest` ไม่ได้รันซ้ำรอบนี้ — ยังคงเป็น **Known Failure** (FAIL 3 รอบ, ต้นเหตุยังไม่ทราบ, ห้ามอ้างว่าผ่าน)

## 25. Universal Mod Doctor V2 (Plan-only, Missing Dependencies) — commit `4b0d067b`, `d0434b1c`

ทำอะไรบ้าง (ใช้ระบบเดิม ไม่สร้าง Resolver/Downloader/Fix ใหม่):
- ใช้ `MISSING_REQUIRED_DEPENDENCY` และ `ModDoctorProblem.fixCandidate` ที่ `withFixCandidates` resolve ไว้แล้วเดิม — ไม่มีคำขอเครือข่ายเพิ่มจากการวางแผน
- `ModDoctorFixCandidate.versionName` (ใหม่, default null): เวอร์ชันแรกที่รองรับ Minecraft/loader ที่ `resolveFixCandidate` เจอแล้ว ใช้แสดงในแผน
- `planMissingDependencies(...)` (Planner): จัดกลุ่มตามโปรเจกต์ → เสนอการติดตั้งครั้งเดียวต่อโปรเจกต์ แม้มีม็อดหลายตัวต้องการ; ระบุม็อดที่ต้องการ (`requiredBy`) และ Id ของ dependency
- รายการที่ resolve ไม่ได้ → UNVERIFIED พร้อมเหตุผลเดิมจาก `resolveFixCandidate` ไม่เดา
- `GroupPlan?.withDependencyPlan(...)`: รวมแผน dependency กับแผน breaks/Known-good โดยไม่ซ่อนกัน (ถ้ามีทั้งสองประเภท แสดงทั้งคู่ และสถานะเป็น UNVERIFIED ถ้าอย่างใดอย่างหนึ่งตรวจไม่ได้)
- อินสแตนซ์ที่เป็นแพ็ก (`ModpackPolicy.isPackManaged`): ไม่เสนอการติดตั้ง dependency → BLOCKED_PACK_OWNED พร้อมหมายเหตุ
- Preview (`GroupPlanPreview`) แสดงหัวข้อ "dependency ที่ขาดและต้องติดตั้ง (แสดงอย่างเดียว)" พร้อมชื่อ dependency, ม็อดที่ต้องการ, เวอร์ชัน (EN/TH)
- ไม่เปลี่ยน Fix/Snapshot/Rollback; ไม่มี Downgrade; ม็อดที่ไม่เกี่ยวข้องไม่ถูกแตะ

ข้อจำกัด (ต้องรู้):
- Plan-only: ปุ่ม Fix เดิมยังทำงานตามกลไกเดิม ไม่ได้ทำตามแผนนี้
- เวอร์ชันที่แสดงคือเวอร์ชันแรกที่ resolve ได้ (รายการเดียวกับที่ `resolveFixCandidate` ตรวจ); การติดตั้งจริงลองตามลำดับ stable-first ของ Fix เดิม จึงอาจติดตั้งเวอร์ชันอื่นได้
- dependency ของผู้ให้บริการ (transitive) ยังไม่ได้ตรวจในแผน; `breaks` ของ jar ผู้ให้บริการตรวจได้ตอน staging เท่านั้น (มีการระบุเป็นความเสี่ยงในแผน)
- ยังไม่ตรวจว่าผู้ให้บริการอาจถูกติดตั้งอยู่แล้วในชื่ออื่น (Fix เดิมตรวจด้วย fingerprint ตอนลงมือ)
- เหตุผลของการติดตั้งแสดงเป็นรายการ Id/ม็อด/เวอร์ชัน ยังไม่ได้ทำประโยคหลักฐานเต็มรูปแบบ
- ยังไม่มี QA บนอุปกรณ์รอบนี้ (UNVERIFIED)

ผลทดสอบ:
- `game.version.mod.*` BUILD SUCCESSFUL; `UniversalDependencyPlanTest` 11/11 PASS (ผู้ต้องการร่วม 1 ติดตั้ง 1 ครั้ง, Id ซ้ำต่างชื่อโปรเจกต์เดียวกัน, resolve ไม่ได้ → UNVERIFIED, ไม่มี dependency, เวอร์ชันจาก candidate, ไม่แตะม็อดอื่น, breaks+dependency แสดงทั้งคู่, breaks UNVERIFIED ไม่ถูกซ่อน, dependency UNVERIFIED คงรายการที่ resolve ได้, แพ็ก → BLOCKED, dependency ล้วนที่ตรวจไม่ได้ → UNVERIFIED); `UniversalBreaksPlanTest` 10/10; `ModGroupPlannerTest` 34/34 PASS
- `:ZalithLauncher:assembleDebug` BUILD SUCCESSFUL, APK SHA-256 `9b08c3fa72243b42bb0ad69772dc980b657b7600eb77fb97b37a107b4f6fe308` (ยังไม่ได้ติดตั้งหรือ QA บนอุปกรณ์)
- `AdaptiveSourceSelectionTest` ไม่ได้รันซ้ำ — ยังคงเป็น **Known Failure** (ต้นเหตุยังไม่ทราบ ห้ามอ้างว่าผ่าน)

### ปิดงาน V2 (Finalize)
- ข้อความ Preview แก้เป็น "เวอร์ชันเบื้องต้น (ตัวเลือก)" และระบุว่าปุ่มแก้ไขอาจติดตั้งเวอร์ชันที่รองรับอื่น (EN/TH)
- สถานะ: **Plan-only**; Unit Tests และ Build PASS (`UniversalDependencyPlanTest` 11/11, `UniversalBreaksPlanTest` 10/10, `ModGroupPlannerTest` 34/34; APK SHA-256 `48026a09…d59`); **Device QA = UNVERIFIED**
- ข้อจำกัดที่ยังเหลือ: เวอร์ชันใน Preview เป็นตัวเลือกเบื้องต้น ปุ่มแก้ไขอาจเลือกเวอร์ชันอื่น; Provider Dependencies ยังตรวจไม่ครบในแผน (ตรวจ breaks ตอน staging); ปัญหาที่ไม่ใช่ breaks/missing dependency ยังไม่รองรับ
- `AdaptiveSourceSelectionTest` ยังเป็น **Known Failure** สาเหตุไม่ทราบ

## 26. Universal Mod Doctor V3: Preview/Fix Consistency — commit `c3bf9b17`

ความต่างที่พบ (ก่อนแก้):
1. **เวอร์ชัน dependency**: Preview เลือกตัวแรกที่รองรับ (ลำดับของแพลตฟอร์ม) แต่ Fix ลองแบบ stable ก่อน → อาจต่างกันเมื่อมี beta ใหม่กว่า
2. **Breaks ฝั่งเป้าหมาย**: Fix เดิมเรียงผู้สมัครทุกเวอร์ชันที่รองรับ (รวมรุ่นเก่ากว่า) จึงอาจ “ดาวน์เกรด” เป้าหมายโดยไม่มีหลักฐานว่าจำเป็น; Preview เลือกเฉพาะรุ่นใหม่กว่าที่ยืนยันได้
3. **Staging**: Fix ตรวจ breaks/dependency ของผู้สมัครตอน staging ตัวที่ถูกปฏิเสธจะข้ามไปตัวถัดไป; Preview มองไม่เห็นขั้นนี้
4. **ฝั่งต้นเหตุ (breaks source)**: Fix อาจเปลี่ยนม็อดฝั่งที่ประกาศ breaks (เช่น ลดเวอร์ชันเพื่อพ้นเงื่อนไข) ซึ่ง Preview ยังไม่วิเคราะห์

สิ่งที่แก้:
- `previewDependencyVersion(...)`: Preview ของ dependency ใช้กติกาเดียวกับ Fix (เฉพาะที่รองรับ MC/loader แล้ว stable ก่อน); ผลลัพธ์เก็บใน `ModDoctorFixCandidate.versionName`
- `ModDoctorMultiModRepair`: **เป้าหมายไม่ถูกลดเวอร์ชันอัตโนมัติ** — ผู้สมัครฝั่งเป้าหมายต้องยืนยันได้ว่าใหม่กว่าเวอร์ชันที่ติดตั้ง (`isNewerBuildThanInstalled`; ไม่รู้เวอร์ชัน → ไม่รับ) ฝั่งต้นเหตุ (ที่ประกาศ breaks) ยังลดเวอร์ชันได้เมื่อมีหลักฐาน และยังผ่าน staging เดิม
- Preview: หัวข้อแยก “เสนอให้เปลี่ยน / เสนอให้ติดตั้ง dependency: ยืนยันหลัง Staging ตอนแก้ไข”, “ยังตรวจไม่ครบ / หมายเหตุ”, และสถานะ “ถูกบล็อก” สำหรับอินสแตนซ์จากแพ็ก (EN/TH)
- หมายเหตุ UNVERIFIED ของ breaks บอกว่าปุ่มแก้ไขอาจลองเปลี่ยนฝั่งต้นเหตุ ซึ่งแผนยังไม่ได้วิเคราะห์

**การเปลี่ยนพฤติกรรม Fix (ต้องรู้):** ก่อนหน้านี้ Fix อาจ stage และติดตั้งเป้าหมายรุ่นเก่ากว่าได้; หลังแก้ไม่ทำแล้ว ผลต่อ `worstCaseWalkStagesTheValidFourthAttempt`: จำนวนการ stage จาก 21 เป็น 9 (ไม่มีการ stage รุ่นเป้าหมายเก่ากว่า) และยังถึงความพยายามที่ 4 ของผู้ติดตามคนที่สองได้; Snapshot/Rollback ไม่เปลี่ยน

ข้อจำกัดที่ยังเหลือ:
- Preview ยังไม่รู้ผลการ staging: ผู้สมัครที่ถูกปฏิเสธตอน staging จะถูกข้ามไปตัวถัดไป; ถ้าไม่มีตัวใดผ่าน Fix จะรายงานเหตุผลเดิม ไม่อ้างว่าทำตาม Preview ได้
- ฝั่งต้นเหตุที่ประกาศ breaks ยังไม่ถูกวิเคราะห์ในแผน (Fix อาจลดเวอร์ชันฝั่งนั้นพร้อมหลักฐาน breaks)
- dependency ของผู้ให้บริการยังตรวจไม่ครบในแผน
- เวอร์ชันที่แสดงเป็น “ตัวเลือกเบื้องต้น”
- ยังไม่มี Device QA (UNVERIFIED)

ผลทดสอบ:
- `game.version.mod.*` BUILD SUCCESSFUL: `PreviewFixConsistencyTest` 5/5 (stable ก่อน, ไม่มีผู้ให้บริการที่ตรงกับ MC/loader, ไม่ลดเวอร์ชันโดยไม่ทราบรุ่น, ผู้สมัครที่เวอร์ชันไม่รู้ไม่ถือเป็นการอัปเดต, UNVERIFIED บอกว่า Fix อาจทำอีกฝั่ง), `ModDoctorMultiModRepairTest` 17/17, `UniversalDependencyPlanTest` 11/11, `UniversalBreaksPlanTest` 10/10, `ModGroupPlannerTest` 34/34
- `:ZalithLauncher:assembleDebug` BUILD SUCCESSFUL, APK SHA-256 `c5353745f6db7113135d60f156dc0b0153ecbb8e20c110cd62e0e8459d803d7a`
- ยังไม่ติดตั้งหรือ QA บนอุปกรณ์
- `AdaptiveSourceSelectionTest` ไม่ได้รันซ้ำ — ยังคงเป็น **Known Failure** (สาเหตุไม่ทราบ ห้ามอ้างว่าผ่าน)

### ปิดงาน V3 (Finalize)
- การเปลี่ยน Fix ใน V3 จำกัดเฉพาะการป้องกัน Downgrade ฝั่ง **Breaks Target** (ผู้สมัครต้องยืนยันได้ว่าใหม่กว่าเวอร์ชันที่ติดตั้ง)
- พฤติกรรม **Breaks Source** คงเดิมในรอบนี้: ฝั่งที่ประกาศ `breaks` ยังอาจถูกลดเวอร์ชันเมื่อหลักฐาน breaks รองรับ และผ่าน staging เดิม; Preview ยังไม่วิเคราะห์ฝั่งนี้
- **ห้ามอ้างว่าทั้งระบบไม่มีการ Downgrade**: การ Downgrade ฝั่ง Source ยังเป็นไปได้
- สถานะ: Unit Tests PASS (`PreviewFixConsistencyTest` 5/5, `ModDoctorMultiModRepairTest` 17/17, planner tests), APK SHA-256 `c5353745…d803d7`, **Device QA = UNVERIFIED**, `AdaptiveSourceSelectionTest` = **Known Failure** (สาเหตุไม่ทราบ)

## 27. Mod Doctor V1–V3 E2E QA บน LDPlayer (2026-10-13) — ผล QA (source `c3bf9b17`, `fdf2a062`)
- รายละเอียดและหลักฐาน: `test-artifacts/mod-doctor-v1-v3-qa-20261013/RESULT.md`
- APK ที่ติดตั้ง: `c5353745…d803d7` (ตรวจด้วย sha256 ของ base.apk แล้ว)
- Instance ทดสอบ: `QA-V3-1` (คัดลอกจาก QA-MD-1 ไม่มีประวัติเดิม); QA-MD-1 ตรง Baseline 2 หลังทดสอบ
- **S1 Missing dependency** (sodium-extra ต้องการ sodium): **PASS** — Preview ↔ Fix ตรงกันที่ sodium 0.8.14; Rescan ไม่มีปัญหา; Rollback ตรงก่อนซ่อม
- **S2 Breaks** (sodium 0.8.14 breaks iris<=1.10.7): **PASS** — Preview ↔ Fix ตรงกันที่ iris 1.10.8; ฝั่ง target ไม่ downgrade; iris 1.10.7 เป็น .disabled; Rescan ไม่มีปัญหา; Rollback ตรงก่อนซ่อม
- **S3 ปัญหาพร้อมกัน**: **NOT ACHIEVED** — Fixture ปลอดภัยที่ยืนยันแหล่งไม่พอ; ถอน fabric-api ไม่ทำให้เกิดปัญหา dependency ใน Mod Doctor (มีแต่ breaks)
- **ข้อสังเกตที่ต้องติดตาม (ไม่แก้ในรอบนี้):** Sodium 0.8.14 ประกาศ dependency โมดูลของ fabric-api (เช่น fabric-block-view-api-v2) แต่เมื่อไม่มี fabric-api Mod Doctor ไม่รายงาน — ผลต่อการเปิดเกมยังไม่ได้ทดสอบ
- Preview แสดงหัวข้อ "ยืนยันหลัง Staging" ทุกกรณี (ไม่รับประกันเวอร์ชันก่อน Staging)
- ไฟล์เกมที่เปลี่ยนจากการเปิดเกม (logs/, options.txt, config/, .fabric/) แยกจากการซ่อม และยังไม่ได้วัด hash แบบเป็นระบบ — UNVERIFIED
- ข้อจำกัดของ QA: ทดสอบบน LDPlayer เท่านั้น ไม่มี iQOO; Device QA เต็มรูปแบบยังไม่ครบ (S3 ไม่บรรลุ)
- Known Failure: `AdaptiveSourceSelectionTest` ยังไม่ทราบสาเหตุ (ไม่ได้รันซ้ำรอบนี้)

### Gap Closure (2026-10-13)
- ยืนยันแล้วว่า Sodium 0.8.14 ฝัง Fabric API modules ไว้ใน `META-INF/jars/` (fabric-block-view-api-v2, fabric-rendering-fluids-v1, fabric-resource-loader-v0) และประกาศใน `jars` → การถอด fabric-api แล้วไม่มี Missing Dependency **ถูกต้อง** ไม่ใช่ Bug; ข้อสังเกตในรอบก่อนถูกถอน
- S3 (Missing + Breaks พร้อมกัน): **NOT ACHIEVED** — รอการอนุมัติดาวน์โหลด Mod Menu 17.0.1 (`mOgUt4GM`, SHA-1 `d5612d70…`, 960,882 bytes; ต้องการ `fabric-api` และ `placeholder-api`)
- Universal นอก Sodium/Iris: **NOT ACHIEVED** — ใช้ Fixture เดียวกันหลังได้รับอนุมัติ
- รอบนี้ไม่มี Fix/Rollback ใหม่ ไม่มีการแก้ Source
- รายละเอียด: `test-artifacts/mod-doctor-v1-v3-qa-20261013/RESULT.md` (หัวข้อ Gap Closure)

### Final E2E Gap Closure (2026-10-14)
- Mod Menu 17.0.1 ดาวน์โหลดจาก Modrinth: SHA-1 `d5612d70…` ตรง, ขนาด 960,882 bytes ตรง, Minecraft 1.21.11 / fabric ตรง → **PASS**
- Mod Menu ไม่มี Missing Dependency จริง: jar ฝัง fabric-api modules และ placeholder-api ไว้แล้ว (`jars`), เกมเปิดถึง Main Menu และมีปุ่ม Mods โดยไม่มี fabric-api/placeholder-api ภายนอก → **Universal นอกกลุ่มด้วย Missing Dependency: NOT ACHIEVED** (ผลตาม metadata และการทดสอบจริง)
- **S3 (Missing + Breaks พร้อมกัน): NOT ACHIEVED** (ไม่มี Missing จริงจาก Mod Menu)
- Variant Breaks + Mod Menu (ไม่มี fabric-api): Scan → Preview → Fix → Rescan → Rollback **PASS** (Preview/Fix ตรงที่ iris 1.10.8; ฝั่ง target ไม่ downgrade; Mod Menu ไม่ถูกระบุ; hash ตรงก่อน/หลัง Rollback; quarantine ถูกต้อง)
- Universal นอกกลุ่ม Sodium/Iris ที่พิสูจน์ได้จริง: **UNVERIFIED**
- ไม่มีการแก้ Source/Scanner/Fix, ไม่มี Build/Unit Tests ใหม่
- หลักฐานเต็ม: `test-artifacts/mod-doctor-v1-v3-qa-20261013/RESULT.md` (หัวข้อ Final E2E Gap Closure)

### Final Mod Doctor E2E Gap Closure — Litematica + MaLiLib (2026-10-14)
- Litematica 0.26.16 (Modrinth `bEpr0Arc`/`mengBR9D`, SHA-1 `6faf0156…`): ต้องใช้ malilib `>=0.27.19 <0.28.0` จริง (ไม่ได้ฝังเอง) → พิสูจน์แล้ว
- **Universal Missing Dependency นอก Sodium/Iris: PASS** — Scan → Preview (malilib 0.27.20 ตัวเลือก) → Fix (malilib 0.27.20, SHA-1 `ba18a2c6…` ตรง Modrinth) → Rescan ไม่มีปัญหา → Rollback (malilib เข้า quarantine, Litematica ไม่เปลี่ยน)
- **S3 (missing malilib + breaks sodium↔iris): Detection PASS, Preview PASS, Fix FAIL**
  - Scan พบ 2 ปัญหา; Preview แสดงทั้งสองรายการ (ยืนยันหลัง Staging)
  - Fix ไม่ซ่อม (snapshot NO_CHANGE, mods ไม่เปลี่ยน)
  - **สาเหตุ (Reproduction):** ขั้น breaks ตรวจความสอดคล้องกับชุดปัจจุบันก่อนติดตั้ง dependency → iris 1.10.8 ถูกปฏิเสธเพราะ "litematica requires malilib"; แล้วลองลดเวอร์ชัน sodium ฝั่ง breaks source (0.8.14 → … → 0.8.15-beta.1) ทุกรุ่นถูกปฏิเสธ; Fix คืน NoSafePlan ก่อนถึงขั้นติดตั้ง dependency
  - ไม่ได้แก้ Source; ข้อเสนอแก้ไขต้องได้รับอนุมัติก่อน
- Rescan/Rollback ของ S3: ไม่ได้ทำ (ไม่มีการเปลี่ยนที่ต้องย้อนกลับ)
- ไม่มี Build/Unit Tests ใหม่; ไม่มีการแก้ Source
- หลักฐาน: `test-artifacts/mod-doctor-v1-v3-qa-20261013/RESULT.md` (หัวข้อ Final Mod Doctor E2E Gap Closure — Litematica + MaLiLib) และ `final/`

### S3 Mixed Repair Fix (2026-10-14)
- แก้บั๊ก S3: `ModDoctorMultiModRepair` ตรวจชุดผลลัพธ์โดยไม่รวม dependency ที่กำลังจะติดตั้ง → ตอนนี้รับ `pendingDependencies` (resolve แล้วเดิม), stage + ตรวจเมทาดาทาของผู้ให้บริการ (≤2 โปรเจกต์ × 2 ครั้ง) แล้วรวมเข้าชุดผลลัพธ์; ไม่ลดระดับ Safety Validation, ไม่มี Downloader/Engine ใหม่; แพ็ก → ไม่ส่ง pending (พฤติกรรมเดิม)
- Unit Tests: `ModDoctorMultiModRepairTest` 22/22 และ `game.version.mod.*` ผ่าน; APK `393ec8ee…4e02e`
- **E2E S3 บน QA-V3-1: PASS** — Scan 2 ปัญหา → Fix ซ่อมทั้งคู่ (iris 1.10.8 `acfbe6d0…`, malilib 0.27.20 `ba18a2c6…`) → Rescan สะอาด → Rollback ตรง Baseline ทุกไฟล์ + quarantine ถูกต้อง
- ข้อจำกัด: ไฟล์เกมจากการเปิดเกมยังไม่วัด hash; ทดสอบ LDPlayer เท่านั้น; `AdaptiveSourceSelectionTest` ยังเป็น Known Failure
- commit `fdf2a062`; หลักฐาน `test-artifacts/mod-doctor-v1-v3-qa-20261013/s3fix/` และ `RESULT.md`

### ปิดงาน Mod Doctor Core (2026-10-14)
- **Universal Mod Doctor V1–V3 Core: E2E PASS บน LDPlayer ตามขอบเขตที่ทดสอบจริง** (QA-V3-1):
  S1 Missing Dependency (Sodium Extra→Sodium), S2 Breaks (Sodium↔Iris), Missing Dependency นอกกลุ่ม (Litematica→MaLiLib 0.27.20), S3 Mixed (MaLiLib + Sodium/Iris) — Scan → Preview → Fix → Rescan → Rollback พร้อมตรวจ SHA-1 ของไฟล์ม็อดและ quarantine
- S3 หลักฐาน SHA-1: Iris 1.10.8 `acfbe6d0…`, MaLiLib 0.27.20 `ba18a2c6…` (ตรง Modrinth); Rollback คืน Litematica `6faf0156…`, Sodium `4a8733fe…`, Iris 1.10.7 `aae8567b…` ตรง Baseline; APK `393ec8ee…4e02e`
- ข้อจำกัดที่ยังเหลือ: Pending Dependencies สูงสุด 2 โปรเจกต์ (× 2 การ stage); ยังไม่ทดสอบบน iQOO; ไฟล์เกมนอก `mods` (logs, options.txt, config, .fabric) ยังไม่ตรวจ Hash; `AdaptiveSourceSelectionTest` ยังเป็น Known Failure (สาเหตุไม่ทราบ)

### Crash Doctor Fast Track — E2E กรณีแรก (2026-10-14)
- เพิ่มการวินิจฉัย Fabric "ม็อดต้องการม็อดอื่นที่ไม่ได้ติดตั้ง (`… which is missing!`)" → HIGH, ระบุม็อดที่ต้องการ (owner) จากบรรทัดจริง; ปุ่มแก้ไขเปลี่ยนเป็น **Preview → ยืนยัน → Snapshot → ปิดใช้งาน → เริ่มใหม่** (เดิมคลิกแล้วเปลี่ยนไฟล์ทันที)
- E2E บน QA-V3-1 (APK `b1229b48…b3a0`): วิเคราะห์ถูก / Preview ไม่เปลี่ยนไฟล์ / Snapshot APPLIED / เปิดเกมได้ (Main Menu) / Rollback คืนตรง baseline + Quarantine → **PASS**
- ข้อจำกัด: แก้โดยปิดม็อดที่ต้องการ (ไม่ได้ติดตั้ง dependency); Java/Renderer ไม่ได้เพิ่ม; iQOO ไม่ได้ทดสอบ; ไฟล์เกมนอก mods ไม่ได้ตรวจ Hash; `AdaptiveSourceSelectionTest` Known Failure
- commit `d9c26a56`; หลักฐาน `test-artifacts/mod-doctor-v1-v3-qa-20261013/crash/` และ `RESULT.md`

### Crash Doctor Safety Finalization (2026-10-14) — commit `d9c26a56`
- **Crash Recovery ปิดม็อดที่ต้องการ dependency เพื่อให้เกมเปิดได้ และไม่ได้ติดตั้ง dependency** — ม็อดที่ปิดแล้วจะยังปิดอยู่จนผู้ใช้ติดตั้ง dependency เองผ่าน Mod Doctor
- Source: `ModRepairSnapshot.kt` เพิ่ม `disableModWithSnapshot` + `SnapshotDisableResult` (Disabled / Busy / SnapshotRefused / DisableFailed) — ใช้ `ModRepairLock` เดียวกับ Mod Doctor, ปล่อยล็อกใน `finally`, `finalize(succeeded = ผลจริงของการปิด)`
- `ErrorActivity.kt` `applyCrashFix`: เฉพาะ `Disabled` เท่านั้นที่บันทึก learning + `markRelaunchPending` + รีสตาร์ท; `Busy` / `SnapshotRefused` / `DisableFailed` แสดง error dialog และไม่รีสตาร์ท; Preview / Snapshot / Rollback / การยืนยันของผู้ใช้เหมือนเดิม
- Strings EN/TH ใหม่ในรอบนี้: `crash_fix_busy`, `crash_fix_disable_failed`
- Unit: `CrashDisableSnapshotTest` 7/7; ผล `build/test-results` (09:25 หลังแก้ source ครั้งสุดท้าย 09:19): 34 suites / 360 tests, failures 0, errors 0; `AdaptiveSourceSelectionTest` ไม่รันซ้ำ
- APK `0350982769…eab19` (build 09:27) ติดตั้งบน QA-V3-1 และ sha ตรง
- E2E QA-V3-1 **PASS**: failure path (error dialog, ไม่รีสตาร์ท, NO_CHANGE, ล็อกถูกปล่อย); success path (APPLIED, รีสตาร์ท, เข้า Main Menu); rollback (litematica `6faf0156…` ตรง baseline, `.disabled` อยู่ใน quarantine ไม่ถูกลบ, ROLLED_BACK) — หลักฐาน `crash/safety/` และ `RESULT.md`
- **UNVERIFIED บนอุปกรณ์**: เส้นทาง Busy, SnapshotRefused, PARTIAL (unit test เท่านั้น)
- ข้อจำกัด: iQOO ไม่ได้ทดสอบ; Java/Renderer ไม่ได้แก้; ไฟล์เกมนอก mods ไม่ตรวจ hash; ถ้า `finalize` เขียน manifest ไม่สำเร็จ snapshot จะค้างเป็น PENDING แล้วถูกนับเป็น INTERRUPTED (ยังย้อนกลับได้; ไม่มี unit test ครอบคลุม); `AdaptiveSourceSelectionTest` Known Failure
- commit `d9c26a56`; ไม่เริ่มเฟสถัดไป

### Crash Doctor × Mod Doctor Integration (2026-10-14) — ยังไม่ commit
- **การเปลี่ยนแนวทางจากรอบก่อน:** เมื่อ Fabric ระบุ missing dependency ทางหลักคือ **"ติดตั้ง <dependency> และซ่อม"** โดยใช้ Mod Doctor เดิม (scan → preview → fix → re-check → รีสตาร์ท) ส่วน **"ปิดม็อดชั่วคราว"** เป็นทางสำรอง ข้อความเดิมของ Safety Finalization ว่า "ไม่ได้ติดตั้ง dependency" ใช้ได้กับทางสำรองเท่านั้น
- Source: `ModDoctor.kt` (เพิ่มแบบ additive: ช่วงเวอร์ชันของ requirer ถูกใช้กรองการเลือกเวอร์ชันที่จะเสนอ/ติดตั้ง; ตัวกรองการเรียนรู้ไม่รวมคลาส missing dependency), `CrashDependencyRepair.kt` (ใหม่: อ่านหลักฐานจากล็อก + เลือกปัญหาที่ตรงใน Mod Doctor report; ไม่มี downloader/resolver/engine ใหม่), `VersionsManager.loadVersionFromFolder`, `ErrorActivity.kt` / `ErrorScreen.kt` / `AllErrorUi.kt` (ปุ่มหลัก, Preview, notice, ตรวจซ้ำหลังซ่อม), strings EN/TH
- Unit: `game.version.mod.*` 36 suites / 381 tests, failures 0, errors 0 (ใหม่: `CrashDependencyRepairTest` 13/13, `DependencyRangePreviewTest` 8/8)
- APK `ALLLauncher-Debug-0.0.2-beta1.apk` sha256 `d9f48182c313136a5427ff0a32763bc542f992fb1b15bf12a11fd3db677f30b1` (build 11:37, หลังแก้ source ล่าสุด 11:27); compiler warnings 0; ติดตั้งบน QA-V3-1 และ sha256 บนเครื่องตรง
- E2E บน QA-V3-1 (LDPlayer): **PASS** — Play → Mod Doctor gate → "เล่นต่อทั้งที่ยังมีปัญหา" → หน้า crash (ม็อดต้นเหตุ/Dependency/หลักฐาน) → ปุ่มหลัก → Preview (malilib 0.27.20 จาก Modrinth) → ติดตั้ง → snapshot `1791955886250` APPLIED → รีสตาร์ท → เกมเข้า Main Menu → Rollback คืน litematica `6faf0156…` ตรง baseline (malilib ไป quarantine ไม่ถูกลบ, manifest ROLLED_BACK) — หลักฐาน `test-artifacts/mod-doctor-v1-v3-qa-20261013/crash/repair/`
- **UNVERIFIED บนเครื่อง:** เส้นทางไม่มีเวอร์ชันปลอดภัย / ไม่พบ dependency (notice), ปุ่ม "ยกเลิก" ใน Preview, ทางสำรอง "ปิดชั่วคราว" หลังการเปลี่ยนนี้ (regression), การดาวน์โหลดล้มเหลว และ "ยังขาดหลังติดตั้ง"
- ข้อจำกัด: ทางหลักต้องเข้าถึง Modrinth และต้องมีเวอร์ชันที่ตรงช่วง; ปัญหา crash ประเภทอื่นยังไม่ครอบคลุม; iQOO ไม่ได้ทดสอบ; Java/Renderer ไม่ได้แก้; `AdaptiveSourceSelectionTest` ไม่ได้รันซ้ำ
- ยังไม่ commit; ไม่ push; ไม่เริ่มเฟสถัดไปเอง

### Mod Doctor Full QA & Release Readiness (2026-10-14, HEAD `62fdfcc5`) — ยังไม่ commit
- **Verdict: NOT READY** สำหรับการประกาศว่า Mod Doctor/Crash Doctor สมบูรณ์ — ไม่ได้ DONE จาก unit tests อย่างเดียว
- Unit (ดู `test-artifacts/mod-doctor-v1-v3-qa-20261013/qa-matrix-2026-10-14/unit-summary.txt`): รัน `testDebugUnitTest` ทั้งชุด 496 tests, failed 6, skipped 5. แพ็กเกจ `game.version.mod.*` 36 suites / 381 tests, failures+errors = 0. `AdaptiveSourceSelectionTest` 3/3 ผ่านในรอบนี้ (เคยล้มเหลว 13–14 vs ≤12 — เป็น race ของ in-flight requests ก่อน breaker ตัดสิน; ไม่ใช่ Mod Doctor)
- ความล้มเหลวที่เหลือ (ทั้งหมดนอก Mod Doctor logic): `VersionCompareTest` 4 (HMCL `GameVersionNumber` ที่ใช้ตั้งแต่ commit `97119fb6` ต่างจากความคาดหวังเก่าเรื่อง snapshot — Low), `FileTest` 1 และ `MurmurHash2IncrementalTest` 1 (fixture `F:\Download\geckolib-forge-1.21.8-5.2.2.jar` ไม่มีบนเครื่องนี้ — สภาพแวดล้อม, Low)
- เอกสารนับ "12 ประเภท" แต่ `ModDoctorProblemType` มี **11** ประเภท (MISSING_REQUIRED_DEPENDENCY, PACK_REQUIRED_CORE, WRONG_LOADER, WRONG_MINECRAFT_VERSION, DUPLICATE, CORRUPT_METADATA, INCOMPATIBLE_DEPENDENCY_VERSION, INCOMPATIBLE_MOD_PAIR, INCOMPATIBLE_MOD_SET, DECLARED_CONFLICT, LEARNED_LOCAL_CRASH)
- Crash signatures ที่ bundled: 4 (`platform_limit_native`, `mixin_apply_failed`, `missing_required_dependency_fabric`, `loader_fatal_unattributed`)
- Device E2E รอบนี้: **UNVERIFIED ทั้งหมด** ยกเว้นกรณี missing dependency ของ Crash Doctor (รายงานแล้วใน `crash/repair/`). เหตุผล: LDPlayer ไม่เสถียร (PackageManager ค้าง, system_server ถูก watchdog ฆ่า, ldconsole reboot ไม่รีสตาร์ท VM จริง) และยังไม่ได้สร้าง fixture ที่ตรวจ hash ได้สำหรับแต่ละกรณี
- หลักฐาน device ก่อนหน้า (ไม่ได้ re-verify ในรอบนี้, ต้องอ้างเป็นประวัติ): S1/S2/Missing นอกกลุ่ม/S3 (`fdf2a062`, RESULT.md), Wrong-loader E2E (Mod Doctor V2 checkpoint), Download safety (`67624e67`: cancel สดและ world สด UNVERIFIED)
- Matrix ที่ยังไม่มี device evidence ในรอบนี้: Duplicate, Corrupt metadata, Wrong MC ปัจจุบัน, Pack protection, Breaks + หลายปัญหาพร้อมกัน (ปัจจุบัน), Download failure/offline/cancel, Preview cancel, Fix failure, Snapshot refusal, Rollback checksum integrity, Pending-dependency limit (unit-tested เท่านั้น: `MAX_PENDING_DEPENDENCY_PROJECTS = 2`)
- Defect ที่สังเกตได้ (ยังไม่แก้ในรอบนี้, Low): `BatchDownloader.run()` มี `connections.withPermit { }` ว่าง ไม่จำกัดจำนวนงานพร้อมกันระดับไฟล์ (ทางจำกัดจริงอยู่ใน `FileDownloader`) — ไม่ใช่ Mod Doctor และไม่พบผลเสียที่ยืนยันแล้ว
- ไม่พบ Critical/High ที่ยืนยันแล้วใน Mod Doctor scope จากรอบนี้ จึงไม่มีการแก้ source ในรอบนี้
- ยังไม่ commit; ไม่ push; ไม่ reset/stash/clean; ไม่แตะ Main Instance, QA-MD-1, Warehouse, QA Backup, iQOO

### QA Completion Pass (2026-10-14, HEAD 62fdfcc5) — ยังไม่ commit
- Device: LDPlayer `emulator-5554` ตอบสนองได้ในรอบนี้ (uptime ~54s หลัง boot, PackageManager ตอบ, APK ที่ติดตั้งคือ build เดิม); ไม่มีการ reboot/สร้าง VM ใหม่ในรอบนี้
- VersionCompare (ใช้ใน plugin gate และการเรียงลำดับ instance): probe ยืนยันว่า HMCL จัดอันดับ `20w30a` > `1.21.5` และ `25.4-snapshot-N` > `25.4` — เป็นข้อบกพร่องจริงของการเปรียบเทียบ (นอกขอบเขต Mod Doctor). ไม่ได้แก้ในรอบนี้ตามขอบเขต. หลักฐาน `qa-matrix-2026-10-14/versioncompare-probe.txt`
- 6 full-suite failures: VersionCompare 4 = product (ดูด้านบน, Medium สำหรับ snapshot instances); FileTest/MurmurHash 2 = fixture environment (ไฟล์ `F:\Download\...` ไม่มี)
- Device E2E รอบนี้ยังไม่ได้ทำกรณีใหม่ — กรณีที่ต้องใช้ fixture ที่ตรวจ hash ได้และต้องขออนุมัติดาวน์โหลดยังเป็น UNVERIFIED
- Release: NOT READY (ไม่มี Critical/High ที่ยืนยันใน Mod Doctor scope; Blocker = ขาด device matrix)
- ยังไม่ commit; ไม่ push/reset/stash/clean; ไม่แตะ Main Instance, QA-MD-1, Warehouse, QA Backup, iQOO

### QA Device Pass 2 (2026-10-14) — ยังไม่ commit
- Simulated-fixture device cases PASS: Duplicate, Corrupt metadata, gate cancel; baseline restored (hash 6faf0156). ดู qa-matrix-2026-10-14/device-matrix.md
- ปิด UNVERIFIED ได้ 3 กรณี; ที่เหลือและเหตุผลอยู่ใน device-matrix.md; VersionCompare = งานแก้แยก (Medium) ไม่แก้ในรอบนี้

### QA Device Pass 3 (2026-10-14) — ยังไม่ commit
- Simulated E2E detection PASS: Wrong MC, Wrong dependency version, Wrong loader, หลายปัญหาพร้อมกัน, Gate cancel/คืนสภาพ (hash baseline 6faf0156). ไม่มี Repair PASS. FAIL 0. ดู device-matrix.md
- เหลือ UNVERIFIED: wrong-version repair, modpack protection, download failure/offline/cancel, Preview cancel (crash page), fix failure, snapshot refusal, pending limit (unit เท่านั้น)

### QA Device Pass 4 (2026-10-14) — ยังไม่ commit
- PASS: offline download failure (Fix, NO_CHANGE), crash-page offline notice, Preview cancel, fallback ปิดชั่วคราว + เปิดเกม + rollback (real mod), modpack protection (simulated policy). FAIL 0. รวม device cases ปิดแล้ว 13
- UNVERIFIED เหลือ: wrong-version repair, snapshot refusal, pending limit, multi-file rollback ล่าสุด, mixin/platform_limit บนเครื่อง; ต้องขออนุมัติ fixture จริง 1 ชุด (ดู device-matrix.md)

### Real multi-mod QA (2026-10-14, HEAD 62fdfcc5) — ยังไม่ commit
- PASS: Jade (missing fabric-api), Jade 1.21.9 บน 1.21.11, Cloth NeoForge บน Fabric, Mod Menu + YACL (ไม่มี false positive), Lithium + FerriteCore (ไม่มี Conflict ปลอม, เปิดเกมถึง Main Menu), ชุดรวม 7 mods (เปิดเกมถึง Main Menu, ไม่มีปัญหาเทียม). ทุกเคสที่มีการ Repair: Detection/Repair/Launch/Rollback PASS. FAIL 0
- UNVERIFIED เหลือ: pending-dependency limit บนเครื่อง (unit เท่านั้น)
- Baseline QA-V3-1 คืนแล้ว: litematica 6faf0156 เพียงตัวเดียว. ดู device-matrix.md (section Real multi-mod QA)

### Final QA Closure (2026-10-14, HEAD 62fdfcc5) — ยังไม่ commit
- Release Matrix: test-artifacts/mod-doctor-v1-v3-qa-20261013/qa-matrix-2026-10-14/RELEASE_MATRIX.md (รวมหลักฐานเก่า + Real Multi A–F, แยก Tested Repair / Tested Detection / Clean Launch / Unsupported / Unverified)
- Beta (Mod Doctor, ขอบเขตที่ทดสอบ): Ready แบบมีเงื่อนไข. Detection/Repair/Launch/Rollback PASS ทุกเคสที่ยืนยันบนเครื่อง, FAIL 0
- เพิ่ม unit สำหรับ MAX_PENDING_DEPENDENCY_PROJECTS=2 (ขีดจำกัดเกิน = ไม่ซ่อม, ไม่เปลี่ยนไฟล์) และ mutation check ยืนยันว่าเทสต์จับขอบได้. Device ยัง UNVERIFIED (ต้องมี breaks graph ที่มี provider จริง 3 ตัว)
- บั๊กจริง (แก้แล้ว, ไม่ใช่ Mod Doctor logic): จำนวนม็อดบนการ์ด Instance ถูกแคชตอนเปิดหน้าต่าง ไม่อัปเดตเมื่อ mods เปลี่ยน. แก้ด้วย produceState รีเฟรช 2 วินาที + countInstalledModJars (นับเฉพาะ .jar/.jar.disabled ระดับบน ไม่ใช่ nested). Device ยืนยันแล้ว (1→2→1)
- Full unit suite: 500 tests, 7 failed, 5 skipped. 4 = VersionCompare (product, Medium, ไม่แก้รอบนี้), 2 = fixture F:\Download ไม่มีในเครื่อง, 1 = AdaptiveSourceSelectionTest flake (ผ่านเมื่อรันเดี่ยว)
- UNVERIFIED บนเครื่อง: pending limit, snapshot refusal, crash signatures จริง, download cancel กลางทาง (พบช่องว่าง: ไม่มีเทสต์พิสูจน์การลบไฟล์ค้างหลัง cancel), wrong-version repair
- Mod Doctor ไม่ใช้ VersionCompare (ใช้ ModGroupPlanner.compareVersionText) — defect VersionCompare ไม่กระทบ logic ซ่อม แต่กระทบการเรียงเวอร์ชันและ plugin gate
- Baseline QA-V3-1 = litematica 6faf0156 เพียงตัวเดียว. ไม่ commit/push

### STEP 1: Download Safety Completion (2026-10-14) — ยังไม่ commit
- รายละเอียด/หลักฐาน/แฮช: test-artifacts/mod-doctor-v1-v3-qa-20261013/qa-matrix-2026-10-14/download-safety/RESULT.md
- แก้ 4 บั๊กจริง: D1 (High) timeout ดาวน์โหลดถูกกลืนเป็นการยกเลิก (ไม่รายงาน error/สำเร็จ, Mod Doctor ค้างรอ) → withDownloadTimeout แปลงเป็น TimeoutException; D2 (Medium) onCancel วิ่งชนกับงานที่ยังเขียนไฟล์ และ retry ด้วย id เดิมถูกยกเลิกการลงทะเบียน → TaskSystem รอให้งานเก่าหยุดก่อน; D3 (Medium) jar/zip ที่ไม่มี sha1 ไม่ถูกตรวจว่าเป็นไฟล์ zip จริง → unreadableArchiveReason + แจ้ง error; D4 (Medium) Retry หลัง Fix ล้มเหลวข้ามรายการที่ล้มเหลว (ต้องกด 2 ครั้ง) → fixProblems re-inspect เมื่อมี marker repair incomplete
- Device PASS 9/9 บน QA-V3-1 (Cancel ก่อนเริ่ม, Cancel กลางทาง 17/52.8MB, Retry หลัง Cancel, เน็ตหลุดกลางทาง, ออฟไลน์ตั้งแต่เริ่ม, Retry หลังล้มเหลว, Mod Doctor Fix ออฟไลน์ = NO_CHANGE, Retry ครั้งเดียวติดตั้ง 2 dependency, คืน baseline) — ทุกครั้งไม่มี .part/ไฟล์เสีย/การทับไฟล์เดิม และแฮชตรงกับ Modrinth
- Unit/Integration ใหม่ 28 เทสต์ (MockWebServer ควบคุมจังหวะ: cancel กลางทาง/สุ่มเวลา, ตัดการเชื่อมต่อ, 500/404, hash ไม่ตรง, ไฟล์ไม่ครบ, retry) ผ่านหมด. Mutation ยืนยันเทสต์ timeout. Full suite 528 tests / 6 ล้มเหลวที่รู้แล้ว (VersionCompare 4, fixture 2) / 5 skipped
- UNVERIFIED บนเครื่อง: hash ไม่ตรง/HTTP error/ข้อมูลเสียจากเซิร์ฟเวอร์ (unit เท่านั้น), เส้นทาง timeout 5 นาที. ข้อจำกัด: สตรีมไม่มี sha1/ไม่มี Content-Length ที่ขาดกลางทางตรวจไม่ได้ (มี zip check เฉพาะ jar/zip); ไฟล์ .part-uuid/.bak อาจเหลือถ้าโปรเซสถูกฆ่ากลางติดตั้ง (ไม่โหลดเป็นม็อด)
- เหตุการณ์ infra: pm install APK 415MB ทำ PackageManager ของ LDPlayer ค้างอีก; adb offline หลังสลับ Wi-Fi; restart instance เดิม (index 0) 2 ครั้ง ไม่สร้าง VM ใหม่
- ไม่มี Critical/High ที่ยังเปิดในขอบเขต Download Safety. Next: รออนุมัติ commit (รวมงานก่อนหน้าที่ยังไม่ commit: Mod Count, pending-limit tests)
- Final Download Safety Guard: TaskSystem ไม่รัน cleanup ขณะ job เดิมยังเขียน (reproduce ด้วย TaskSystemStuckCancelTest แล้วแก้), retry ที่ id เดิมถูกปฏิเสธพร้อม error ขณะ job ค้าง, งานเก่าไม่ unregister งานใหม่. Unit ผ่าน 39 เทสต์ที่เกี่ยวข้อง; ไม่มี device re-run (เส้นทางค้างเกิน 10 วินาทีสร้างบนเครื่องจริงไม่ได้อย่างปลอดภัย)

### STEP 2: Dependency & Repair Completion (2026-10-14) - ยังไม่ commit
- หลักฐาน: test-artifacts/mod-doctor-v1-v3-qa-20261013/qa-matrix-2026-10-14/step2/RESULT.md
- แก้บั๊ก: evaluator ไม่รู้จัก ~ และ ^ (YACL จริงประกาศ minecraft ~1.21.11) ให้ expandVersionShorthand ใน versionPredicateDecision; เทสต์ใหม่ RealMetadataRangeTest 4 + VersionShorthandTest 3
- Device PASS (QA-V3-1, APK 10069d1f): E1 Detection ถึง Rollback ด้วยม็อดจริง (fabric-api c98467cb + malilib ba18a2c6, hash ก่อน/หลัง/quarantine ตรง), E2 ซ่อมบางส่วนรายงานตามจริง, E3 ไม่มี candidate แจ้งเหตุผลและไม่แก้ไฟล์, E4 dependency ผิดเวอร์ชันตรวจพบแต่ไม่มี Fix (ตามดีไซน์)
- UNSUPPORTED: เปลี่ยน dependency ผิดเวอร์ชันอัตโนมัติ. UNVERIFIED บนเครื่อง: pending limit 2 (ต้อง fixture breaks จริง), กรณีผิดเวอร์ชันของจริง (ต้อง fabric-api รุ่นเก่า)
- Full suite 537 tests ล้มเหลว 6 ที่รู้แล้ว. ไม่ commit/push

### STEP 2 Completion: ซ่อม dependency ผิดเวอร์ชัน (2026-10-15) - ยังไม่ commit, STEP 2 ยังไม่ CLOSED
- หลักฐาน: qa-matrix-2026-10-14/step3/RESULT.md
- ต่อยอด Mod Doctor เดิม: ModDoctorProblem.dependencyVersionFix + resolveLibraryVersionFix (project จาก fingerprint เท่านั้น) + ModDoctorMultiModRepair.repairLibrary (ใช้ solver/staging/apply/snapshot เดิม, LIBRARY_REPAIR_BOUNDS = ห้ามเปลี่ยนม็อดอื่น) + Preview EN/TH + ป้องกันไฟล์ของ modpack
- Unit/Integration ผ่าน (557 tests, 6 ล้มเหลวที่รู้แล้ว): upgrade, downgrade, shared conflict, ไม่มี candidate, breaks ใหม่, ดาวน์โหลดล้มเหลว, pack-owned, rollback, pending limit 2
- Device PASS (ไม่ดาวน์โหลดใหม่): DA ตรวจพบ+Preview, DB ปฏิเสธเมื่อไม่มีเวอร์ชันร่วม, DC ปฏิเสธเมื่อระบุ project ไม่ได้. ยังไม่ได้ verify บนเครื่อง: Repair/Launch/Rollback จริง (รออนุมัติ fabric-api 0.139.5 และ 0.140.2)

### STEP 2 Final (2026-10-15) - ยังไม่ commit
- หลักฐาน: qa-matrix-2026-10-14/step3/FINAL.md
- ซ่อม dependency ผิดเวอร์ชันจริง: upgrade (fabric-api 0.139.5 + YACL 3.8.2 → 0.141.6) ครบ Detection/Preview/Repair/Rescan/Launch/Rollback PASS พร้อม hash และ quarantine; downgrade (0.141.6 → 0.140.2, dependent จำลอง) ครบทุกขั้น PASS; shared dependency ไม่มีเวอร์ชันร่วม = ปฏิเสธ
- บั๊กที่แก้: solver ดาวน์โหลด build ที่อยู่นอกช่วง (0.141.1, 0.141.0) ก่อนถูกปฏิเสธ → กรองด้วย requirements ก่อน staging (ModDoctorReplacementFix.requirements) พร้อมเทสต์ และยืนยันบนเครื่อง (ดาวน์โหลดเฉพาะ 0.140.2)
- Pending limit: unit/integration เท่านั้น (ไม่มี fixture ที่ทำบนเครื่องได้อย่างปลอดภัย). Full suite 559 tests, 6 ล้มเหลวที่รู้แล้ว. Baseline QA-V3-1 = litematica 6faf0156. ไม่ commit/push

### STEP 3: Snapshot, Rollback, Crash Doctor (2026-10-15) - ยังไม่ commit
- หลักฐาน: qa-matrix-2026-10-14/step4/RESULT.md
- แก้ ModRepairSnapshot: ตรวจพื้นที่ว่างก่อนสำรอง (ไม่พอ = Refused ก่อนเขียนอะไร), restore ผ่านไฟล์ชั่วคราวพร้อมตรวจ SHA-1 (ไม่มีไฟล์ครึ่งๆ ใช้ชื่อเดิม), ตรวจผลจริงก่อนประกาศ ROLLED_BACK (ไม่ตรง = ROLLED_BACK_PARTIAL)
- Unit (fault injection) 12 + Crash Doctor safety 7: refusal 5 แบบ, rollback หลายไฟล์ทุก hash, interrupted ทุกขั้น, rollback ล้มเหลวแล้วลองใหม่ได้, invariant ของ signature (เฉพาะ HIGH ที่ระบุเจ้าของได้เท่านั้นที่ DISABLE)
- Device (QA-V3-1, APK bce89e01): CD1 missing dependency บน crash page Repair/Rescan/Launch/Rollback PASS; CD2 loader fatal ไม่มีวิธีซ่อมปลอดภัย = LOW ไม่มี action ไม่แก้ไฟล์; MF rollback 4 ไฟล์ hash ตรงทุกตัว; INT ฆ่าแอปกลาง repair = PENDING แล้ว rollback ได้ตามจริง
- UNVERIFIED คงเหลือ: pending limit บนเครื่อง, mixin/native crash จริง, refusal ที่ต้องทำให้ filesystem เสีย (unit เท่านั้น). Low: AdaptiveSourceSelectionTest ไม่เสถียร (engine ไม่ได้แก้). ไม่ commit/push

### STEP 4 Final (2026-10-15) - ยังไม่ commit
- หลักฐาน: qa-matrix-2026-10-14/step5/FINAL.md
- Full suite 585 tests, 0 failures, 5 skipped (benchmark). APK สุดท้าย sha256 0A1CF33939266FC3FC107A30BDBE3F53B26C99F5ADA37595DF99FD33119D4333
- VersionCompare: สาเหตุคือ stub versions.txt ใน test resources บังตารางจริง (0 vs 504 snapshots) ไม่ใช่บั๊ก HMCL/โปรดักชัน; build ก๊อปตารางจริงเข้า test classpath. FileTest/Murmur ใช้ข้อมูลสร้างเอง. AdaptiveSourceSelectionTest ขอบบนผิด (เทียบ 40 รอบ: 2-15) แก้เป็นขอบที่คำนวณได้ + stress test
- Regression บน APK สุดท้าย: cancel download, crash-page repair + rollback PASS; baseline QA-V3-1 = litematica 6faf0156
- iQOO: แผนรออนุมัติ (step5/IQOO_PLAN_PENDING_APPROVAL.md) ยังไม่แตะมือถือ
