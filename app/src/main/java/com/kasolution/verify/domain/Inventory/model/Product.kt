package com.kasolution.verify.domain.Inventory.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Product(
    val id: Int,
    val codigo: String,
    val nombre: String,
    val idCategoria: Int,
    val nombreCategoria: String?,
    val idProveedor: Int,
    val nombreProveedor: String?,
    val precioCompra: Double,
    val precioVenta: Double,
    val stock: Int,
    val unidadMedida: String,
    val estado: Boolean
) : Parcelable