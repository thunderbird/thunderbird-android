package com.fsck.k9.ui.settings.account

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckedTextView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.widget.SwitchCompat
import androidx.preference.PreferenceDialogFragmentCompat
import app.k9mail.legacy.notification.NotificationVibration
import app.k9mail.legacy.notification.VibratePattern
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.bundle.getEnum
import com.fsck.k9.ui.base.bundle.putEnum
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import org.koin.android.ext.android.inject
import com.fsck.k9.ui.base.R as BaseR

class VibrationDialogFragment : PreferenceDialogFragmentCompat() {
    private val vibrator: Vibrator by inject()

    private val vibrationPreference: VibrationPreference
        get() = preference as VibrationPreference

    private lateinit var adapter: VibrationPatternAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()

        val isVibrationEnabled: Boolean
        val vibratePattern: VibratePattern
        val vibrationTimes: Int
        if (savedInstanceState != null) {
            isVibrationEnabled = savedInstanceState.getBoolean(STATE_VIBRATE)
            vibratePattern = savedInstanceState.getEnum(STATE_VIBRATE_PATTERN)
            vibrationTimes = savedInstanceState.getInt(STATE_VIBRATION_TIMES)
        } else {
            isVibrationEnabled = vibrationPreference.isVibrationEnabled
            vibratePattern = vibrationPreference.vibratePattern
            vibrationTimes = vibrationPreference.vibrationTimes
        }

        adapter = VibrationPatternAdapter(
            isVibrationEnabled,
            entries = vibrationPreference.entries.map { it.toString() },
            entryValues = vibrationPreference.entryValues.map { it.toString().toInt() },
            vibratePattern,
            vibrationTimes,
        )

        return MaterialAlertDialogBuilder(context)
            .setAdapter(adapter, null)
            .setPositiveButton(BaseR.string.okay_action, ::onClick)
            .setNegativeButton(BaseR.string.cancel_action, ::onClick)
            .create()
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            vibrationPreference.setVibration(
                isVibrationEnabled = adapter.isVibrationEnabled,
                vibratePattern = adapter.vibratePattern,
                vibrationTimes = adapter.vibrationTimes,
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_VIBRATE, adapter.isVibrationEnabled)
        outState.putEnum(STATE_VIBRATE_PATTERN, adapter.vibratePattern)
        outState.putInt(STATE_VIBRATION_TIMES, adapter.vibrationTimes)
    }

    private fun playVibration() {
        val vibratePattern = adapter.vibratePattern
        val vibrationTimes = adapter.vibrationTimes
        val vibrationPattern = NotificationVibration.getSystemPattern(vibratePattern, vibrationTimes)

        vibrator.vibrate(vibrationPattern)
    }

    private inner class VibrationPatternAdapter(
        var isVibrationEnabled: Boolean,
        private val entries: List<String>,
        private val entryValues: List<Int>,
        initialVibratePattern: VibratePattern,
        initialVibrationTimes: Int,
    ) : BaseAdapter() {
        private var checkedEntryIndex = entryValues.indexOf(initialVibratePattern.serialize()).takeIf { it != -1 } ?: 0

        val vibratePattern: VibratePattern
            get() = VibratePattern.deserialize(entryValues[checkedEntryIndex])

        var vibrationTimes = initialVibrationTimes

        override fun hasStableIds(): Boolean = true

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getItemViewType(position: Int): Int {
            return when {
                position == 0 -> 0
                position.toEntryIndex() < entries.size -> 1
                else -> 2
            }
        }

        override fun getViewTypeCount(): Int = 3

        override fun getCount(): Int = entries.size + 2

        override fun getItem(position: Int): Any? {
            return when {
                position == 0 -> null
                position.toEntryIndex() < entries.size -> entries[position.toEntryIndex()]
                else -> null
            }
        }

        private fun Int.toEntryIndex() = this - 1

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            return when (getItemViewType(position)) {
                0 -> getVibrationSwitchView(convertView, parent)
                1 -> getVibrationPatternView(position, convertView, parent)
                2 -> getVibrationTimesView(convertView, parent)
                else -> error("Unknown item type")
            }
        }

        private fun getVibrationSwitchView(convertView: View?, parent: ViewGroup?): View {
            return convertView.orInflate<View>(R.layout.preference_vibration_switch_item, parent)
                .apply {
                    val switchButton = findViewById<SwitchCompat>(R.id.vibrationSwitch)
                    switchButton.isChecked = isVibrationEnabled
                    switchButton.setOnCheckedChangeListener { _, isChecked ->
                        isVibrationEnabled = isChecked
                        notifyDataSetChanged()
                    }

                    findViewById<View>(R.id.switchContainer).setOnClickListener {
                        switchButton.toggle()
                    }
                }
        }

        private fun getVibrationPatternView(position: Int, convertView: View?, parent: ViewGroup?): View {
            return convertView.orInflate<CheckedTextView>(R.layout.preference_vibration_pattern_item, parent)
                .apply {
                    text = getItem(position) as String
                    val entryIndex = position.toEntryIndex()
                    isChecked = entryIndex == checkedEntryIndex
                    isEnabled = isVibrationEnabled
                    setOnClickListener {
                        checkedEntryIndex = entryIndex
                        playVibration()
                        notifyDataSetChanged()
                    }
                }
        }

        private fun getVibrationTimesView(convertView: View?, parent: ViewGroup?): View {
            return convertView.orInflate<View>(R.layout.preference_vibration_times_item, parent).apply {
                val vibrationTimesValue = findViewById<MaterialTextView>(R.id.vibrationTimesValue)
                vibrationTimesValue.text = vibrationTimes.toString()

                val vibrationTimesSeekBar = findViewById<SeekBar>(R.id.vibrationTimesSeekBar).apply {
                    isEnabled = isVibrationEnabled
                }

                val progress = vibrationTimes - 1
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    vibrationTimesSeekBar.setProgress(progress, false)
                } else {
                    vibrationTimesSeekBar.progress = progress
                }

                vibrationTimesSeekBar.setOnSeekBarChangeListener(
                    object : OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                            vibrationTimes = progress + 1
                            vibrationTimesValue.text = vibrationTimes.toString()
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

                        override fun onStopTrackingTouch(seekBar: SeekBar) {
                            playVibration()
                        }
                    },
                )
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T : View> View?.orInflate(layoutResId: Int, parent: ViewGroup?): T {
            val view = this ?: layoutInflater.inflate(layoutResId, parent, false)
            return view as T
        }
    }

    companion object {
        private const val STATE_VIBRATE = "vibrate"
        private const val STATE_VIBRATE_PATTERN = "vibratePattern"
        private const val STATE_VIBRATION_TIMES = "vibrationTimes"
    }
}
