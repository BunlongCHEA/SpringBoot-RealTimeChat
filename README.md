# How To Register Firebase and Service Account
## STEP 1 : Register Service Account

Go to: 
- Project Settings > Service Accounts
- Click "Generate new private key"
- Save as "firebase-service-account.json" in src/main/resources/ OR you can Encode JSON file as base64 with command below

In Linux
```bash
base64 -i firebase-service-account.json | tr -d '\n'
```

In Window Powershell
```bash
[Convert]::ToBase64String([IO.File]::ReadAllBytes("firebase-service-account.json"))
```

## STEP 2 : Get Web Push Certificate (VAPID Key)

Go to:
- Project Settings > Cloud Messaging
- Under "Web Push certificates", generate a key pair
- Use the public key as NEXT_PUBLIC_FIREBASE_VAPID_KEY

## STEP 3 : Register Web App

In Firebase project, 
- Click the gear icon ⚙️ → Project settings
- Scroll down to "Your apps" section
- Click the Web icon </>
- Enter app nickname (e.g., realtimechat-web)
- Check "Also set up Firebase Hosting" (optional)
- Click "Register app"

Also, check out instruction with this URL:

https://firebase.google.com/docs/hosting/quickstart?authuser=0&_gl=1*181ha3x*_ga*MTg5ODY4NTY1OC4xNzYwNzg3NDI2*_ga_CW55HF8NVT*czE3NjQ5NTg0OTgkbzYkZzEkdDE3NjQ5NTkxNjUkajYwJGwwJGgw

Or with this image:

![Create a new site](/images/firebase_1.png)

![Firebase SDK](/images/firebase_2.png)

![Firebase CLI Install](/images/firebase_3.png)

![Deploy to Firebase Hosting](/images/firebase_4.png)

Upload to your own .env
Example: 

```bash
    const firebaseConfig = {
        apiKey: "AIzaSyB1234567890abcdefghijklmnop",
        authDomain: "realtimechat-12345.firebaseapp.com",
        projectId: "realtimechat-12345",
        storageBucket: "realtimechat-12345. appspot.com",
        messagingSenderId: "123456789012",
        appId: "1:123456789012:web:abcdef1234567890" 
        measurementId: "G-*******" };
```

| Firebase Config Key | Environment Variable |
|---|---|
| `apiKey` | NEXT_PUBLIC_FIREBASE_API_KEY |
| `authDomain` | NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN |
| `projectId` | NEXT_PUBLIC_FIREBASE_PROJECT_ID |
| `storageBucket` | NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET |
| `messagingSenderId` | NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID |
| `appId` | NEXT_PUBLIC_FIREBASE_APP_ID |

