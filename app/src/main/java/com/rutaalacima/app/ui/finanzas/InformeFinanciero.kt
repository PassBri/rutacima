package com.rutaalacima.app.ui.finanzas

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.rutaalacima.app.R
import com.rutaalacima.app.domain.model.Cuentas
import com.rutaalacima.app.domain.model.Deudas
import com.rutaalacima.app.domain.model.Salud
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Análisis financiero completo (igual que informeFinanciero en la web): resumen, Índice de Salud Financiera con
 * cada indicador y su meta, qué hacer ahora, estado de cuentas, en qué se va el dinero, activos y pasivos, cada
 * deuda (cuotas, capital e intereses) y el plan para salir de deudas. Se arma como página y se abre el diálogo
 * de impresión del sistema, donde se elige «Guardar como PDF».
 */
object InformeFinanciero {
    private fun esc(t: String) = t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    private fun pct(v: Double) = NumberFormat.getNumberInstance(Locale.getDefault()).apply { maximumFractionDigits = 1 }.format(v * 100) + " %"
    private fun num(v: Double) = NumberFormat.getNumberInstance(Locale.getDefault()).apply { maximumFractionDigits = 2 }.format(v)
    private fun s(ctx: Context, n: String) = ctx.resources.getIdentifier(n, "string", ctx.packageName).let { if (it != 0) ctx.getString(it) else n }
    private fun mesesDespues(n: Int): String { val ym = YearMonth.now().plusMonths(n.toLong()); return com.rutaalacima.app.ui.ruta.mesEnFrase(ym.monthValue) + " " + ym.year }

    private fun valor(ctx: Context, i: Salud.Indicador) = when (i.clave) {
        "liquidez", "libertad", "netoing" -> num(i.valor)
        "fondo" -> ctx.getString(R.string.cu_meses_x, num(i.valor))
        "endeuda" -> if (i.valor.isInfinite()) ctx.getString(R.string.cu_sin_activos) else pct(i.valor)
        else -> pct(i.valor)
    }
    private fun meta(ctx: Context, k: String) = when (k) {
        "flujo", "construye", "pasivos" -> "20 %"; "ahorro" -> "10 %"; "dti" -> "30 %"; "gasto" -> "70 %"; "endeuda" -> "30 %"; "buenas" -> "50 %"
        "liquidez" -> num(1.2); "libertad" -> num(1.0); "fondo" -> ctx.getString(R.string.cu_meses_x, "3–6"); "netoing" -> ctx.getString(R.string.cu_meta_anio); else -> ""
    }

    fun html(ctx: Context, libro: Libro): String {
        val plata: (Double) -> String = { Cuentas.formato(it, libro.moneda) }
        val r = libro.salud(null)
        val d = libro.deudasDatos; val lista = libro.deudas; val plan = Deudas.plan(lista, d.metodo, d.extra)
        val hoy = LocalDate.now()
        val meses = libro.dias.keys.filter { it <= hoy.toString() }.map { it.take(7) }.distinct().sorted().takeLast(12)
        val nomMes = { ym: String -> com.rutaalacima.app.ui.ruta.mesEnFrase(ym.takeLast(2).toInt()) + " " + ym.take(4) }
        val periodo = if (meses.isEmpty()) "—" else "${nomMes(meses.first())} – ${nomMes(meses.last())} (${ctx.getString(R.string.cu_meses_x, "${r.meses}")})"
        val est = mapOf(Salud.Estado.OK to ctx.getString(R.string.cu_est_ok), Salud.Estado.MEDIO to ctx.getString(R.string.cu_est_medio), Salud.Estado.BAJO to ctx.getString(R.string.cu_est_bajo), Salud.Estado.NA to "—")
        val cls = mapOf(Salud.Estado.OK to "e-ok", Salud.Estado.MEDIO to "e-medio", Salud.Estado.BAJO to "e-bajo", Salud.Estado.NA to "")
        fun fila(a: String, b: String, c: String? = "", total: Boolean = false) = "<tr${if (total) " class=\"total\"" else ""}><th>$a</th><td>$b</td>${if (c != null) "<td>$c</td>" else ""}</tr>"
        val nivel = r.nivel?.let {
            when (it) { Salud.Nivel.EXCELENTE -> R.string.cu_nivel_excelente to R.string.cu_nivel_excelente_d; Salud.Nivel.BUENA -> R.string.cu_nivel_buena to R.string.cu_nivel_buena_d
                Salud.Nivel.MODERADA -> R.string.cu_nivel_moderada to R.string.cu_nivel_moderada_d; Salud.Nivel.RIESGO -> R.string.cu_nivel_riesgo to R.string.cu_nivel_riesgo_d }
        }
        val malos = r.indicadores.filter { it.estado == Salud.Estado.BAJO || it.estado == Salud.Estado.MEDIO }
        val gastos = r.suma.categorias.filterKeys { it.first == Cuentas.Tipo.GAS }.entries.sortedByDescending { it.value }
        val totGas = gastos.sumOf { it.value }.takeIf { it > 0 } ?: 1.0
        fun nomCat(t: Cuentas.Tipo, c: String) = if (c.startsWith("u_")) libro.propias[t.codigo]?.firstOrNull { it.c == c }?.n ?: c else s(ctx, "cu_cat_${t.codigo}_$c").let { if (it.startsWith("cu_cat_")) c else it }
        val led = libro.total.construido
        val css = """*{box-sizing:border-box}body{font:13px/1.5 Roboto,sans-serif;color:#1D2427;margin:0}
            header{display:flex;justify-content:space-between;align-items:flex-end;border-bottom:3px solid #6B2A1A;padding-bottom:12px}
            h1{font:600 24px Georgia,serif;margin:0;color:#6B2A1A}h2{font:600 17px Georgia,serif;color:#6B2A1A;margin:24px 0 6px}
            .suave{color:#5A666B}.marca{font-size:11px;letter-spacing:.08em;text-transform:uppercase;color:#86683A}
            .kpis{display:flex;gap:8px;margin:14px 0}.kpi{flex:1;border:1px solid #DCE2E4;border-radius:8px;padding:8px 10px}.kpi span{display:block;font-size:11px;color:#5A666B}.kpi b{font-size:16px}
            table{width:100%;border-collapse:collapse;margin:4px 0}th,td{padding:5px 6px;border-bottom:1px solid #EEF1F2;text-align:left;vertical-align:top}td{text-align:right}
            thead th{font-size:11px;color:#5A666B;font-weight:500}tr.total th,tr.total td{font-weight:700}.neg{color:#B3261E}.e-ok{color:#0C7A0C}.e-medio{color:#9A6A00}.e-bajo{color:#B3261E}
            .isf{display:flex;gap:16px;align-items:center;padding:12px 14px;border-radius:10px;background:#F3E3DC}.isf b{font:600 34px Georgia,serif;color:#6B2A1A}
            .deuda{border:1px solid #DCE2E4;border-radius:8px;padding:8px 12px;margin:8px 0;page-break-inside:avoid}.pie{margin-top:24px;font-size:11px;color:#5A666B;border-top:1px solid #DCE2E4;padding-top:8px}
            @page{margin:14mm 12mm}"""
        val sb = StringBuilder()
        sb.append("<!doctype html><html><head><meta charset=\"utf-8\"><style>$css</style></head><body>")
        sb.append("<header><div><div class=\"marca\">SuRoute</div><h1>${esc(ctx.getString(R.string.cu_informe_titulo))}</h1><div class=\"suave\">${esc(ctx.getString(R.string.cu_informe_periodo, periodo))}</div></div>")
        sb.append("<div class=\"suave\">${hoy.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))}</div></header>")
        sb.append("<div class=\"kpis\"><div class=\"kpi\"><span>${esc(ctx.getString(R.string.cu_c_ing_tot))}</span><b>${plata(r.suma.ing)}</b></div><div class=\"kpi\"><span>${esc(ctx.getString(R.string.cu_c_egresos))}</span><b>${plata(r.egresos)}</b></div>")
        sb.append("<div class=\"kpi\"><span>${esc(ctx.getString(R.string.cu_c_flujo))}</span><b class=\"${if (r.flujoNeto < 0) "neg" else ""}\">${plata(r.flujoNeto)}</b></div><div class=\"kpi\"><span>${esc(ctx.getString(R.string.cu_c_neto))}</span><b class=\"${if (r.neto < 0) "neg" else ""}\">${plata(r.neto)}</b></div></div>")
        sb.append("<h2>${esc(ctx.getString(R.string.cu_salud))}</h2>")
        if (r.isf != null && nivel != null) sb.append("<div class=\"isf\"><b>${r.isf} %</b><div><strong>${esc(ctx.getString(nivel.first))}</strong><br><span class=\"suave\">${esc(ctx.getString(nivel.second))}</span></div></div>")
        sb.append("<table><thead><tr><th></th><th style=\"text-align:right\">${esc(ctx.getString(R.string.cu_informe_valor))}</th><th style=\"text-align:right\">${esc(ctx.getString(R.string.cu_informe_meta))}</th><th style=\"text-align:right\">${esc(ctx.getString(R.string.cu_informe_estado))}</th></tr></thead><tbody>")
        r.indicadores.forEach { i -> sb.append("<tr><th>${esc(s(ctx, "cu_ind_${i.clave}"))}<br><small class=\"suave\">${esc(s(ctx, "cu_ind_${i.clave}_d"))}</small></th><td>${esc(valor(ctx, i))}</td><td>${esc(meta(ctx, i.clave))}</td><td class=\"${cls[i.estado]}\">${esc(est[i.estado] ?: "")}</td></tr>") }
        sb.append("</tbody></table><h2>${esc(ctx.getString(R.string.cu_informe_que_hacer))}</h2>")
        if (malos.isEmpty()) sb.append("<p>${esc(ctx.getString(R.string.cu_informe_todo_bien))}</p>")
        else sb.append("<ul>" + malos.joinToString("") { "<li><b>${esc(s(ctx, "cu_ind_${it.clave}"))}</b> (${esc(valor(ctx, it))}): ${esc(s(ctx, "cu_rec_${it.clave}"))}</li>" } + "</ul>")
        val m = r.meses.coerceAtLeast(1)
        sb.append("<h2>${esc(ctx.getString(R.string.cu_informe_estado_cuentas))}</h2><table><thead><tr><th>${esc(ctx.getString(R.string.cu_c_concepto))}</th><th style=\"text-align:right\">${esc(ctx.getString(R.string.cu_c_promedio_n, r.meses))}</th><th style=\"text-align:right\">${esc(ctx.getString(R.string.cu_c_total_n, r.meses))}</th></tr></thead><tbody>")
        listOf(Triple(R.string.cu_c_ing_act, r.ingActivos, false), Triple(R.string.cu_c_ing_pas, r.ingPasivos, false), Triple(R.string.cu_c_ing_tot, r.suma.ing, true),
            Triple(R.string.cu_c_fijos, r.fijos, false), Triple(R.string.cu_c_variables, r.variables, false), Triple(R.string.cu_aho, r.suma.aho, false), Triple(R.string.cu_inv, r.suma.inv, false),
            Triple(R.string.cu_c_egresos, r.egresos, true), Triple(R.string.cu_c_flujo, r.flujoNeto, true)).forEach { (t, v, tot) -> sb.append(fila(esc(ctx.getString(t)), plata(Math.round(v / m).toDouble()), plata(v), tot)) }
        sb.append(fila(esc(ctx.getString(R.string.cu_c_cuotas)), plata(Math.round(r.deudaMes).toDouble()), "")).append("</tbody></table>")
        if (gastos.isNotEmpty()) {
            sb.append("<h2>${esc(ctx.getString(R.string.cu_informe_en_que))}</h2><table><tbody>")
            gastos.take(12).forEach { (k, v) -> sb.append(fila(esc(nomCat(k.first, k.second)), plata(v), "${Math.round(v / totGas * 100)} %")) }
            sb.append("</tbody></table>")
        }
        sb.append("<h2>${esc(ctx.getString(R.string.cu_lo_que_tienes))}</h2><table><tbody>")
        libro.activos.forEach { a -> sb.append(fila(esc(a.nombre), esc(s(ctx, "cu_act_${a.tipo}")), plata(a.valor))) }
        sb.append(fila(esc(ctx.getString(R.string.cu_activo_libro)), esc(ctx.getString(R.string.cu_se_suma_solo)), plata(led)))
        sb.append(fila(esc(ctx.getString(R.string.cu_c_activos)), "", plata(r.activos), true)).append("</tbody></table>")
        sb.append("<h2>${esc(ctx.getString(R.string.cu_lo_que_debes))}</h2><table><tbody>")
        lista.forEach { h -> sb.append(fila(esc(h.deuda.nombre), esc(ctx.getString(if (h.deuda.tipo == "buena") R.string.cu_buena_d else R.string.cu_mala_d)), plata(h.actual))) }
        sb.append(fila(esc(ctx.getString(R.string.cu_c_pasivos)), "", plata(r.pasivos), true)).append(fila(esc(ctx.getString(R.string.cu_c_neto)), "", "<span class=\"${if (r.neto < 0) "neg" else ""}\">${plata(r.neto)}</span>", true)).append("</tbody></table>")
        if (lista.isNotEmpty()) {
            sb.append("<h2>${esc(ctx.getString(R.string.cu_tus_deudas))}</h2>")
            if (plan.meses != null && lista.any { it.actual > 0 }) sb.append("<p>${esc(ctx.getString(R.string.cu_informe_plan, mesesDespues(plan.meses), plata(Math.round(plan.interes).toDouble())))}</p>")
            lista.forEach { h ->
                val x = h.deuda; val av = h.avance; val fin = plan.fin[x.id]
                sb.append("<div class=\"deuda\"><strong>${esc(x.nombre)}</strong> <span class=\"suave\">· ${esc(ctx.getString(R.string.cu_deuda_det, num(x.tasa), plata(x.minimo)))}</span><table><tbody>")
                sb.append(fila(esc(ctx.getString(R.string.cu_debes_hoy)), plata(h.actual), null))
                if (av != null) {
                    sb.append(fila(esc(ctx.getString(R.string.cu_cuota_k_n, av.k, av.n)), esc(if (av.faltan > 0) ctx.getString(R.string.cu_faltan_cuotas, av.faltan, mesesDespues(av.faltan)) else ctx.getString(R.string.cu_todas_pagadas)), null))
                    if (x.minimo > 0) sb.append("<tr><td colspan=\"2\" style=\"text-align:left\">${esc(ctx.getString(R.string.cu_has_pagado, plata(Math.round(av.pagadoTotal).toDouble()), plata(Math.round(av.capital).toDouble()), plata(Math.round(av.intereses).toDouble()), plata(h.actual)))}</td></tr>")
                }
                if (h.actual > 0) sb.append(fila(esc(ctx.getString(R.string.cu_la_eliminas)), fin?.let { mesesDespues(it) } ?: "—", null))
                sb.append("</tbody></table></div>")
            }
        }
        sb.append("<p class=\"pie\">${esc(ctx.getString(R.string.cu_informe_pie))}</p></body></html>")
        return sb.toString()
    }

    private var vista: WebView? = null   // se guarda mientras imprime
    /** Abre el diálogo de impresión del sistema con el análisis (ahí se elige «Guardar como PDF»). */
    fun imprimir(ctx: Context, libro: Libro) {
        val w = WebView(ctx); vista = w
        w.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val pm = ctx.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val nombre = "SuRoute-analisis-${LocalDate.now()}"
                pm.print(nombre, view.createPrintDocumentAdapter(nombre), PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build())
            }
        }
        w.loadDataWithBaseURL(null, html(ctx, libro), "text/html", "utf-8", null)
    }
}
