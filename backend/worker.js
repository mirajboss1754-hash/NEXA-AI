// Nexa AI proxy for Cloudflare Workers (free tier).
// Configure GEMINI_API_KEY as a Worker secret; never put it in the Android app.
export default {
  async fetch(request, env) {
    const cors = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type",
      "Content-Type": "application/json; charset=utf-8"
    };
    if (request.method === "OPTIONS") return new Response(null, { headers: cors });
    if (new URL(request.url).pathname !== "/chat") return new Response(JSON.stringify({ error: "Use POST /chat" }), { status: 404, headers: cors });
    if (request.method !== "POST") return new Response(JSON.stringify({ error: "POST required" }), { status: 405, headers: cors });
    if (!env.GEMINI_API_KEY) return new Response(JSON.stringify({ error: "Server is missing GEMINI_API_KEY secret" }), { status: 503, headers: cors });
    let body;
    try { body = await request.json(); } catch { return new Response(JSON.stringify({ error: "Invalid JSON" }), { status: 400, headers: cors }); }
    const message = typeof body.message === "string" ? body.message.trim() : "";
    if (!message) return new Response(JSON.stringify({ error: "message is required" }), { status: 400, headers: cors });
    if (message.length > 6000) return new Response(JSON.stringify({ error: "Message too long (limit 6000 characters)" }), { status: 413, headers: cors });
    try {
      const upstream = await fetch("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent", {
        method: "POST",
        headers: { "Content-Type": "application/json", "x-goog-api-key": env.GEMINI_API_KEY },
        body: JSON.stringify({
          systemInstruction: { parts: [{ text: "You are Nexa, a helpful personal assistant. Reply naturally in the language the user uses, especially Bangla. Be concise, safe, and honest. You cannot directly control phone functions unless the app provides that action." }] },
          contents: [{ role: "user", parts: [{ text: message }] }],
          generationConfig: { temperature: 0.7, maxOutputTokens: 800 }
        })
      });
      const data = await upstream.json();
      if (!upstream.ok) return new Response(JSON.stringify({ error: "AI provider request failed", status: upstream.status }), { status: 502, headers: cors });
      const answer = data?.candidates?.[0]?.content?.parts?.map(p => p.text || "").join("").trim();
      if (!answer) return new Response(JSON.stringify({ error: "AI returned no text" }), { status: 502, headers: cors });
      return new Response(JSON.stringify({ reply: answer }), { headers: cors });
    } catch {
      return new Response(JSON.stringify({ error: "Could not reach AI provider" }), { status: 502, headers: cors });
    }
  }
};
