# 💇 Contactify
Business Card Scanning and Management Application

**Contactify** is a mobile app that digitizes business cards using **on-device OCR** and **AI-powered categorization**, offering secure, offline-first contact management with multilingual support.

---

## ✨ Features
- **On-Device OCR**: Scan and extract business card data without internet (ML Kit based).
- **Structured Contact Fields**: Auto-fill names, phone numbers, emails, job titles, companies, websites, and addresses.
- **Industry and Field Categorization**: Organize contacts hierarchically: *Industry ➔ Field ➔ Contact*.
- **Offline Functionality**: No cloud storage needed; complete privacy.
- **User-Friendly Interface**: Designed with Figma prototypes, built using Android Studio and Java.
- **Free and Accessible**: No subscriptions or hidden fees.
- **Manual & Future Auto-Categorization**: Currently manual; automatic categorization planned in future updates.

---

## 🏗 App Structure
- `MainActivity`: Start page to select camera/gallery.
- `ReviewActivity`: Review and edit scanned contact fields.
- `DatabaseHelper`: SQLite DB handler for contacts, industries, and fields.
- `IndustryFieldHelper`: Load industry/field data from assets.
- `ContactListActivity`: Display saved contacts categorized under industry/field.
- `EntityExtractionHelper`: Extract structured fields (email, phone, address, etc.) from scanned text.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest version)
- Java (8 or higher)
- ML Kit dependency
- Gradle build system

### Installation
```bash
git clone https://github.com/ezxuen/Contactify
cd Contactify
```
- Open the project in **Android Studio**.
- Sync Gradle.
- Run on emulator or Android device.

---

## 📋 Changes & Future Enhancements
- ➡ **Moved Automatic Categorization** to future releases.
- ➡ **Completed Industry/Field database integration** (Excel-based).
- ➡ **Enhanced manual review/edit flow** before saving contacts.

Future Plans:
- **Automatic industry and field suggestion** based on job title/company name.
- **Optional cloud backup (with user consent)**.
- **iOS Version**.

---

## 🧑‍💻 Contributors
- Hassan Yahya Alzahrani
- Suhayb Medhesh
- Bassam Kasar
- Ahmad Firas Alnajjar
- Mohammed Alsulaiman
- Ali Hussain Alfilfil

**Supervisor**: Dr. Saeed Matar Alshahrani

---

## 🔒 License
This project is licensed under a **Proprietary License**.  
© 2025 Hassan Yahya Alzahrani. All rights reserved.
