
# ScanPro AI 📄✨

<p align="center">
  <img src="app/src/main/res/drawable/scanpro_logo.png" width="130" alt="ScanPro AI Logo" style="border-radius: 24px;"/>
</p>

<p align="center">
  <strong>Next-Generation AI Document Scanner, Multi-Format Office Suite & Cloud Vault</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform"/>
  <img src="https://img.shields.io/badge/Language-Kotlin-blue.svg?logo=kotlin" alt="Language"/>
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?logo=jetpackcompose" alt="Compose"/>
  <img src="https://img.shields.io/badge/AI-Google%20Gemini%202.5-orange.svg" alt="Gemini"/>
  <img src="https://img.shields.io/badge/Min%20SDK-24-informational.svg" alt="Min SDK"/>
  <img src="https://img.shields.io/badge/License-MIT-purple.svg" alt="License"/>
</p>

---

## 📌 Overview

**ScanPro AI** ek all-in-one smart document scanner aur productivity suite hai jo Google Gemini AI aur computer vision (OpenCV) par mabni hai. Yeh sirf scans capture nahi karta balke documents ko summarize, translate, edit, secure aur convert karne ki mukammal sahulat deta hai.

---

## 📱 Screenshots

| 🏠 Home Dashboard | 📷 Smart Scanner | 🔍 OCR & AI Chat | 🛠️ PDF Tools Suite | 👤 Profile & Vault |
|:---:|:---:|:---:|:---:|:---:|
| <img src="screenshots/home.png" width="180" alt="Home"/> | <img src="screenshots/scanner.png" width="180" alt="Scanner"/> | <img src="screenshots/ocr.png" width="180" alt="OCR"/> | <img src="screenshots/tools.png" width="180" alt="Tools"/> | <img src="screenshots/profile.png" width="180" alt="Profile"/> |

---

## ✨ Key Features

### 📸 1. Smart Camera Scanner
- **Live Edge Detection:** OpenCV par mabni real-time border detection aur automatic 4-point perspective warp.
- **Multiple Scan Modes:** Documents, ID Cards (Front & Back auto alignment), Books (curvature flattening), Receipts, Whiteboard, aur Presentations.
- **Camera Controls:** Flash / Torch hardware toggle, pinch-to-zoom (1x - 5x), rule-of-thirds grid aur tap-to-focus reticle.
- **Smart Color Filters:** Original, Magic Color Enhance, B&W Clean, Grayscale aur Invert.

### 🤖 2. Gemini AI Document Intelligence
- **AI OCR Text Extraction:** Image aur scan se text nikal kar editable format mein tabdeel karein.
- **Executive Summarizer:** Badi reports aur contracts ka sub-second executive summary generate karein.
- **Contextual Document Q&A:** Scan kiye gaye document se sawal karein aur Gemini AI se context ke mutabiq jawab payein.
- **Language Translation:** 50+ zabanon mein accurate neutral translation.
- **Grammar Polish & Typo Fix:** OCR aur spelling mistakes ki automatic correction.

### 🛠️ 3. Complete PDF Studio & Security
- **Merge & Split:** Multiple PDF files ko aapas mein jodein ya specific page ranges (e.g. `1-3, 5, 8-10`) extract karein.
- **Real PDF Encryption:** PDFBox ke zariye real AES-128 password encryption (files kisi bhi standard PDF viewer mein PIN ke baghair nahi khulengi).
- **Watermark & e-Sign:** Custom security stamps lagayein ya live signature draw karke document par stamp karein.
- **PDF Compressor:** File size optimize karein image rendering algorithms ke sath.

### 💼 4. Multi-Format Office Suite & Viewers
- **Word Editor (.docx / .txt):** Rich-text editor with tables, formatting, and one-click PDF export.
- **Excel Spreadsheet (.xlsx / .csv):** Grid calculation viewer aur editor.
- **PowerPoint Presenter (.pptx):** Interactive slide viewer with built-in digital laser pointer mode.
- **Code & Web Previewer (.html / .json / .xml):** Syntax formatted editor with live WebView rendering.
- **EPUB & ZIP Manager:** E-book reading canvas with Day/Sepia/Night modes aur ZIP archive extraction.

### 🔐 5. Offline Cache & Cloud Vault
- **Room SQLite Persistence:** Offline mode mein sabhi scanned documents aur AI chat history phone ke local database mein mehfooz rehti hai.
- **Firebase Authentication:** Email/Password, Phone OTP SMS verification aur One-Tap Guest Vault login.
- **Edge-to-Edge Adaptive UI:** Har phone ke aspect ratio, display cutouts aur 3-button navigation bar ke sath fully compatible.

---

## 🛠️ Tech Stack & Architecture

- **Architecture:** MVVM (Model-View-ViewModel) with Clean Architecture & Repository Pattern
- **Language:** Kotlin 2.2+
- **UI Framework:** Jetpack Compose (Material Design 3)
- **Camera Engine:** AndroidX CameraX (`1.5.0`)
- **Computer Vision:** Custom OpenCV Edge Detector & Perspective Transform Matrix
- **AI Backend:** Google Gemini 2.5 Flash API (OkHttp / JSON)
- **PDF Core:** Apache PDFBox for Android (`pdfbox-android:2.0.27.0`)
- **Local Storage:** Room Database with KSP + DataStore Preferences
- **Backend / Auth:** Firebase Auth, Firestore, Play Services Credentials
- **Image Loading:** Coil Compose

---

## 🚀 Getting Started

### Prerequisites
* **Android Studio:** Ladybug (2024.2+) ya Meerkat
* **JDK:** Version 17 ya 21
* **Android SDK:** Min SDK 24 (Android 7.0) | Target SDK 36 (Android 15)

---

### Installation & Setup

1. **Repository Clone karein:**
   ```bash
   git clone https://github.com/your-username/ScanPro-AI.git
   cd ScanPro-AI

2.  Environment Variables Configure karein (.env): Project ke root folder mein
    .env file banayein (ya .env.example ko copy karein) aur apna Gemini API key
    add karein:

    GEMINI_API_KEY=your_gemini_api_key_here

    (Gemini API key aap Google AI Studio se free mein le sakte hain).

3.  Firebase Setup (Optional lekin Recommended):

      - Firebase Console par naya project banayein.
      - Package name com.aistudio.scanproai.app add karein.
      - google-services.json file download karke ScanProAi-main/app/ folder mein
        daal dein. (Note: Agar Firebase file na ho, tab bhi app Guest Vault aur
        offline features ke sath chale gi).

4.  Project Build karein:

      - Android Studio mein project open karein.
      - Gradle Sync run karein.
      - Device ya Emulator connect karke Run (Shift + F10) dabayein.

📂 Project Directory Structure

ScanProAi-main/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── data/
│   │   │   │   │   ├── ai/          # Gemini API Client & Prompts
│   │   │   │   │   ├── auth/        # Firebase & Guest Session Repository
│   │   │   │   │   └── local/       # Room DB, Entities & DAOs
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/     # Compose UI Screens (Scanner, PDF, Office, Tools)
│   │   │   │   │   ├── theme/       # Material 3 Color Schemes & Typography
│   │   │   │   │   └── ScanProViewModel.kt
│   │   │   │   └── util/            # OpenCV Detector, PdfEngine (PDFBox)
│   │   │   └── res/                 # Vector Drawables, Strings, Icons
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
└── README.md

🔒 Security & Privacy

  - Local-First Processing: Camera stream aur document edge detection mukammal
    tor par on-device execute hote hain.
  - Zero Silent Failures: Authentication aur file encryption fail-closed
    architecture par chalti hain.
  - PIN Protected: Password protected PDFs standard 128-bit AES encryption use
    karte hain.

📄 License

Yeh project MIT License ke tehat licensed hai. Mazeed tafseelat ke liye LICENSE
file dekhein.
