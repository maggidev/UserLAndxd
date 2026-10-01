package tech.ula

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import tech.ula.library.MainActivity as LibraryMainActivity
import tech.ula.library.model.entities.Session
import tech.ula.library.ui.SessionManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class MainActivity : AppCompatActivity() {

    private lateinit var btnStartSetup: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var tvTerminalOutput: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var terminalBuffer = StringBuilder()
    private var isSetupRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnStartSetup = findViewById(R.id.btn_start_setup)
        progressBar = findViewById(R.id.progress_bar)
        tvStatus = findViewById(R.id.tv_status)
        tvTerminalOutput = findViewById(R.id.tv_terminal_output)

        btnStartSetup.setOnClickListener { startBotSetup() }

        checkExistingSession()
    }

    private fun checkExistingSession() {
        val sessionManager = SessionManager.getInstance(this)
        val sessions = sessionManager.getSessions()
        val alpineSession = sessions.find { it.distro == "alpine" && it.name == "whatsapp-bot" }

        if (alpineSession != null) {
            tvStatus.text = getString(R.string.status_session_exists)
            btnStartSetup.text = getString(R.string.btn_resume_bot)
            btnStartSetup.setOnClickListener { resumeBotSession(alpineSession) }
        }
    }

    private fun startBotSetup() {
        if (isSetupRunning) return
        isSetupRunning = true

        btnStartSetup.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        tvStatus.text = getString(R.string.status_downloading_alpine)
        tvTerminalOutput.text = ""
        terminalBuffer.setLength(0)

        val intent = Intent(this, LibraryMainActivity::class.java)
        intent.putExtra("distro", "alpine")
        intent.putExtra("app_name", "whatsapp-bot")
        intent.putExtra("auto_start_script", true)
        intent.putExtra("script_path", "/root/setup_bot.sh")
        startActivity(intent)
    }

    private fun resumeBotSession(session: Session) {
        val intent = Intent(this, LibraryMainActivity::class.java)
        intent.putExtra("session", session)
        startActivity(intent)
    }

    fun updateStatus(status: String, progress: Int = -1) {
        handler.post {
            tvStatus.text = status
            if (progress >= 0) {
                progressBar.progress = progress
            }
        }
    }

    fun appendTerminalOutput(line: String) {
        handler.post {
            terminalBuffer.append(line).append("\n")
            tvTerminalOutput.text = terminalBuffer.toString()
        }
    }

    fun onSetupComplete(success: Boolean) {
        handler.post {
            isSetupRunning = false
            progressBar.visibility = View.GONE
            if (success) {
                tvStatus.text = getString(R.string.status_bot_running)
                btnStartSetup.text = getString(R.string.btn_open_session)
                btnStartSetup.visibility = View.VISIBLE
                btnStartSetup.setOnClickListener {
                    checkExistingSession()
                }
            } else {
                tvStatus.text = getString(R.string.status_setup_failed)
                btnStartSetup.text = getString(R.string.btn_retry)
                btnStartSetup.visibility = View.VISIBLE
                btnStartSetup.setOnClickListener { startBotSetup() }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}