package com.example.gramakata.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gramakata.data.AppDatabase
import com.example.gramakata.data.Customer
import com.example.gramakata.data.CustomerWithBalance
import com.example.gramakata.data.Transaction
import com.example.gramakata.data.PaymentReportItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import androidx.core.content.edit
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class KhataViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).khataDao()
    private val prefs = application.getSharedPreferences("grama_khata_prefs", Context.MODE_PRIVATE)

    private val _languageCode = MutableStateFlow(prefs.getString("language", "en") ?: "en")
    val languageCode: StateFlow<String> = _languageCode

    // Profile StateFlows
    private val _shopName = MutableStateFlow(prefs.getString("shop_name", "") ?: "")
    val shopName: StateFlow<String> = _shopName
    
    private val _ownerName = MutableStateFlow(prefs.getString("owner_name", "") ?: "")
    val ownerName: StateFlow<String> = _ownerName
    
    private val _shopPhone = MutableStateFlow(prefs.getString("shop_phone", "") ?: "")
    val shopPhone: StateFlow<String> = _shopPhone
    
    private val _shopPhotoUri = MutableStateFlow(prefs.getString("shop_photo_uri", null))
    val shopPhotoUri: StateFlow<String?> = _shopPhotoUri

    fun setLanguage(code: String) {
        prefs.edit { putString("language", code) }
        _languageCode.value = code
    }

    fun updateProfile(shopName: String, ownerName: String, phone: String, photoUri: String?) {
        prefs.edit {
            putString("shop_name", shopName)
            putString("owner_name", ownerName)
            putString("shop_phone", phone)
            putString("shop_photo_uri", photoUri)
        }
        _shopName.value = shopName
        _ownerName.value = ownerName
        _shopPhone.value = phone
        _shopPhotoUri.value = photoUri
    }

    val customersWithBalance: StateFlow<List<CustomerWithBalance>> = dao.getCustomersWithBalance()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addCustomer(name: String, phone: String = "") {
        viewModelScope.launch {
            dao.insertCustomer(Customer(name = name, phone = phone))
        }
    }

    fun addTransaction(customerId: Long, amount: Double, isGive: Boolean) {
        viewModelScope.launch {
            dao.insertTransaction(Transaction(customerId = customerId, amount = amount, isGive = isGive))
        }
    }

    fun getTransactionsForCustomer(customerId: Long) = dao.getTransactionsForCustomer(customerId)

    fun getCustomerById(customerId: Long) = dao.getCustomerById(customerId)

    suspend fun generateDailyCollectionReport(context: Context): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        val payments = dao.getPaymentReportItemsSince(startOfDay)
        if (payments.isEmpty()) return context.getString(com.example.gramakata.R.string.report_no_collections)

        var total = 0.0
        val sb = java.lang.StringBuilder("${context.getString(com.example.gramakata.R.string.report_title)}\n--------------------\n")
        
        payments.forEach {
            total += it.amount
            sb.append("${it.customerName}: ₹${it.amount}\n")
        }
        sb.append("--------------------\n")
        sb.append("${context.getString(com.example.gramakata.R.string.report_total_amount)}: ₹$total\n")
        return sb.toString()
    }

    fun deleteCustomer(customerId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            dao.deleteTransactionsByCustomerId(customerId)
            dao.deleteCustomerById(customerId)
            onSuccess()
        }
    }
}
