# Wear Store — Master Code

פלטפורמת פריסה פרטית לאפליקציות Wear OS.

## מבנה הפרויקט

```
wear-store/
├── .github/workflows/build.yml   ← GitHub Actions
├── server/                        ← Node.js על Render
│   ├── index.js
│   ├── package.json
│   ├── render.yaml
│   └── public/index.html         ← דף אדמין
└── watch-app/                     ← אפליקציית חנות לשעון
    ├── app/src/main/
    │   ├── java/com/mastercode/wearstore/MainActivity.kt
    │   └── AndroidManifest.xml
    └── build.gradle.kts
```

---

## הגדרה ראשונית

### 1. GitHub Actions — בניית APK

כל `push` ל-`main` בונה אוטומטית APK.

להוריד: **Actions → Build Wear Store APK → wear-store-debug**

### 2. שרת — Render

1. העלה את תיקיית `server/` ל-GitHub repo נפרד (או אותו repo)
2. ב-Render: **New Web Service** → בחר את ה-repo
3. Render מזהה את `render.yaml` אוטומטית
4. אחרי deploy — העתק את ה-URL

### 3. חיבור השעון לשרת

ב-`watch-app/app/src/main/java/com/mastercode/wearstore/MainActivity.kt`:

```kotlin
const val SERVER_URL = "https://YOUR-APP.onrender.com"  // ← שנה לכאן
```

commit + push → GitHub Actions בונה APK חדש.

### 4. התקנה על השעון

```bash
cd C:\adb\platform-tools

# Pair (פעם ראשונה)
adb pair 10.0.0.16:[PORT] [CODE]

# Connect
adb connect 10.0.0.16:35511

# התקן
adb -s 10.0.0.16:35511 install app-debug.apk
```

---

## שימוש שוטף

### להעלות APK חדש:
1. פתח `https://your-app.onrender.com` בדפדפן
2. גרור/בחר APK
3. לחץ **העלה לשרת**

### להתקין מהשעון:
1. פתח את **Wear Store** בשעון
2. בחר אפליקציה מהרשימה
3. לחץ **התקן**

---

## API

| Method | Path | תיאור |
|--------|------|--------|
| GET | `/apps` | רשימת כל ה-APK |
| POST | `/upload` | העלאת APK |
| GET | `/download/:filename` | הורדת APK |
| DELETE | `/apps/:filename` | מחיקת APK |
