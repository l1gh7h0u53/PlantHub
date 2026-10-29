# PlantHub 🌿

> **Privacy-First, 100% Offline Botanical Cultivation Tracker & Lifecycle Ledger**

PlantHub is a vibe coded zero-trust, local-first Android application designed for craft growers and plant enthusiasts. It operates with zero network permissions, storing all records, care metrics, telemetry, and high-resolution photos strictly inside the device's local sandbox.

---

## ⚠️ Vibe Coding Disclaimer & Warning

> **Notice to Contributors & Users:**
> This repository and core codebase were built leveraging **vibe coding** principles (AI-assisted rapid prototyping and expressive development iterations). 
> 
> - **Refactor Before Production:** While functional and strictly offline, expect idiosyncratic patterns or rapid-prototyping artifacts in parts of the codebase.
> - **Verify Changes:** If you are contributing code or modifying Room schemas/migrations, please thoroughly audit and test data migrations locally.
> - **Self-Sovereignty:** Because the app is 100% offline, **always back up your local ZIP/JSON data** via the settings tab before performing major app upgrades or manual code updates.

---

## ✨ Features Overview

### 🌿 1. Cultivar Profile & Fleet Management
* **Multi-Plant Tracking:** Manage distinct cultivars with custom identifiers, strain details, and botanical icons (*Bonsai, Cannabis, Flower, Herb, Seedling, Tropical*).
* **Lifecycle Stages:** Track plant progression through *Seedling, Vegetative, Flowering, Autoflower, Drying, Curing,* and *Harvested* with visual stage badges.
* **Substrate & Container Specs:** Select substrate mediums (*Living Organic Soil, Coco/Perlite, DWC Hydroponics, Peat Moss, Rockwool, Super Soil*) organized in a Material 3 segmented list with container volumes in Liters.
* **Cultivar Strategy Notes:** Record phenotype traits, nutrient regimens, and strain-specific growth objectives.

### ☀️ 2. Precision Photoperiod & Light Cycle Ticker
* **Preset & Custom Schedules:** Configure vegetative (`18/6`), generative flowering (`12/12`), or autoflowering (`20/4`) light cycles.
* **Custom Turn-On Times:** Set the exact fixture turn-on time using a native Material 3 time picker.
* **Live Light Phase Ticker:** Real-time counter displaying whether lights are currently **ON** or **OFF**, accompanied by an exact countdown to the next phase transition.
* **Status Bar Live Notification:** Optional Android status bar chip displaying real-time photoperiod status without opening the app.

### 📊 3. Care Activity Ledger & GitHub-Style Heatmap
* **16-Week Activity Heatmap:** Developer-inspired contribution grid displaying cultivation consistency, watering intervals, and feed frequency at a glance.
* **One-Tap Quick Actions:** Fast action bottom sheets for quick logging:
  * 🚿 Watering
  * 🧪 Nutrients
  * ✂️ Defoliation
  * 🎗️ Low-Stress Training (LST)
  * 🌊 Flush
  * 🌧️ Misting
* **Precision Telemetry:** Log solution pH, runoff pH, electrical conductivity (EC / PPM), nutrient mix details, and care observation notes.

### 📸 4. Milestone Photo Progression Vault
* **Visual Canopy Analytics:** Capture high-resolution milestone photos comparing Day 1 baselines against 7-day weekly checkpoints.
* **Height & Growth Differentials:** Track vertical canopy growth (cm) and calculate automated height deltas between photo milestones.
* **Hardware-Sandboxed Storage:** Photos are saved directly to internal private storage (`context.filesDir/milestone_photos`), completely isolated from public galleries and third-party cloud scrapers.

### 💾 5. Data Sovereignty, Portability & Backups
* **Complete ZIP Bundle Backup:** Export and restore the entire SQLite database bundled with original-resolution milestone photos in a single `.zip` archive.
* **Universal CSV Ingestion:** Multi-table CSV export and import with header normalization (`camelCase`, `snake_case`, human headers) and automatic primary cultivar linking.
* **Standard JSON Backups:** Portable JSON export and restore for external scripting or custom data analysis pipelines.
* **Automated Folder Backup:** Schedule weekly automatic background backups to any user-selected device folder via the Android Storage Access Framework (SAF).

### 🔒 6. 100% Offline & Zero-Trust Privacy
* **Zero Internet Permissions:** `android.permission.INTERNET` is completely excluded from `AndroidManifest.xml`. The operating system kernel physically blocks all external network socket connections.
* **No Cloud Accounts or Telemetry:** Zero logins, zero passwords, zero remote tracking SDKs, zero ads, and zero cloud databases.
* **Local-First Architecture:** Powered entirely on-device by Jetpack Room ORM (SQLite) and local Android system alarms.

### 🎨 7. Modern Material 3 Expressive UI
* **First-Launch Onboarding Carousel:** Interactive multi-step guided tour explaining photoperiods, ledgers, vaults, and data portability in simple and technical language (replayable from Settings).
* **Clean Garden Zero-State:** Fresh installs start with an uncluttered garden and an immediate *"Add First Plant"* action.
* **High-Contrast Dark Mode & Dynamic Color (Monet):** High-contrast typography and solid container pills across the Floating Bottom Menu Bar, Photo Vault cards, and lifecycle dialogs.

---

## 🛠 Tech Stack

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose / Material 3 Expressive
* **Local Database:** Jetpack Room ORM (SQLite)
* **Background Tasks:** Android WorkManager & AlarmManager
* **Storage Access Framework (SAF):** Local device backup exports and imports
* **Image Management:** Coil / Android Internal Sandbox Storage (`context.filesDir`)

---

## 🔒 Security & Permission Audit

```xml
<!-- AndroidManifest.xml -->
<!-- Explicitly NO internet permission requested -->
<!-- <uses-permission android:name="android.permission.INTERNET" /> -->
```

PlantHub is strictly constrained to the local device sandbox. No network sockets are initialized, ensuring 100% air-gapped data sovereignty.

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for details.
