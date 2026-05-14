package com.example.gramakata.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KhataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Insert
    suspend fun insertTransaction(transaction: Transaction)

    @Query("""
        SELECT c.*, COALESCE(SUM(CASE WHEN t.isGive THEN t.amount ELSE -t.amount END), 0.0) as netBalance
        FROM customers c
        LEFT JOIN transactions t ON c.id = t.customerId
        GROUP BY c.id
        ORDER BY netBalance DESC
    """)
    fun getCustomersWithBalance(): Flow<List<CustomerWithBalance>>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getTransactionsForCustomer(customerId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM customers WHERE id = :customerId")
    fun getCustomerById(customerId: Long): Flow<Customer?>
    
    @Query("SELECT c.name as customerName, t.amount as amount FROM transactions t INNER JOIN customers c ON t.customerId = c.id WHERE t.isGive = 0 AND t.timestamp >= :startOfDay ORDER BY t.timestamp DESC")
    suspend fun getPaymentReportItemsSince(startOfDay: Long): List<PaymentReportItem>

    @Query("DELETE FROM customers WHERE id = :customerId")
    suspend fun deleteCustomerById(customerId: Long)

    @Query("DELETE FROM transactions WHERE customerId = :customerId")
    suspend fun deleteTransactionsByCustomerId(customerId: Long)
}
