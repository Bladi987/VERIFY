package com.kasolution.verify.UI.Employees.interfaces

import com.kasolution.verify.domain.employees.model.Employee

interface EmpleadoClickListener {
    fun onEditClick(empleado: Employee)
    fun onDeleteClick(empleado: Employee)
}