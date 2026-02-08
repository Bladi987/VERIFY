package com.kasolution.verify.core.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kasolution.verify.R

object PermissionHelper {

    // Comprobar si un permiso ya fue concedido
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    // Comprobar múltiples permisos a la vez
    fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Método para explicar por qué se necesita el permiso antes de pedirlo
    // Aquí integramos tu DialogHelper personalizado
    fun askWithExplanation(
        activity: Activity,
        permission: String,
        title: String,
        message: String,
        requestLauncher: ActivityResultLauncher<String>
    ) {
        if (hasPermission(activity, permission)) {
            // Ya lo tiene, no hacemos nada o ejecutamos lógica
            return
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            // Mostramos tu DialogHelper personalizado
            DialogHelper.showNotification(
                context = activity,
                title = title,
                message = message,
                icon = R.drawable.ic_warning, // Usa tu icono de alerta
                colorRes = android.R.color.holo_orange_dark
            ) {
                requestLauncher.launch(permission)
            }
        } else {
            // Pedir directamente
            requestLauncher.launch(permission)
        }
    }
}