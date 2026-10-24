# Nexa — free AI backend setup

Nexa's Android app currently includes voice input, speech output, reminders, and phone shortcuts. Online AI chat is **not connected yet**. This folder provides a free-tier Cloudflare Worker proxy so the Gemini key is kept off the phone.

## One-time setup (requires your own accounts)
1. Create a free Cloudflare account and open **Workers & Pages**.
2. Create a Worker named `nexa-ai`.
3. Paste the contents of `worker.js` into the Worker editor and deploy.
4. In Worker **Settings → Variables and Secrets**, add a secret named `GEMINI_API_KEY`. Create a Gemini API key in Google AI Studio and paste it there. Never commit the key to GitHub or put it in the APK.
5. The Worker URL will look like `https://nexa-ai.<your-subdomain>.workers.dev`. Test it with a POST request to `/chat` using JSON `{"message":"বাংলায় উত্তর দাও: তুমি কে?"}`.
6. Free quotas and availability are controlled by Google/Cloudflare and can change. A free tier is not a guarantee of unlimited requests.

## Important limitations
- This worker is a starter backend and is not deployed automatically by GitHub.
- The Android app still needs its chat screen connected to your deployed `/chat` URL before online AI works inside Nexa.
- For public use, add rate limiting/authentication and consider restricting CORS to your app's intended clients. Android native apps do not enforce browser CORS, so CORS alone is not security.
- Do not share API keys in chat, screenshots, or source code.
