package com.kasolution.verify.domain.Inventory.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Product(
    val id: Int = 0,
    val codigo: String = "",
    val nombre: String = "",
    val idCategoria: Int = 0,
    val nombreCategoria: String? = null,
    val idProveedor: Int = 0,
    val nombreProveedor: String? = null,
    val precioCompra: Double = 0.0,
    val precioVenta: Double = 0.0,
    val stock: Int = 0,
    val unidadMedida: String = "unidad",
    val estado: Boolean = true
) : Parcelable