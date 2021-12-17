package com.fsck.k9.ui.settings.account

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckedTextView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import androidx.preference.PreferenceDialogFragmentCompat
import com.fsck.k9.NotificationSetting
import com.fsck.k9.ui.R

class VibrationPatternDialogFragment : PreferenceDialogFragmentCompat() {
    private val vibrator by lazy { requireContext().getSystemService<Vibrator>() ?: error("Vibrator service missing") }

    private val vibratePatternPreference: VibrationPatternPreference
        get() = preference as VibrationPatternPreference

    private lateinit var adapter: VibrationPatternAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()

        val vibrationPattern: Int
        val vibrationTimes: Int
        if (savedInstanceState != null) {
            vibrationPattern = savedInstanceState.getInt(STATE_VIBRATION_PATTERN)
            vibrationTimes = savedInstanceState.getInt(STATE_VIBRATION_TIMES)
        } else {
            vibrationPattern = vibratePatternPreference.vibrationPattern
            vibrationTimes = vibratePatternPreference.vibrationTimes
        }

        adapter = VibrationPatternAdapter(
            entries = vibratePatternPreference.entries.map { it.toString() },
            entryValues = vibratePatternPreference.entryValues.map { it.toString().toInt() },
            vibrationPattern,
            vibrationTimes
        )

        return AlertDialog.Builder(context)
            .setTitle(preference.title)
            .setAdapter(adapter, null)
            .setPositiveButton(R.string.okay_action, ::onClick)
            .setNegativeButton(R.string.cancel_action, ::onClick)
            .create()
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            vibratePatternPreference.setVibrationPattern(adapter.vibrationPattern, adapter.vibrationTimes)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_VIBRATION_PATTERN, adapter.vibrationPattern)
        outState.putInt(STATE_VIBRATION_TIMES, adapter.vibrationTimes)
    }

    private fun playVibration() {
        val vibrationPattern = adapter.vibrationPattern
        val vibrationTimes = adapter.vibrationTimes
        val combinedPattern = NotificationSetting.getVibration(vibrationPattern, vibrationTimes)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrationEffect = VibrationEffect.createWaveform(combinedPattern, -1)
            vibrator.vibrate(vibrationEffect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(combinedPattern, -1)
        }
    }

    private inner class VibrationPatternAdapter(
        private val entries: List<String>,
        private val entryValues: List<Int>,
        initialVibrationPattern: Int,
        initialVibrationTimes: Int
    ) : BaseAdapter() {
        private var checkedPosition = entryValues.indexOf(initialVibrationPattern).takeIf { it != -1 } ?: 0

        val vibrationPattern: Int
            get() = entryValues[checkedPosition]

        var vibrationTimes = initialVibrationTimes

        override fun hasStableIds(): Boolean = true

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getItemViewType(position: Int): Int {
            return if (position < entries.size) 0 else 1
        }

        override fun getViewTypeCount(): Int = 2

        override fun getCount(): Int = entries.size + 1

        override fun getItem(position: Int): Any? {
            return if (position < entries.size) entries[position] else null
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val itemType = getItemViewType(position)
            return if (itemType == 0) {
                getVibrationPatternView(position, convertView, parent)
            } else {
                getVibrationTimesView(convertView, parent)
            }
        }

        private fun getVibrationPatternView(position: Int, convertView: View?, parent: ViewGroup?): View {
            return convertView.orInflate<CheckedTextView>(R.layout.preference_vibration_pattern_item, parent)
                .apply {
                    text = getItem(position) as String
                    isChecked = position == checkedPosition
                    setOnClickListener {
                        checkedPosition = position
                        playVibration()
                        notifyDataSetChanged()
                    }
                }
        }

        private fun getVibrationTimesView(convertView: View?, parent: ViewGroup?): View {
            return convertView.orInflate<View>(R.layout.preference_vibration_times_item, parent).apply {
                val vibrationTimesValue = findViewById<TextView>(R.id.vibrationTimesValue)
                vibrationTimesValue.text = vibrationTimes.toString()

                val vibrationTimesSeekBar = findViewById<SeekBar>(R.id.vibrationTimesSeekBar)
                val progress = vibrationTimes - 1
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    vibrationTimesSeekBar.setProgress(progress, false)
                } else {
                    vibrationTimesSeekBar.progress = progress
                }

                vibrationTimesSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                        vibrationTimes = progress + 1
                        vibrationTimesValue.text = vibrationTimes.toString()
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        playVibration()
                    }
                })
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T : View> View?.orInflate(layoutResId: Int, parent: ViewGroup?): T {
            val view = this ?: layoutInflater.inflate(layoutResId, parent, false)
            return view as T
        }
    }

    companion object {
        private const val STATE_VIBRATION_PATTERN = "vibrationPattern"
        private const val STATE_VIBRATION_TIMES = "vibrationTimes"
    }
}
