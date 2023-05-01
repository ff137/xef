package com.xebia.functiona.langchain4k.auto

import com.xebia.functional.auto.ai
import kotlinx.serialization.Serializable

@Serializable
data class Employee(val firstName: String, val lastName: String, val age: Int, val position: String, val company: Company)

@Serializable
data class Address(val street: String, val city: String, val country: String)

@Serializable
data class Company(val name: String, val address: Address)

suspend fun main() {
    val complexPrompt = "Provide information for an Employee that includes their first name, last name, age, position, and their company's name and address (street, city, and country)."

    val employeeData: Employee = ai(complexPrompt)

    println("Employee Information:\n\n" +
            "Name: ${employeeData.firstName} ${employeeData.lastName}\n" +
            "Age: ${employeeData.age}\n" +
            "Position: ${employeeData.position}\n" +
            "Company: ${employeeData.company.name}\n" +
            "Address: ${employeeData.company.address.street}, ${employeeData.company.address.city}, ${employeeData.company.address.country}")
}