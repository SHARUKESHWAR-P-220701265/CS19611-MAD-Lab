package com.example.gotg

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

class ContactsActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("EmergencyContacts", MODE_PRIVATE)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ContactsScreen()
                }
            }
        }
    }

    @Composable
    fun ContactsScreen() {
        var contact1Name by remember { mutableStateOf(sharedPreferences.getString("contact1_name", "") ?: "") }
        var contact1Number by remember { mutableStateOf(sharedPreferences.getString("contact1_number", "") ?: "") }
        var contact2Name by remember { mutableStateOf(sharedPreferences.getString("contact2_name", "") ?: "") }
        var contact2Number by remember { mutableStateOf(sharedPreferences.getString("contact2_number", "") ?: "") }
        var contact3Name by remember { mutableStateOf(sharedPreferences.getString("contact3_name", "") ?: "") }
        var contact3Number by remember { mutableStateOf(sharedPreferences.getString("contact3_number", "") ?: "") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Emergency Contacts",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ContactRow(
                name = contact1Name,
                onNameChange = { contact1Name = it },
                number = contact1Number,
                onNumberChange = { contact1Number = it },
                label = "Contact 1"
            )
            ContactRow(
                name = contact2Name,
                onNameChange = { contact2Name = it },
                number = contact2Number,
                onNumberChange = { contact2Number = it },
                label = "Contact 2"
            )
            ContactRow(
                name = contact3Name,
                onNameChange = { contact3Name = it },
                number = contact3Number,
                onNumberChange = { contact3Number = it },
                label = "Contact 3"
            )


            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    sharedPreferences.edit().apply {
                        putString("contact1_name", contact1Name)
                        putString("contact1_number", contact1Number)
                        putString("contact2_name", contact2Name)
                        putString("contact2_number", contact2Number)
                        putString("contact3_name", contact3Name)
                        putString("contact3_number", contact3Number)
                        apply()
                    }
                    finish()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Contacts")
            }
        }
    }

    @Composable
    fun ContactRow(
        name: String,
        onNameChange: (String) -> Unit,
        number: String,
        onNumberChange: (String) -> Unit,
        label: String
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Name") },
                modifier = Modifier.weight(1.2f)
            )
            OutlinedTextField(
                value = number,
                onValueChange = onNumberChange,
                label = { Text("Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(1f)
            )
        }
    }
} 