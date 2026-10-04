package com.rutaalacima.app.ui.i18n

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rutaalacima.app.R
import com.rutaalacima.app.data.social.TipoPost
import com.rutaalacima.app.data.social.Visibilidad
import com.rutaalacima.app.domain.model.Decision
import com.rutaalacima.app.domain.model.Eje
import com.rutaalacima.app.domain.model.EstadoMeta
import com.rutaalacima.app.domain.model.Fase
import com.rutaalacima.app.domain.model.Prioridad

/**
 * Textos traducibles de los conceptos del método. El dominio (domain/model) conserva sus textos
 * en español para la lógica y las pruebas; la interfaz usa siempre estas funciones (12 idiomas).
 */
object Textos {
    @StringRes fun nombre(e: Eje): Int = when (e) {
        Eje.VOLUNTAD -> R.string.eje_vol; Eje.MAESTRIA -> R.string.eje_mae; Eje.VOZ -> R.string.eje_voz
        Eje.VALOR -> R.string.eje_val; Eje.EVOLUCION -> R.string.eje_evo; Eje.TRASCENDENCIA -> R.string.eje_tra
    }

    @StringRes fun lema(e: Eje): Int = when (e) {
        Eje.VOLUNTAD -> R.string.eje_vol_lema; Eje.MAESTRIA -> R.string.eje_mae_lema; Eje.VOZ -> R.string.eje_voz_lema
        Eje.VALOR -> R.string.eje_val_lema; Eje.EVOLUCION -> R.string.eje_evo_lema; Eje.TRASCENDENCIA -> R.string.eje_tra_lema
    }

    @StringRes fun pregunta(e: Eje): Int = when (e) {
        Eje.VOLUNTAD -> R.string.eje_vol_pregunta; Eje.MAESTRIA -> R.string.eje_mae_pregunta; Eje.VOZ -> R.string.eje_voz_pregunta
        Eje.VALOR -> R.string.eje_val_pregunta; Eje.EVOLUCION -> R.string.eje_evo_pregunta; Eje.TRASCENDENCIA -> R.string.eje_tra_pregunta
    }

    @StringRes fun nombre(f: Fase): Int = when (f) {
        Fase.ORIENTACION -> R.string.fase_1; Fase.PREPARACION -> R.string.fase_2; Fase.TRAVESIA -> R.string.fase_3
        Fase.ASCENSO -> R.string.fase_4; Fase.CULMINACION -> R.string.fase_5; Fase.CONTEMPLACION -> R.string.fase_6
        Fase.DESCENSO -> R.string.fase_7
    }

    @StringRes fun diagnostico(f: Fase): Int = when (f) {
        Fase.ORIENTACION -> R.string.fase_1_diag; Fase.PREPARACION -> R.string.fase_2_diag; Fase.TRAVESIA -> R.string.fase_3_diag
        Fase.ASCENSO -> R.string.fase_4_diag; Fase.CULMINACION -> R.string.fase_5_diag; Fase.CONTEMPLACION -> R.string.fase_6_diag
        Fase.DESCENSO -> R.string.fase_7_diag
    }

    @StringRes fun nivel(p: Prioridad): Int = when (p) {
        Prioridad.A -> R.string.prioridad_a; Prioridad.B -> R.string.prioridad_b
        Prioridad.C -> R.string.prioridad_c; Prioridad.D -> R.string.prioridad_d
    }

    @StringRes fun descripcion(p: Prioridad): Int = when (p) {
        Prioridad.A -> R.string.prioridad_a_desc; Prioridad.B -> R.string.prioridad_b_desc
        Prioridad.C -> R.string.prioridad_c_desc; Prioridad.D -> R.string.prioridad_d_desc
    }

    @StringRes fun nombre(d: Decision): Int = when (d) {
        Decision.CONTINUAR -> R.string.decision_continuar; Decision.ACELERAR -> R.string.decision_acelerar
        Decision.PAUSAR -> R.string.decision_pausar; Decision.ELIMINAR -> R.string.decision_eliminar
    }

    @StringRes fun descripcion(d: Decision): Int = when (d) {
        Decision.CONTINUAR -> R.string.decision_continuar_desc; Decision.ACELERAR -> R.string.decision_acelerar_desc
        Decision.PAUSAR -> R.string.decision_pausar_desc; Decision.ELIMINAR -> R.string.decision_eliminar_desc
    }

    @StringRes fun nombre(e: EstadoMeta): Int = when (e) {
        EstadoMeta.NO_INICIADA -> R.string.estado_no_iniciada; EstadoMeta.INICIADA -> R.string.estado_iniciada
        EstadoMeta.EN_PAUSA -> R.string.estado_en_pausa; EstadoMeta.EN_CURSO -> R.string.estado_en_curso
        EstadoMeta.AVANZADA -> R.string.estado_avanzada; EstadoMeta.CUMPLIDA -> R.string.estado_cumplida
    }

    @StringRes fun significado(e: EstadoMeta): Int = when (e) {
        EstadoMeta.NO_INICIADA -> R.string.estado_no_iniciada_desc; EstadoMeta.INICIADA -> R.string.estado_iniciada_desc
        EstadoMeta.EN_PAUSA -> R.string.estado_en_pausa_desc; EstadoMeta.EN_CURSO -> R.string.estado_en_curso_desc
        EstadoMeta.AVANZADA -> R.string.estado_avanzada_desc; EstadoMeta.CUMPLIDA -> R.string.estado_cumplida_desc
    }

    @StringRes fun habito(id: String): Int = when (id) {
        "VOL1" -> R.string.habito_vol1; "VOL2" -> R.string.habito_vol2; "VOL3" -> R.string.habito_vol3
        "MAE1" -> R.string.habito_mae1; "MAE2" -> R.string.habito_mae2; "MAE3" -> R.string.habito_mae3
        "VOZ1" -> R.string.habito_voz1; "VOZ2" -> R.string.habito_voz2; "VOZ3" -> R.string.habito_voz3
        "VAL1" -> R.string.habito_val1; "VAL2" -> R.string.habito_val2; "VAL3" -> R.string.habito_val3
        "EVO1" -> R.string.habito_evo1; "EVO2" -> R.string.habito_evo2; "EVO3" -> R.string.habito_evo3
        "TRA1" -> R.string.habito_tra1; "TRA2" -> R.string.habito_tra2; else -> R.string.habito_tra3
    }

    @StringRes fun tarjeta(e: Eje): Int = when (e) {
        Eje.VOLUNTAD -> R.string.tarjeta_vol; Eje.MAESTRIA -> R.string.tarjeta_mae; Eje.VOZ -> R.string.tarjeta_voz
        Eje.VALOR -> R.string.tarjeta_val; Eje.EVOLUCION -> R.string.tarjeta_evo; Eje.TRASCENDENCIA -> R.string.tarjeta_tra
    }

    @StringRes fun protocolo(n: Int): Int = when (n) {
        1 -> R.string.protocolo_1; 2 -> R.string.protocolo_2; 3 -> R.string.protocolo_3; 4 -> R.string.protocolo_4
        5 -> R.string.protocolo_5; 6 -> R.string.protocolo_6; 7 -> R.string.protocolo_7; else -> R.string.protocolo_8
    }

    @StringRes fun pregunta(matriz: Int): Int = when (matriz) {
        0 -> R.string.matriz_p1; 1 -> R.string.matriz_p2; 2 -> R.string.matriz_p3; 3 -> R.string.matriz_p4; else -> R.string.matriz_p5
    }

    @StringRes fun nombre(t: TipoPost): Int = when (t) {
        TipoPost.LOGRO -> R.string.tipo_logro; TipoPost.EVIDENCIA -> R.string.tipo_evidencia; TipoPost.VISION -> R.string.tipo_vision
        TipoPost.META -> R.string.tipo_meta; TipoPost.REFLEXION -> R.string.tipo_reflexion
    }

    @StringRes fun nombre(v: Visibilidad): Int = when (v) {
        Visibilidad.PUBLICA -> R.string.visibilidad_publica; Visibilidad.SEGUIDORES -> R.string.visibilidad_seguidores
        Visibilidad.PRIVADA -> R.string.visibilidad_privada
    }

    @StringRes fun protocoloCuando(n: Int): Int = when (n) {
        1 -> R.string.protocolo_1_cuando; 2 -> R.string.protocolo_2_cuando; 3 -> R.string.protocolo_3_cuando; 4 -> R.string.protocolo_4_cuando
        5 -> R.string.protocolo_5_cuando; 6 -> R.string.protocolo_6_cuando; 7 -> R.string.protocolo_7_cuando; else -> R.string.protocolo_8_cuando
    }

    /** Lectura del checklist diario (18 hábitos). */
    @StringRes fun lecturaChecklist(checks: Int): Int = when {
        checks >= 10 -> R.string.checklist_bien
        checks >= 8 -> R.string.checklist_avanzando
        checks > 0 -> R.string.checklist_revisa
        else -> R.string.checklist_vacio
    }

    /** Interpretación de un eje (1-10). */
    @StringRes fun interpretacionEje(p: Int): Int = when {
        p >= 8 -> R.string.eje_fuerte
        p >= 5 -> R.string.eje_desarrollo
        p >= 1 -> R.string.eje_urgente
        else -> R.string.eje_sin_evaluar
    }

    /** Interpretación del total de los 6 ejes (sobre 60). */
    @StringRes fun interpretacionTotal(total: Int): Int = when {
        total >= 48 -> R.string.total_cerca
        total >= 30 -> R.string.total_proceso
        total >= 6 -> R.string.total_inicio
        else -> R.string.total_sin
    }

    /** Interpretación de un puntaje (0..1) de una evaluación. */
    @StringRes fun interpretacion(pct: Float): Int = when {
        pct >= 0.8f -> R.string.resultado_fuerte
        pct >= 0.5f -> R.string.resultado_desarrollo
        else -> R.string.resultado_atencion
    }
}

@Composable fun Eje.texto(): String = stringResource(Textos.nombre(this))
@Composable fun Fase.texto(): String = stringResource(Textos.nombre(this))
@Composable fun Prioridad.texto(): String = stringResource(Textos.nivel(this))
@Composable fun Decision.texto(): String = stringResource(Textos.nombre(this))
@Composable fun EstadoMeta.texto(): String = stringResource(Textos.nombre(this))
@Composable fun TipoPost.texto(): String = stringResource(Textos.nombre(this))
@Composable fun Visibilidad.texto(): String = stringResource(Textos.nombre(this))
