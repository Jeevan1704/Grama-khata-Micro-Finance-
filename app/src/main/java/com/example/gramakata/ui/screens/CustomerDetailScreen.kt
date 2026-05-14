package com.example.gramakata.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.gramakata.R
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gramakata.data.Transaction
import com.example.gramakata.viewmodel.KhataViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerId: Long,
    viewModel: KhataViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val customerFlow = remember(customerId) { viewModel.getCustomerById(customerId) }
    val customer by customerFlow.collectAsState(initial = null)
    
    val transactionsFlow = remember(customerId) { viewModel.getTransactionsForCustomer(customerId) }
    val transactions by transactionsFlow.collectAsState(initial = emptyList())
    
    val shopNameState by viewModel.shopName.collectAsState()
    val displayShopName = if (shopNameState.isNotBlank()) shopNameState else stringResource(R.string.app_name)
    
    var showTransactionDialog by remember { mutableStateOf(false) }
    var isGiveDialog by remember { mutableStateOf(true) }
    var amountInput by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val netBalance = transactions.sumOf { if (it.isGive) it.amount else -it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customer?.name ?: stringResource(R.string.customer_details)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_customer))
                    }
                    IconButton(onClick = {
                        customer?.let {
                            if (it.phone.isBlank()) {
                                Toast.makeText(context, context.getString(R.string.no_phone_number), Toast.LENGTH_SHORT).show()
                            } else if (netBalance <= 0) {
                                Toast.makeText(context, context.getString(R.string.no_pending_balance), Toast.LENGTH_SHORT).show()
                            } else {
                                sendSms(context, it.phone, netBalance, displayShopName)
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Email, contentDescription = "Send SMS")
                    }
                }
            )
        },
        bottomBar = {
            // One hand usable buttons placed at the bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        isGiveDialog = true
                        showTransactionDialog = true
                    },
                    modifier = Modifier.weight(1f).height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text(stringResource(R.string.give_credit), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        isGiveDialog = false
                        showTransactionDialog = true
                    },
                    modifier = Modifier.weight(1f).height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                ) {
                    Text(stringResource(R.string.take_payment), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Balance Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.net_due), fontSize = 16.sp)
                    Text(
                        text = "₹${Math.abs(netBalance)}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (netBalance > 0) Color(0xFFD32F2F) else Color(0xFF388E3C)
                    )
                    Text(
                        text = if (netBalance > 0) stringResource(R.string.customer_owes_you) else if (netBalance < 0) stringResource(R.string.you_owe_customer) else stringResource(R.string.settled),
                        color = Color.Gray
                    )
                }
            }

            Text(
                stringResource(R.string.transaction_history),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(transactions) { tx ->
                    TransactionItem(tx)
                }
            }
        }

        if (showTransactionDialog) {
            var sendSmsChecked by remember { mutableStateOf(customer?.phone?.isNotBlank() == true) }
            AlertDialog(
                onDismissRequest = { showTransactionDialog = false },
                title = { Text(if (isGiveDialog) stringResource(R.string.give_credit_dialog) else stringResource(R.string.take_payment_dialog)) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            label = { Text(stringResource(R.string.amount_rupees)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        if (customer?.phone?.isNotBlank() == true) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = sendSmsChecked,
                                    onCheckedChange = { sendSmsChecked = it }
                                )
                                Text(stringResource(R.string.send_sms_to_customer))
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val amount = amountInput.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            viewModel.addTransaction(customerId, amount, isGiveDialog)
                            
                            if (sendSmsChecked && customer?.phone?.isNotBlank() == true) {
                                sendTransactionSms(context, customer!!.phone, amount, isGiveDialog, displayShopName)
                            }
                            
                            showTransactionDialog = false
                            amountInput = ""
                        }
                    }) {
                        Text(stringResource(R.string.save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTransactionDialog = false }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.delete_customer)) },
                text = { Text(stringResource(R.string.delete_customer_confirmation)) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteCustomer(customerId) {
                                showDeleteDialog = false
                                onBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
    }
}

@Composable
fun TransactionItem(tx: Transaction) {
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(tx.timestamp))
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(dateStr, color = Color.Gray, fontSize = 12.sp)
            Text(if (tx.isGive) stringResource(R.string.credit_given) else stringResource(R.string.payment_received), fontWeight = FontWeight.SemiBold)
        }
        Text(
            text = "₹${tx.amount}",
            color = if (tx.isGive) Color(0xFFD32F2F) else Color(0xFF388E3C),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}


fun sendSms(context: Context, phoneNumber: String, amountOwed: Double, shopName: String) {
    if (amountOwed <= 0) return
    
    val message = context.getString(R.string.reminder_message, amountOwed.toString(), shopName)
    
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("sms:$phoneNumber")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
        } catch (e2: Exception) {
            Toast.makeText(context, "Failed to open SMS app", Toast.LENGTH_SHORT).show()
        }
    }
}

fun sendTransactionSms(context: Context, phoneNumber: String, amount: Double, isGive: Boolean, shopName: String) {
    val message = if (isGive) {
        context.getString(R.string.message_credit_given, amount.toString(), shopName)
    } else {
        context.getString(R.string.message_payment_received, amount.toString(), shopName)
    }
    
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("sms:$phoneNumber")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
        } catch (e2: Exception) {
            Toast.makeText(context, "Failed to open SMS app", Toast.LENGTH_SHORT).show()
        }
    }
}
