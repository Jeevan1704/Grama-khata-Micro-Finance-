package com.example.gramakata.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.example.gramakata.R
import com.example.gramakata.viewmodel.KhataViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: KhataViewModel,
    onBack: () -> Unit
) {
    val shopName by viewModel.shopName.collectAsState()
    val ownerName by viewModel.ownerName.collectAsState()
    val shopPhone by viewModel.shopPhone.collectAsState()
    val shopPhotoUri by viewModel.shopPhotoUri.collectAsState()

    var inputShopName by remember { mutableStateOf(shopName) }
    var inputOwnerName by remember { mutableStateOf(ownerName) }
    var inputShopPhone by remember { mutableStateOf(shopPhone) }
    var inputPhotoUri by remember { mutableStateOf<Uri?>(shopPhotoUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }) }
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(inputPhotoUri) {
        if (inputPhotoUri != null) {
            try {
                val bmp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, inputPhotoUri!!))
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, inputPhotoUri)
                    }
                }
                bitmap = bmp.asImageBitmap()
            } catch (e: Exception) {
                e.printStackTrace()
                bitmap = null
            }
        } else {
            bitmap = null
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { inputPhotoUri = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        photoPickerLauncher.launch("image/*")
                    },
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!,
                        contentDescription = "Profile Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "Add Photo",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.tap_to_change_photo), style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = inputShopName,
                onValueChange = { inputShopName = it },
                label = { Text(stringResource(R.string.shop_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = inputOwnerName,
                onValueChange = { inputOwnerName = it },
                label = { Text(stringResource(R.string.owner_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = inputShopPhone,
                onValueChange = { inputShopPhone = it },
                label = { Text(stringResource(R.string.phone_number)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    viewModel.updateProfile(
                        shopName = inputShopName,
                        ownerName = inputOwnerName,
                        phone = inputShopPhone,
                        photoUri = inputPhotoUri?.toString()
                    )
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
            }
        }
    }
}
