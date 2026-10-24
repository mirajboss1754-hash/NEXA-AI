# Nexa AI — Android starter project

এই প্রজেক্টটি Nexa AI-এর প্রথম UI prototype। এতে আছে:
- Dark chat interface
- Android Speech Recognition দিয়ে বাংলা voice input (ফোনে সমর্থিত service থাকলে)
- Android Text-to-Speech দিয়ে উত্তর শোনার বাটন
- প্রাথমিক demo replies

**গুরুত্বপূর্ণ:** এই সংস্করণে অনলাইন AI backend/API এখনো যুক্ত নেই। তাই এটি এখনো ChatGPT-এর মতো উত্তর দিতে পারে না। API key সরাসরি Android APK-তে রাখা নিরাপদ নয়; AI যুক্ত করার সময় server-side proxy ব্যবহার করা উচিত।

## APK build করার সহজ cloud পদ্ধতি (Android ফোন থেকেও)

1. GitHub-এ sign in করে নতুন public বা private repository তৈরি করো।
2. এই ZIP-এর ভেতরের ফাইলগুলো repository-তে upload করো (ZIP-টিকে শুধু একটি ফাইল হিসেবে নয়—extract করে ভেতরের ফাইলগুলো upload করতে হবে)।
3. GitHub Actions-এ `Build Android APK` workflow চালাও।
4. workflow সফল হলে Actions run-এর Artifacts অংশ থেকে `NexaAI-debug-apk` download করো।
5. ZIP artifact extract করে `app-debug.apk` ইনস্টল করো। Android চাইলে browser/file manager-কে “Install unknown apps” অনুমতি দিতে হবে।

## Build locally
Android Studio + JDK 17 + Android SDK required. Run:
`gradle assembleDebug`

Application ID: `com.miraj.nexa`
Minimum Android: 7.0 (API 24)
