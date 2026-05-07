package com.kasolution.verify.domain.reports.model

import android.os.Parcelable
import com.kasolution.verify.UI.Reports.model.ProductoCritico
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ReporteInventario(
    val capital_invertido: Double,
    val valor_venta_estimado: Double,
    val productos_criticos: Int,
    val productos_agotados: Int,
    val lista_productos_criticos: List<ProductoCritico> = emptyList()
) : Parcelable