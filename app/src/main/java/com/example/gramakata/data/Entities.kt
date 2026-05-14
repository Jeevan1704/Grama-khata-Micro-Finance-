package com.example.gramakata.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val photoUri: String? = null
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val amount: Double,
    val isGive: Boolean, // True = Give (Credit), False = Take (Payment)
    val timestamp: Long = System.currentTimeMillis()
)

data class CustomerWithBalance(
    @Embedded val customer: Customer,
    val netBalance: Double
)

data class PaymentReportItem(
    val customerName: String,
    val amount: Double
)
