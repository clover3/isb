package com.postech.isb.preference

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.postech.isb.PostechIsb
import com.postech.isb.R
import kotlinx.coroutines.launch

class SettingsFragment : PreferenceFragmentCompat() {

    // The fragment gets its ViewModel using the 'by viewModels' delegate,
    // passing in the factory to correctly create the ViewModel instance.
    private val viewModel: SettingsViewModel by viewModels {
        val application = requireActivity().application as PostechIsb
        SettingsViewModelFactory(application.boardRepository, application.settingsManager)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // This line loads the UI from your res/xml/root_preferences.xml file
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        // Find the preferences by their keys
        val backupPref: Preference? = findPreference("fav_backup")
        val restorePref: Preference? = findPreference("fav_restore")

        // Set up click listeners that call the ViewModel to do the actual work
        backupPref?.setOnPreferenceClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Backup Favorites")
                .setMessage("Save your favorite list to your diary?")
                .setPositiveButton("OK") { _, _ ->
                    viewModel.backupFavorites()
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }

        restorePref?.setOnPreferenceClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Restore Favorites")
                .setMessage("Restore your favorite list from your diary? Existing favorites will remain.")
                .setPositiveButton("OK") { _, _ ->
                    viewModel.restoreFavorites()
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe the state from the ViewModel to show feedback to the user
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is SettingsActionState.Success -> {
                            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                            viewModel.resetState()
                        }
                        is SettingsActionState.Error -> {
                            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                            viewModel.resetState()
                        }
                        is SettingsActionState.Loading -> {
                            // Optionally, you could show a loading dialog here
                        }
                        is SettingsActionState.Idle -> {
                            // Do nothing
                        }
                    }
                }
            }
        }
    }
}
