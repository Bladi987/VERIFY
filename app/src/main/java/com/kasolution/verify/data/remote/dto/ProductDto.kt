package com.kasolution.verify.data.remote.dto
import com.google.gson.annotations.SerializedName

data class ProductDto(
    @SerializedName("id_producto") val id: Int = 0,
    @SerializedName("codigo") val codigo: String? = null, // Permitimos nulo temporalmente
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("id_categoria") val idCategoria: String? = null, // Viene como "1"
    @SerializedName("nombre_categoria") val nombreCategoria: String? = null,
    @SerializedName("id_proveedor") val idProveedor: String? = null, // Viene como "2"
    @SerializedName("nombre_proveedor") val nombreProveedor: String? = null,
    @SerializedName("precio_compra") val precioCompra: String? = null, // Viene como "20.00"
    @SerializedName("precio_venta") val precioVenta: String? = null,
    @SerializedName("stock") val stock: String? = null, // Viene como "150"
    @SerializedName("unidad_medida") val unidadMedida: String? = "unidad",
    @SerializedName("estado") val estado: Any? = null
)