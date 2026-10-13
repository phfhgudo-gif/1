# Thanpisitkub Power (Forge 1.20.1)

พลังใหม่ชื่อ **thanpisitkub** มี 2 ท่า: **Galaxy Impact** (ท่าที่ 1) และ **Galaxy Divide** (ท่าที่ 2)
ใช้เสียง, particle (จาก textures ทั้ง 130 รูป), และ player animation ที่คุณให้มา

mod id: `thanpisitkub` · Minecraft 1.20.1 · Forge 47.x · Java 17

## ต้องมี
- **Player Animation Library** (KosmX/Zigy) สำหรับ Forge 1.20.1 — ใช้เล่นไฟล์ `player_animation/*.json`
  - ตอน dev: `build.gradle` ดึงจาก `https://maven.kosmx.dev/` (`pal_version` ใน `gradle.properties`)
  - ตอนเล่น: ใส่ jar ของ Player Animation Library ลงโฟลเดอร์ `mods`
  - ถ้าไม่มี ม็อดยังโหลดได้ แต่ท่าทางตัวละครจะไม่ขยับ (particle/เสียงยังทำงาน)

## Build
(ต้องใช้ **JDK 17** รัน Gradle — ตั้ง `JAVA_HOME` เป็น JDK 17)
```
./gradlew build          # ได้ไฟล์ใน build/libs/
./gradlew runClient      # ทดสอบในเกม
```

## วิธีเล่น (ผ่านม็อด Origins)
ต้องติดตั้งม็อด **Origins** (1.20.1 Forge) คู่กับม็อดนี้
1. เลือก Origin **Thanpisitkub** ตอนเข้าโลก (หรือใช้ Orb of Origin) — หรือพิมพ์
   `/origin set @s origins:origin thanpisitkub:thanpisitkub`
2. กดปุ่มของ Origins: **Primary Active Ability** = Galaxy Impact · **Secondary Active Ability** = Galaxy Divide
   (ตั้งปุ่มได้ใน Options → Controls → หมวด Origins) คูลดาวน์ 60 วินาที แก้ได้ที่ `cooldown` (หน่วย tick) ใน
   `data/thanpisitkub/powers/*.json`
3. ระหว่างใช้ท่า: เล่น animation + particle + เสียง, ตัวละครถูกล็อกการเคลื่อนที่, กล้องเป็นมุมมองบุคคลที่สาม,
   ไม่ได้รับดาเมจ (ปรับได้ใน config)

ไม่มีไอเท็มและไม่มีปุ่มของม็อดนี้เองแล้ว
คำสั่ง: `/thanpisitkub cancel` (ทุกคน, ยกเลิกท่าตัวเอง), op: `/thanpisitkub cast <galaxy_impact|galaxy_divide> [ผู้เล่น]`
ทดสอบ particle เดี่ยว ๆ: `/particle thanpisitkub:garp_galaxydivide1 ~ ~2 ~`

## Config
- `config/thanpisitkub-common-v2.toml`: ตัวคูณดาเมจ, invulnerableWhileCasting
- `config/thanpisitkub-client-v2.toml`: **effectScale** (ค่าเริ่มต้น 1.0), **particleDensity** (0.6), cameraShake, **soundVolume** (0.6), **originalAnimation** (false = ท่า 'safe', true = ท่าต้นฉบับ), lock/third-person

`effectScale = 1.0` = ตัวเลขขนาด/รัศมี/ความเร็วตามไฟล์ particle ต้นฉบับเป๊ะ ๆ (ขนาดอนิเมะ ใหญ่หลายสิบบล็อก)
ถ้าเอฟเฟกต์ใหญ่จนแลค (โดยเฉพาะมือถือ) ให้ลด `particleDensity` เป็น 0.2-0.4 ก่อน
**หมายเหตุ:** เวอร์ชันนี้ใช้ชื่อไฟล์ config ใหม่ (`-v2`) เพื่อทิ้งค่าเก่า (effectScale 0.25) ที่ค้างจากรอบก่อน — ไฟล์ `thanpisitkub-client.toml` / `thanpisitkub-common.toml` เก่าลบทิ้งได้

## ไฟล์ที่ควรรู้
| ไฟล์ | หน้าที่ |
|---|---|
| `move/Moves.java` | **ไทม์ไลน์ของทั้ง 2 ท่า** (เวลา, particle ไหน, เสียงไหน, ดาเมจ) แก้ตัวเลขได้เลย |
| `assets/.../bedrock_particles/*.json` | พฤติกรรม emitter ของ particle แต่ละชุด (โหลดใหม่ด้วย F3+T) |
| `assets/.../particles/*.json` | รายการ texture (เฟรม animation) ของ particle แต่ละชุด |
| `assets/.../player_animation/*.json` | animation ตัวผู้เล่น |
| `assets/.../sounds.json` + `sounds/*.ogg` | เสียง |

## Origins (ทางเลือก)
มีไฟล์ datapack ให้ใน `data/thanpisitkub/` (origin `thanpisitkub` + power 2 ตัวที่เรียก `/thanpisitkub cast ...`)
ใช้ได้เมื่อลงม็อด Origins/Apoli สำหรับ Forge 1.20.1 — ปุ่มความสามารถหลัก = Galaxy Impact, รอง = Galaxy Divide
(ถ้าไม่ได้ใช้ Origins ไฟล์พวกนี้ไม่มีผลอะไร)


## เรื่อง Animation (Player Animation Library 1.0.2-rc1+1.20)

- PAL 1.0.2 อ่านค่า rotation แบบ array เป็น **step (ค้างค่า ไม่ interpolate)** ถ้า keyframe ห่างกัน ท่าจะกระตุก/ค้างเป็นช่วง ๆ
  จึงแปลงไฟล์ท่าให้มี keyframe **ทุก tick** ด้วย `tools/resample_animation.py` (ไฟล์ต้นฉบับของคุณเก็บไว้ที่ `tools/original_animations/`)
- ท่ามี 2 แบบ: `galaxy_*.json` (safe: จำกัดการพลิกลำตัว/การยกตัวที่สุดโต่ง) และ `galaxy_*_original.json` (ท่าต้นฉบับ) สลับด้วย `originalAnimation` ใน client config
- ถ้าอยากแก้ท่าเอง: แก้ไฟล์ใน `tools/original_animations/` แล้วรัน `python3 tools/build_animations.py`
- (แก้คำอธิบายเดิม: ก่อนหน้านี้เคยบอกว่า bug มาจาก rotation ถูกอ่านเป็น radian — ตรวจโค้ดจริงแล้วไม่ใช่ PAL แปลงองศาเป็น radian ให้อยู่แล้ว)

## ข้อควรรู้

- โค้ดยังไม่ได้คอมไพล์กับ Forge / ทดสอบในเกมจริง (ทดสอบได้เฉพาะส่วนตรรกะที่ไม่ใช้ Minecraft)
- ถ้าเกมกระตุก (มือถือ) ลด `particleDensity` ใน client config เป็น 0.2–0.3
