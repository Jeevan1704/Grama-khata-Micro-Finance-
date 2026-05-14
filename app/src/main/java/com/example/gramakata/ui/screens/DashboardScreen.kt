package com.example.gramakata.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gramakata.data.CustomerWithBalance
import com.example.gramakata.viewmodel.KhataViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.example.gramakata.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: KhataViewModel,
    onCustomerClick: (Long) -> Unit,
    onProfileClick: () -> Unit
) {
    val customers by viewModel.customersWithBalance.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newCustomerName by remember { mutableStateOf("") }
    var newCustomerPhone by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Filled.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    TextButton(onClick = {
                        val newLang = if (viewModel.languageCode.value == "en") "kn" else "en"
                        viewModel.setLanguage(newLang)
                    }) {
                        Text("EN/ಕ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Button(onClick = {
                        scope.launch {
                            val report = viewModel.generateDailyCollectionReport(context)
                            snackbarHostState.showSnackbar(report)
                        }
                    }) {
                        Text(stringResource(R.string.daily_report))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, stringResource(R.string.add_customer)) },
                text = { Text(stringResource(R.string.new_customer)) }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (customers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_customers_yet))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(customers) { item ->
                        CustomerItem(item, onClick = { onCustomerClick(item.customer.id) })
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text(stringResource(R.string.add_customer)) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newCustomerName,
                            onValueChange = { newCustomerName = it },
                            label = { Text(stringResource(R.string.customer_name)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newCustomerPhone,
                            onValueChange = { newCustomerPhone = it },
                            label = { Text(stringResource(R.string.phone_number)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newCustomerName.isNotBlank()) {
                                viewModel.addCustomer(newCustomerName, newCustomerPhone)
                                newCustomerName = ""
                                newCustomerPhone = ""
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text(stringResource(R.string.add))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
    }
}

@Composable
fun CustomerItem(item: CustomerWithBalance, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.customer.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            val isDue = item.netBalance > 0
            val color = if (isDue) Color(0xFFD32F2F) else Color(0xFF388E3C)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${Math.abs(item.netBalance)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = if (isDue) stringResource(R.string.due) else stringResource(R.string.advance),
                    fontSize = 12.sp,
                    color = color
                )
            }
        }
    }
}
