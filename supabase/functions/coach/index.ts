// RutaCima · Coach de IA (Supabase Edge Function)
//
// La clave de la IA vive aquí, en el servidor (nunca en el teléfono).
// Secretos necesarios (Supabase › Edge Functions › Secrets):
//   ANTHROPIC_API_KEY   clave de la API de Claude (console.anthropic.com)
//   COACH_MODEL         opcional, por defecto "claude-sonnet-5-5"
//   COACH_DAILY_LIMIT   opcional, mensajes por usuario y día (por defecto 30)
// Despliegue:  supabase functions deploy coach
//
// Entrada (POST JSON): { mensajes: [{role, content}], contexto: string, idioma: "es" }
// Salida: { texto: string }

import { createClient } from "jsr:@supabase/supabase-js@2";

const MODELO = Deno.env.get("COACH_MODEL") ?? "claude-sonnet-5-5";
const LIMITE = Number(Deno.env.get("COACH_DAILY_LIMIT") ?? "30");

const IDIOMAS: Record<string, string> = {
  es: "español", en: "English", pt: "português", fr: "français", de: "Deutsch", it: "italiano",
  zh: "中文", ja: "日本語", ko: "한국어", ar: "العربية", hi: "हिन्दी", ru: "русский",
};

function sistema(contexto: string, idioma: string): string {
  return `Eres el coach de RutaCima, una app para lograr metas personales con el método "Ruta a la Cima".

EL MÉTODO
- La Cumbre Personal es el punto donde lo que la persona es, hace y aporta se alinean.
- 7 fases del viaje transformativo: 1 Orientación (claridad), 2 Preparación (habilidades y hábitos),
  3 Travesía (aplicar y enfrentar obstáculos), 4 Ascenso (ver resultados y ajustar), 5 Culminación
  (alcanzar una cumbre concreta), 6 Contemplación (asimilar e integrar), 7 Descenso (prepararse para la próxima montaña).
- 6 ejes: Voluntad (lo que me mueve), Maestría (en qué soy excelente), Voz (mi mensaje), Valor (cómo sostengo
  mi vida con lo que aporto), Evolución (cómo crezco), Trascendencia (qué legado dejo). Se evalúan de 1 a 10.
- Confluencia: los ejes se multiplican; un proyecto que active varios ejes vale más que tareas sueltas.
- Cascada de metas: propósitos a 5 años → metas anuales (campamentos base) → metas mensuales → acciones diarias.
- Metas ORSE: Observables, Relevantes, Específicas y con Evidencia. Prioridad ABCD. Matriz: continuar,
  acelerar, pausar o eliminar. Semáforo: 0, 25, 50, 75, 100 % o en pausa.
- Protocolos de rescate: Día cero energía (regla 1-3-5), Semana mala (una sola prioridad y 3 hábitos mínimos),
  Recuperación 48 h, regla de los 5 minutos y regla de las 48 horas antes de rendirse.

CÓMO RESPONDES
- Responde SIEMPRE en ${IDIOMAS[idioma] ?? "español"}.
- Breve (máximo ~180 palabras), cálido, directo y práctico. Termina con un paso concreto para hoy o una pregunta poderosa.
- Usa el contexto real del usuario; si falta información clave, pregunta una sola cosa.
- Cuando propongas metas, escríbelas en formato ORSE e indica el eje que activan y en qué nivel de la cascada van.
- No eres terapeuta ni médico. Si la persona menciona crisis, autolesión o riesgo, responde con calidez, invítala a
  buscar ayuda profesional o una línea de crisis local de inmediato y no continúes con coaching de metas.

CONTEXTO DEL USUARIO
${contexto || "(sin datos todavía)"}`;
}

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
};

function json(cuerpo: unknown, estado = 200) {
  return new Response(JSON.stringify(cuerpo), { status: estado, headers: { ...cors, "Content-Type": "application/json" } });
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  try {
    const clave = Deno.env.get("ANTHROPIC_API_KEY");
    if (!clave) return json({ error: "Falta ANTHROPIC_API_KEY en los secretos de la función." }, 500);

    // Usuario autenticado (el token de la app).
    const admin = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
    const token = (req.headers.get("Authorization") ?? "").replace("Bearer ", "");
    const { data: { user } } = await admin.auth.getUser(token);
    if (!user) return json({ error: "Inicia sesión para usar el coach." }, 401);

    // RutaCima Web entra como usuario anónimo: el coach responde a nombre de la cuenta que lo vinculó.
    let duenoId = user.id;
    if (user.is_anonymous) {
      const { data: d } = await admin.from("dispositivos").select("user_id").eq("web_uid", user.id).maybeSingle();
      if (!d) return json({ error: "Vincula este computador desde la app para usar el coach." }, 401);
      duenoId = d.user_id;
    }

    // Límite diario por cuenta (teléfono y web suman juntos).
    const hoy = new Date().toISOString().slice(0, 10);
    const { data: uso } = await admin.from("ai_usage").select("usos").eq("user_id", duenoId).eq("dia", hoy).maybeSingle();
    const usos = uso?.usos ?? 0;
    if (usos >= LIMITE) return json({ texto: "Llegaste al límite de mensajes de hoy. Mañana seguimos: mientras tanto, da un paso pequeño hacia tu cumbre." });
    await admin.from("ai_usage").upsert({ user_id: duenoId, dia: hoy, usos: usos + 1 });

    const { mensajes = [], contexto = "", idioma = "es" } = await req.json();
    const limpios = (mensajes as { role: string; content: string }[])
      .filter((m) => (m.role === "user" || m.role === "assistant") && typeof m.content === "string" && m.content.trim())
      .slice(-16)
      .map((m) => ({ role: m.role, content: m.content.slice(0, 4000) }));
    // La API exige que la conversación empiece con el usuario.
    while (limpios.length && limpios[0].role !== "user") limpios.shift();
    if (!limpios.length) return json({ error: "Mensaje vacío." }, 400);

    const r = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: { "x-api-key": clave, "anthropic-version": "2023-06-01", "content-type": "application/json" },
      body: JSON.stringify({ model: MODELO, max_tokens: 700, system: sistema(String(contexto).slice(0, 4000), idioma), messages: limpios }),
    });
    const datos = await r.json();
    if (!r.ok) return json({ error: datos?.error?.message ?? "Error de la IA" }, 502);
    const texto = (datos.content ?? []).filter((b: { type: string }) => b.type === "text").map((b: { text: string }) => b.text).join("\n").trim();
    return json({ texto });
  } catch (e) {
    return json({ error: String(e) }, 500);
  }
});
