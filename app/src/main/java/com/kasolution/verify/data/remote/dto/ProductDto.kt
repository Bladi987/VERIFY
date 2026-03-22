package com.kasolution.verify.data.remote.dto
import com.google.gson.annotations.SerializedName

data class ProductDto(
    @SerializedName("id_producto") val id: Int? = null,
    @SerializedName("codigo") val codigo: String? = null,
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("id_categoria") val idCategoria: Int? = null,
    @SerializedName("nombre_categoria") val nombreCategoria: String? = null,
    @SerializedName("id_proveedor") val idProveedor: Int? = null,
    @SerializedName("nombre_proveedor") val nombreProveedor: String? = null,
    @SerializedName("precio_compra") val precioCompra: Double? = null,
    @SerializedName("precio_venta") val precioVenta: Double? = null,
    @SerializedName("stock") val stock: Int? = null,
    @SerializedName("unidad_medida") val unidadMedida: String? = null,
    @SerializedName("estado") val estado: Boolean? = null
)