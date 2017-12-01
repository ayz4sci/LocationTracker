package com.spykins.locationtracker.ui.registrationView;

import android.app.PendingIntent;
import android.arch.lifecycle.Observer;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.spykins.locationtracker.AppConstants;
import com.spykins.locationtracker.AppManager;
import com.spykins.locationtracker.Injector;
import com.spykins.locationtracker.R;
import com.spykins.locationtracker.Util;
import com.spykins.locationtracker.location.GeoFenceReceiver;
import com.spykins.locationtracker.model.GeoData;

import java.util.Calendar;

public class RegisterLocationActivity extends AppCompatActivity {

    private EditText mAddressText;
    private EditText mLongitudeText;
    private EditText mLatitudeText;
    private TextView mErrorTextView;
    private AppManager appManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_location);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        Injector.setApplicationContext(getApplicationContext());

        mAddressText = findViewById(R.id.addressEditText);
        mLatitudeText = findViewById(R.id.latitudeEditText);
        mLongitudeText = findViewById(R.id.longitudeEditText);
        mErrorTextView = findViewById(R.id.textViewLocationError);
    }

    public void registerButtonClicked(View view) {

        if (TextUtils.isEmpty(mAddressText.getText().toString().trim())) {
            mErrorTextView.setVisibility(View.VISIBLE);
            mErrorTextView.setText(getResources().getString(R.string.address_is_empty));
            return;
        }

        String longitudeText = mLongitudeText.getText().toString().trim();
        String latitudeText = mLatitudeText.getText().toString().trim();
        boolean isValidDouble = Util.isValidDouble(longitudeText) && Util.isValidDouble(latitudeText);

        if (!isValidDouble) {
            mErrorTextView.setVisibility(View.VISIBLE);
            mErrorTextView.setText(getResources().getString(R.string.location_is_empty));
            return;
        }

        registerGoogleApiClient(true);
    }

    private void registerGoogleApiClient(final boolean shouldSetUpGeoFencing) {

        GoogleApiClient googleApiClient = Injector.provideGoogleApiClient(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable Bundle bundle) {
               setUpGeoFencingInNeccessary(shouldSetUpGeoFencing);
            }

            @Override
            public void onConnectionSuspended(int i) {}
        });

        if (googleApiClient.isConnected()) {
            setUpGeoFencingInNeccessary(shouldSetUpGeoFencing);
        } else {
            googleApiClient.connect();
        }
    }

    private void setUpGeoFencingInNeccessary(boolean shouldSetUpGeoFencing) {
        Log.d("wale", "log onConnected");
        appManager = Injector.provideAppManager();
        if (shouldSetUpGeoFencing) {
            setUpGeoFencing();
        } else {
            fetchCurrentLocation();
        }
    }

    private void fetchCurrentLocation() {
        appManager.fetchCurrentUserLocation(this).observe(this, new Observer<Location>() {
            @Override
            public void onChanged(@Nullable Location location) {
                if (location == null) {
                    mErrorTextView.setText("Error fetching location, turn on Location and tap again");
                } else {
                    mLatitudeText.setText(String.valueOf(location.getLatitude()));
                    mLongitudeText.setText(String.valueOf(location.getLongitude()));
                    mErrorTextView.setVisibility(View.GONE);
                }
            }
        });
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void setUpGeoFencing() {
        Intent intent = new Intent(AppConstants.FENCE_RECEIVER_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        GeoFenceReceiver geoFenceReceiver = new GeoFenceReceiver();
        registerReceiver(geoFenceReceiver, new IntentFilter(AppConstants.FENCE_RECEIVER_ACTION));

        Calendar calendar = Calendar.getInstance();
        String longitude = mLongitudeText.getText().toString().trim();
        String latitude = mLatitudeText.getText().toString().trim();
        String address = mAddressText.getText().toString().trim();
        GeoData geoData;
        if (Util.isValidDouble(latitude) && Util.isValidDouble(longitude) && !address.isEmpty() ) {
            geoData = new GeoData(calendar.getTime(), Double.valueOf(latitude),
                    Double.valueOf(longitude), "Andela Office");
        } else {
            mErrorTextView.setText("Error with the input, try again");
            mErrorTextView.setVisibility(View.VISIBLE);
            return;
        }

        appManager.init(geoData, pendingIntent, this);
        finish();
    }

    public void errorTexViewClicked(View view) {
        registerGoogleApiClient(false);
    }
}