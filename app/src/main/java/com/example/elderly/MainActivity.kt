package com.example.elderly

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.elderly.ui.theme.EderlyAppTheme
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.elderly.models.LoginRequest
import com.example.elderly.models.LoginResponse
import com.example.elderly.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response



class MainActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText

    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var forgotPassword: TextView
    private lateinit var registerText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        usernameInput = findViewById(R.id.emailInput)

        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        forgotPassword = findViewById(R.id.forgotPassword)
        registerText = findViewById(R.id.registerText)

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString()


            val password = passwordInput.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show()
            } else {
                val loginRequest = LoginRequest(
                    usuario = username,
                    contra = password,
                    recordarDisp =false
                )


                ApiClient.instance.loginUsuario(loginRequest).enqueue(object : Callback<LoginResponse> {
                    override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                        if (response.isSuccessful && response.body() != null) {
                            val loginData = response.body()!!
                            // Guardar userId en SharedPreferences
                            val sharedPreferences = getSharedPreferences("MiAppPrefs", MODE_PRIVATE)
                            val editor = sharedPreferences.edit()
                            editor.putString("userId", loginData.userId)
                            editor.apply()

                            val intent = Intent(this@MainActivity, DashboardActivity::class.java)
                            intent.putExtra("nombreUsuario", username)
                            intent.putExtra("userId", loginData.userId) // 👈 pasamos el userId
                                startActivity(intent)
                            finish()


                        } else {
                            val errorMsg = "Credenciales incorrectas"
                            Log.w("MainActivity", errorMsg) // Puedes usar Log.e si lo consideras un error crítico
                            Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        }

                    }

                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                        Toast.makeText(this@MainActivity, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
                    }
                })
            }
        }


        registerText.setOnClickListener {
            Toast.makeText(this, "Ir a pantalla de registro", Toast.LENGTH_SHORT).show()
            // Aquí irías a una nueva actividad RegisterActivity
        }

        forgotPassword.setOnClickListener {
            Toast.makeText(this, "Funcionalidad de recuperación pendiente", Toast.LENGTH_SHORT).show()
        }

        registerText.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        moveTaskToBack(true)
    }

}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    EderlyAppTheme {
        Greeting("Android")
    }
}