package com.example.appesp32

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.appesp32.ui.theme.AppESP32Theme
import com.google.firebase.database.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.Alignment

class MainActivity : ComponentActivity() {
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        database = FirebaseDatabase.getInstance().reference.child("devices").child("esp32_1")
        setContent {
            AppESP32Theme {
                AppScreen(database)
            }
        }
    }
}

@Composable
fun AppScreen(database: DatabaseReference) {
    var temperature by remember { mutableStateOf("--") }
    var humidity by remember { mutableStateOf("--") }
    var alert by remember { mutableStateOf("") }
    var ledStates by remember { mutableStateOf(listOf(false, false, false, false)) }

    LaunchedEffect(Unit) {
        database.child("sensors").child("temperature").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                temperature = snapshot.getValue(Float::class.java)?.toString() ?: "--"
            }

            override fun onCancelled(error: DatabaseError) {}
        })
        database.child("sensors").child("humidity").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                humidity = snapshot.getValue(Float::class.java)?.toString() ?: "--"
            }

            override fun onCancelled(error: DatabaseError) {}
        })
        database.child("alert").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                alert = snapshot.getValue(String::class.java) ?: ""
            }

            override fun onCancelled(error: DatabaseError) {}
        })
        for (i in 1..4) {
            database.child("leds").child("led$i").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val newStates = ledStates.toMutableList()
                    newStates[i - 1] = snapshot.getValue(Boolean::class.java) ?: false
                    ledStates = newStates
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Nhiệt độ: $temperature °C", style = MaterialTheme.typography.headlineSmall)
            Text("Độ ẩm: $humidity %", style = MaterialTheme.typography.headlineSmall)
            if (alert.isNotEmpty()) {
                Text("⚠️ $alert", color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))

            for (i in 0..3) {
                val index = i
                Button(onClick = {
                    val ledRef = database.child("leds").child("led${index + 1}")
                    ledRef.setValue(!ledStates[index])
                }) {
                    Text("LED ${index + 1}: ${if (ledStates[index]) "Bật" else "Tắt"}")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppScreenPreview() {
    AppESP32Theme {
        AppScreen(FirebaseDatabase.getInstance().reference)
    }
}