package com.kasolution.verify.UI.Employees.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Empleado(
    @SerializedName("id_empleado") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("usuario") val usuario: String,
    @SerializedName("rol") val rol: String,
    @SerializedName("estado") val estado: Boolean
) : Serializable {
    /**
     * Propiedad de utilidad para obtener las iniciales (Ej: J A de Juan Antonio).
     */
    val initials: String
        // Asumiendo que las iniciales se extraen de las primeras letras del nombre
        get() = nombre.split(" ")
            .take(2) // Tomar las dos primeras palabras (ej: Nombre y Apellido)
            .mapNotNull { it.firstOrNull() }
            .joinToString("")
            .uppercase()
}