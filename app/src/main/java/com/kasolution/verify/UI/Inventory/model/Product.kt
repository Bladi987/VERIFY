package com.kasolution.verify.UI.Inventory.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Product(
    @SerializedName("id_producto") val id: Int = 0,
    @SerializedName("codigo") val codigo: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("id_categoria") val idCategoria: Int,
    @SerializedName("nombre_categoria") val nombreCategoria: String? = null,
    @SerializedName("id_proveedor") val idProveedor: Int,
    @SerializedName("nombre_proveedor") val nombreProveedor: String? = null,
    @SerializedName("precio_compra") val precioCompra: Double,
    @SerializedName("precio_venta") val precioVenta: Double,
    @SerializedName("stock") val stock: Int,
    @SerializedName("unidad_medida") val unidadMedida: String = "unidad",
    @SerializedName("estado") val estado: Boolean = true
) : Serializable