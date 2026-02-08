package com.kasolution.verify.UI.Employees.interfaces

import com.kasolution.verify.UI.Employees.model.Empleado

interface EmpleadoClickListener {
    fun onEditClick(empleado: Empleado)
    fun onDeleteClick(empleado: Empleado)
}