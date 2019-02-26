package com.ses.zebra.personalshopperassistant.Activities;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.preference.PreferenceManager;

import com.ses.zebra.personalshopperassistant.Fragments.SettingsFragment;
import com.ses.zebra.personalshopperassistant.R;
import com.ses.zebra.personalshopperassistant.databinding.ActivitySettingsBinding;

public class SettingsActivity extends BaseActivity {

    // Debugging
    private static final String TAG = "SettingsActivity";

    // Constants
    private static final int DEMO_KIT_MAP = 0;
    private static final int BE_ZEC_MAP = 1;

    // Variables
    private ActivitySettingsBinding mDataBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Init DataBinding
        mDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_settings);

        // Init Title
        mDataBinding.headerText.setText("Settings");
        mDataBinding.headerIcon.setImageResource(R.drawable.ic_back);
        mDataBinding.headerIcon.setOnClickListener(view ->
                NavUtils.navigateUpFromSameTask(this));

        // Display Settings Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settingsFragmentContainer, new SettingsFragment())
                .commit();

        // Init SharedPreferences
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Initialise default values
        PreferenceManager.setDefaultValues(this, R.xml.configuration, false);
    }
}
