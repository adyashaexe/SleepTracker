package com.example.sleeptracker

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.SharedPreferences
import android.os.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var chronometer: Chronometer
    private lateinit var stageText: TextView
    private lateinit var startBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var resetBtn: Button
    private lateinit var settingsBtn: Button

    private var isTracking = false
    private var startTime = 0L
    private var pauseOffset = 0L

    private lateinit var prefs: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())
    private var stageRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        // FIX 2: Don't overwrite stageText with avg on launch; show neutral state instead
        showHistoricalSummaryInTitle()
        updateStopButtonState()
    }

    private fun initViews() {
        chronometer = findViewById(R.id.chronometer)
        stageText = findViewById(R.id.stageText)
        startBtn = findViewById(R.id.startBtn)
        stopBtn = findViewById(R.id.stopBtn)
        resetBtn = findViewById(R.id.resetBtn)
        settingsBtn = findViewById(R.id.settingsBtn)

        // FIX 2: Set a clear initial state for the stage label
        stageText.text = "Not Tracking"

        prefs = getSharedPreferences("sleep_data", MODE_PRIVATE)

        startBtn.setOnClickListener { startTracking() }
        stopBtn.setOnClickListener { stopTracking() }
        resetBtn.setOnClickListener { resetTracking() }
        settingsBtn.setOnClickListener { showSettingsDialog() }
    }

    private fun startTracking() {
        if (isTracking) return

        isTracking = true
        startTime = System.currentTimeMillis()

        chronometer.base = SystemClock.elapsedRealtime() - pauseOffset
        chronometer.start()

        startBtn.isEnabled = false
        updateStopButtonState()

        // FIX 3: Update stage immediately on start, then schedule periodic updates
        updateSleepStage(pauseOffset)
        scheduleStageUpdates()
    }

    private fun stopTracking() {
        if (!isTracking) return

        isTracking = false
        chronometer.stop()

        val totalTime = SystemClock.elapsedRealtime() - chronometer.base
        pauseOffset = totalTime

        stageRunnable?.let { handler.removeCallbacks(it) }

        saveSleepSession(totalTime)

        startBtn.isEnabled = true
        updateStopButtonState()

        // FIX 1: updateSleepStage is now called BEFORE showSummaryDialog so the
        // stage label reflects the final state and is not overwritten.
        updateSleepStage(totalTime)
        showSummaryDialog()
    }

    private fun resetTracking() {
        isTracking = false
        pauseOffset = 0L
        // FIX 4: Reset startTime so a subsequent session records the correct start date
        startTime = 0L

        chronometer.base = SystemClock.elapsedRealtime()
        chronometer.stop()

        stageText.text = "Not Tracking"

        startBtn.isEnabled = true
        updateStopButtonState()

        stageRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun scheduleStageUpdates() {
        stageRunnable = object : Runnable {
            override fun run() {
                if (isTracking) {
                    val elapsed = SystemClock.elapsedRealtime() - chronometer.base
                    updateSleepStage(elapsed)
                    handler.postDelayed(this, 30000)
                }
            }
        }
        // FIX 3: First periodic callback after 30s (immediate update already done in startTracking)
        handler.postDelayed(stageRunnable!!, 30000)
    }

    private fun updateSleepStage(elapsedMs: Long) {
        val elapsedMinutes = elapsedMs / (1000.0 * 60)

        // FIX 6: More realistic sleep stage progression based on elapsed minutes
        val stage = when {
            elapsedMinutes < 15   -> "Awake"
            elapsedMinutes < 30   -> "Falling Asleep"  // N1 onset
            elapsedMinutes < 60   -> "Light Sleep"     // N1/N2
            elapsedMinutes < 90   -> "Deep Sleep"      // N3 first cycle
            elapsedMinutes < 110  -> "REM Sleep"       // First REM
            elapsedMinutes < 180  -> "Light Sleep"     // N2 second cycle
            elapsedMinutes < 210  -> "Deep Sleep"      // N3 second cycle
            elapsedMinutes < 240  -> "REM Sleep"       // Second REM
            elapsedMinutes < 300  -> "Light Sleep"     // N2 third cycle
            elapsedMinutes < 330  -> "Deep Sleep"      // N3 third cycle
            else                  -> "REM Sleep"       // Late-night REM dominant
        }

        stageText.text = stage
    }

    private fun saveSleepSession(durationMs: Long) {
        val sessions = getSleepSessions()

        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        val session = mutableMapOf<String, Any>(
            "date" to formatter.format(Date(startTime)),
            "duration" to durationMs,
            "hours" to durationMs / (1000.0 * 60 * 60)
        )

        sessions.add(session)

        prefs.edit()
            .putString("sessions", listToJson(sessions))
            .apply()
    }

    private fun listToJson(sessions: List<Map<String, Any>>): String {
        val jsonArray = JSONArray()

        for (session in sessions) {
            val jsonObj = JSONObject()
            try {
                jsonObj.put("date", session["date"])
                jsonObj.put("duration", session["duration"])
                jsonObj.put("hours", session["hours"])
            } catch (e: Exception) {
                e.printStackTrace()
            }
            jsonArray.put(jsonObj)
        }

        return jsonArray.toString()
    }

    private fun getSleepSessions(): MutableList<MutableMap<String, Any>> {
        val json = prefs.getString("sessions", "[]")
        val sessions = mutableListOf<MutableMap<String, Any>>()

        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val jsonObj = jsonArray.getJSONObject(i)
                val session = mutableMapOf<String, Any>(
                    "date" to jsonObj.getString("date"),
                    "duration" to jsonObj.getLong("duration"),
                    "hours" to jsonObj.getDouble("hours")
                )
                sessions.add(session)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return sessions
    }

    // FIX 2: Renamed from loadHistoricalData; now only updates title/subtitle, not stageText
    private fun showHistoricalSummaryInTitle() {
        val sessions = getSleepSessions()
        if (sessions.isNotEmpty()) {
            var totalHours = 0.0
            for (session in sessions) {
                totalHours += session["hours"] as Double
            }
            val avgHours = totalHours / sessions.size
            // Show average in the ActionBar title instead of overwriting the stage label
            supportActionBar?.subtitle = String.format("Avg sleep: %.1fh", avgHours)
        }
    }

    private fun updateStopButtonState() {
        stopBtn.isEnabled = isTracking
    }

    private fun showSettingsDialog() {
        val goalMinutes = prefs.getInt("goal_minutes", 480)
        val hours = goalMinutes / 60
        val minutes = goalMinutes % 60

        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val newGoalMinutes = hourOfDay * 60 + minute
                prefs.edit()
                    .putInt("goal_minutes", newGoalMinutes)
                    .apply()
                checkGoalCompliance(newGoalMinutes)
            },
            hours,
            minutes,
            false
        ).show()
    }

    // FIX 5: Accept goalMinutes as a parameter and compute elapsed time correctly,
    // accounting for an active (not-yet-stopped) session.
    private fun checkGoalCompliance(goalMinutes: Int) {
        val elapsed = if (isTracking) {
            // Session is live: measure from chronometer base
            SystemClock.elapsedRealtime() - chronometer.base
        } else {
            // Session stopped or not started: use saved offset
            pauseOffset
        }

        val actualMinutes = (elapsed / (1000.0 * 60)).toInt()
        val difference = goalMinutes - actualMinutes

        val message = if (difference > 0) {
            val hrs = difference / 60
            val mins = difference % 60
            "Slept ${hrs}h ${mins}m less than ${goalMinutes / 60}h goal"
        } else {
            "Met sleep goal!"
        }

        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSummaryDialog() {
        val sessions = getSleepSessions()

        var avgHours = 0.0
        for (session in sessions) {
            avgHours += session["hours"] as Double
        }
        if (sessions.isNotEmpty()) avgHours /= sessions.size

        val thisSessionHours = pauseOffset / (1000.0 * 60 * 60)

        AlertDialog.Builder(this)
            .setTitle("Sleep Summary")
            .setMessage(
                String.format(
                    "This session: %.1fh\nAll-time average: %.1fh",
                    thisSessionHours,
                    avgHours
                )
            )
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}