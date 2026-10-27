package com.rutaalacima.app.ui.vision

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import com.rutaalacima.app.R
import com.rutaalacima.app.domain.model.Mandala
import androidx.compose.ui.graphics.toArgb
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Informe de la Brújula 1·9·81 (igual que informeBrujula en la web): la cumbre, el avance, el ritmo del
 * Método Cima 9×52, la imagen del 9×9 y cada campamento con sus 8 pasos y las metas que lo sostienen.
 * Se arma como página y se abre el diálogo de impresión, donde se elige «Guardar como PDF».
 */
object InformeBrujula {
    private fun esc(t: String) = t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    private var vista: WebView? = null

    /** [nombres]: nombre de cada campamento; [porcentajes]: avance por campamento; [metas]: títulos de metas del año por código. */
    fun imprimir(
        ctx: Context, datos: DatosMandala, anio: Int?, nombre: String, porcentajeCumbre: Int,
        nombres: List<String>, ejes: List<String>, fases: List<String>, porcentajes: List<Int>, metas: Map<String, List<String>>,
        textos: MandalaImagen.Textos,
    ) {
        val html = html(ctx, datos, anio, nombre, porcentajeCumbre, nombres, ejes, fases, porcentajes, metas, "")
        val w = WebView(ctx); vista = w
        w.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val pm = ctx.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val n = "SuRoute-brujula-${LocalDate.now()}"
                pm.print(n, view.createPrintDocumentAdapter(n), PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build())
            }
        }
        w.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null)
    }

    /** El 9×9 en texto: sin colores ni imágenes, con líneas más gruesas entre los bloques de 3×3. */
    private fun grilla(d: DatosMandala, nombres: List<String>): String = "<table class=\"g9\">" + (0 until 9).joinToString("") { f ->
        "<tr>" + (0 until 9).joinToString("") { c ->
            val cel = Mandala.celda(f, c)
            val cls = listOfNotNull("bi".takeIf { c % 3 == 0 }, "bt".takeIf { f % 3 == 0 }, "bd".takeIf { c == 8 }, "bb".takeIf { f == 8 }).joinToString(" ")
            val t = when (cel.tipo) {
                Mandala.Tipo.CUMBRE -> "<b>${esc(d.tituloCumbre)}</b>"
                Mandala.Tipo.CAMPAMENTO -> "<b>${esc(nombres[cel.campamento])}</b>"
                else -> d.paso(cel.campamento, cel.paso).trim().let { if (it.isEmpty()) "" else esc(it) + if (d.estado(cel.campamento, cel.paso) == Mandala.EstadoPaso.HECHO) " ✓" else "" }
            }
            "<td class=\"$cls\">$t</td>"
        } + "</tr>"
    } + "</table>"

    private fun html(
        ctx: Context, d: DatosMandala, anio: Int?, nombre: String, pctC: Int, nombres: List<String>, ejes: List<String>,
        fases: List<String>, pcts: List<Int>, metas: Map<String, List<String>>, img: String,
    ): String {
        fun s(id: Int, vararg a: Any) = esc(ctx.getString(id, *a))
        val nf = NumberFormat.getNumberInstance()
        val fecha = LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
        val r = d.ritmo; val pr = d.progreso
        var sec = 0
        fun h2(t: String) = "<h2><span>${"%02d".format(++sec)}</span>$t</h2>"
        val est = mapOf(
            Mandala.EstadoPaso.VACIO to Triple(s(R.string.ib_por_escribir), "#5A666B", "#EEF1F2"),
            Mandala.EstadoPaso.ESCRITO to Triple(s(R.string.ib_en_camino), "#8A5A00", "#FBEFD8"),
            Mandala.EstadoPaso.HECHO to Triple(s(R.string.ib_ganado), "#1E7B4A", "#E3F2E9"),
        )
        val anioMetas = anio ?: LocalDate.now().year
        val camps = (0 until Mandala.CAMPAMENTOS).joinToString("") { i ->
            val cv = d.casilla(i)
            val col = String.format("#%06X", 0xFFFFFF and d.color(i).toArgb())
            val cab = "<header><span class=\"rumbo\" style=\"background:$col\">${Mandala.RUMBOS[i]}</span><div><b>${esc(nombres[i])}</b><small>${esc(ejes[i])}</small></div><span class=\"pct\">${pcts[i]} %</span></header>"
            if (cv == null) return@joinToString "<section class=\"camp\">$cab<p class=\"vacio\">${s(R.string.ib_libre)}</p></section>"
            val filas = (0 until Mandala.PASOS).joinToString("") { p ->
                val t = d.paso(i, p).trim(); val e = d.estado(i, p); val x = d.pasoCima(i, p); val req = x.requeridas ?: r.dificultad
                val (et, fg, bg) = est.getValue(e)
                val jor = when (e) {
                    Mandala.EstadoPaso.HECHO -> x.cumplidoEn?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: "✓"
                    Mandala.EstadoPaso.ESCRITO -> s(R.string.ib_n_de_m, x.jornadas.size, req)
                    else -> "—"
                }
                "<tr><td class=\"num\">${p + 1}</td><td>${if (t.isNotEmpty()) esc(t) else "<span class=\"suave\">—</span>"}</td>" +
                    "<td class=\"n\"><span class=\"pill\" style=\"color:$fg;background:$bg\">$et</span></td><td class=\"n\">$jor</td></tr>"
            }
            val ms = metas[d.codigo(i)].orEmpty()
            "<section class=\"camp\">$cab<table><thead><tr><th>#</th><th>${s(R.string.ib_paso)}</th><th class=\"n\">${s(R.string.ib_estado)}</th><th class=\"n\">${s(R.string.ib_jornadas)}</th></tr></thead><tbody>$filas</tbody></table>" +
                (if (ms.isNotEmpty()) "<p class=\"metas\"><b>${s(R.string.ib_metas, anioMetas)}</b> ${ms.joinToString(" · ") { esc(it) }}</p>" else "") + "</section>"
        }
        val css = """*{box-sizing:border-box}body{font:13.5px/1.55 sans-serif;color:#1D2427;margin:0;background:#fff;-webkit-print-color-adjust:exact}
            .banda{background:#6B2A1A;color:#F3EDE0;padding:18px 28px;display:flex;justify-content:space-between;align-items:center}.banda b{font:600 20px serif;letter-spacing:.08em}
            .banda .meta{text-align:right;font-size:11px;letter-spacing:.06em;text-transform:uppercase;opacity:.85}
            .portada{padding:22px 28px 14px;border-bottom:1px solid #DCE2E4}h1{font:600 26px/1.15 serif;margin:0}.sub{color:#5A666B;margin:6px 0 0}
            .cumbre{margin:14px 0 0;padding:14px 18px;border-radius:12px;background:#FBF8F2;border:1px solid #E8DCC4;font:600 17px/1.4 serif;color:#6B2A1A}
            .cumbre small{display:block;font:500 10.5px sans-serif;letter-spacing:.1em;text-transform:uppercase;color:#86683A;margin-bottom:4px}
            .cuerpo{padding:4px 28px 28px}h2{display:flex;align-items:baseline;gap:10px;font:600 18px serif;color:#6B2A1A;margin:24px 0 10px}h2 span{font:500 11px sans-serif;color:#86683A;letter-spacing:.1em}
            .kpis{display:grid;grid-template-columns:repeat(4,1fr);gap:8px}.kpi{border-radius:10px;background:#F5F7F8;padding:9px 11px}.kpi span{display:block;font-size:11px;color:#5A666B}.kpi b{font-size:16px}
            .barra{height:8px;border-radius:4px;background:#EEF1F2;overflow:hidden;margin:10px 0 4px}.barra i{display:block;height:100%;background:#C9973B}
            .ritmo{margin:10px 0 0;padding:10px 14px;border-left:3px solid #86683A;background:#FBF8F2}.ritmo p{margin:3px 0}
            table.g9{table-layout:fixed;border-collapse:collapse;width:100%;page-break-inside:avoid}table.g9 td{border:1px solid #9AA3A7;padding:3px;height:58px;text-align:center;vertical-align:middle;font-size:8.5px;line-height:1.25;word-break:break-word}
            table.g9 td.bi{border-left:2.5px solid #1D2427}table.g9 td.bt{border-top:2.5px solid #1D2427}table.g9 td.bd{border-right:2.5px solid #1D2427}table.g9 td.bb{border-bottom:2.5px solid #1D2427}
            .camp{border:1px solid #DCE2E4;border-radius:12px;padding:12px 14px;margin:10px 0;page-break-inside:avoid}
            .camp header{display:flex;gap:10px;align-items:center;margin-bottom:6px}.camp header div{flex:1}.camp header b{display:block;font:600 15px serif}.camp header small{color:#5A666B}
            .rumbo{min-width:32px;height:32px;padding:0 6px;border-radius:9px;color:#fff;display:inline-flex;align-items:center;justify-content:center;font-weight:700;font-size:12px}.pct{font:600 17px serif}
            table{width:100%;border-collapse:collapse}th,td{padding:5px 7px;border-bottom:1px solid #EEF1F2;text-align:left;vertical-align:top}thead th{font:500 10.5px sans-serif;color:#5A666B;text-transform:uppercase}
            td small{display:block;font-size:10px;color:#86683A;font-weight:700;text-transform:uppercase}.num{width:22px;color:#5A666B}.n{text-align:right;white-space:nowrap}
            .pill{display:inline-block;padding:2px 9px;border-radius:999px;font-size:11px;font-weight:700}.suave,.vacio{color:#5A666B}.metas{margin:6px 0 0;font-size:12px}
            .nota{margin-top:22px;padding-top:10px;border-top:1px solid #DCE2E4;font-size:11px;color:#5A666B}"""
        return """<!doctype html><html><head><meta charset="utf-8"><style>$css</style></head><body>
            <div class="banda"><b>SUROUTE</b><div class="meta">Brújula 1·9·81<br>${if (anio != null) s(R.string.ib_anio, anio) else s(R.string.ib_general)}</div></div>
            <div class="portada"><h1>${s(R.string.ib_titulo)}</h1><p class="sub">${if (nombre.isNotBlank()) esc(nombre) + " · " else ""}$fecha</p>
              <div class="cumbre"><small>${s(R.string.ib_mi_cumbre)}</small>${esc(d.tituloCumbre)}</div></div>
            <div class="cuerpo">${h2(s(R.string.ib_como_voy))}
              <div class="kpis"><div class="kpi"><span>${s(R.string.ib_avance)}</span><b>$pctC %</b></div><div class="kpi"><span>${s(R.string.ib_escritos)}</span><b>${s(R.string.ib_de_64, pr.escritos)}</b></div>
                <div class="kpi"><span>${s(R.string.ib_ganados)}</span><b>${s(R.string.ib_de_64, pr.hechos)}</b></div><div class="kpi"><span>${s(R.string.ib_evidencias)}</span><b>${d.evidencias}</b></div></div>
              <div class="barra"><i style="width:$pctC%"></i></div>
              <div class="ritmo"><p><b>${s(R.string.metodo_altura, nf.format(r.mm))}</b></p><p>${s(R.string.ib_ritmo, r.tramo + 1, r.dificultad)}</p></div>
              ${h2(s(R.string.ib_imagen))}${grilla(d, nombres)}
              ${h2(s(R.string.ib_campamentos))}$camps
              <div class="nota">${s(R.string.ib_nota)}</div></div></body></html>"""
    }
}
