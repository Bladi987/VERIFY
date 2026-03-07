package com.kasolution.verify.core.utils

object NumberToLetterConverter {
    private val UNIDADES = arrayOf(
        "",
        "UN ",
        "DOS ",
        "TRES ",
        "CUATRO ",
        "CINCO ",
        "SEIS ",
        "SIETE ",
        "OCHO ",
        "NUEVE "
    )
    private val DECENAS = arrayOf(
        "DIEZ ",
        "ONCE ",
        "DOCE ",
        "TRECE ",
        "CATORCE ",
        "QUINCE ",
        "DIECISEIS ",
        "DIECISIETE ",
        "DIECIOCHO ",
        "DIECINUEVE ",
        "VEINTE ",
        "TREINTA ",
        "CUARENTA ",
        "CINCUENTA ",
        "SESENTA ",
        "SETENTA ",
        "OCHENTA ",
        "NOVENTA "
    )
    private val CENTENAS = arrayOf(
        "",
        "CIENTO ",
        "DOSCIENTOS ",
        "TRESCIENTOS ",
        "CUATROCIENTOS ",
        "QUINIENTOS ",
        "SEISCIENTOS ",
        "SETECIENTOS ",
        "OCHOCIENTOS ",
        "NOVECIENTOS "
    )

    fun convert(n: Long): String {
        if (n == 0L) return "CERO "
        if (n == 100L) return "CIEN "

        var numero = n
        var letras = ""

        if (numero >= 1000000) {
            val millones = (numero / 1000000).toInt()
            letras += if (millones == 1) "UN MILLON " else "${convert(millones.toLong())}MILLONES "
            numero %= 1000000
        }

        if (numero >= 1000) {
            val miles = (numero / 1000).toInt()
            letras += if (miles == 1) "MIL " else "${convert(miles.toLong())}MIL "
            numero %= 1000
        }

        if (numero >= 100) {
            letras += CENTENAS[(numero / 100).toInt()]
            numero %= 100
        }

        if (numero >= 20) {
            val d = (numero / 10).toInt()
            letras += DECENAS[d + 8]
            numero %= 10
            if (numero > 0) letras += "Y "
        } else if (numero >= 10) {
            letras += DECENAS[(numero - 10).toInt()]
            numero = 0
        }

        letras += UNIDADES[numero.toInt()]
        return letras
    }
}