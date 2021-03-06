package com.htsm.lookbook.Fragments;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.geofire.GeoLocation;
import com.google.android.gms.maps.model.LatLng;
import com.htsm.lookbook.Activities.HomeActivity;
import com.htsm.lookbook.Controllers.UserController;
import com.htsm.lookbook.Dialogs.ChooseLocationDialog;
import com.htsm.lookbook.Helper.LocationHelper;
import com.htsm.lookbook.Models.User;
import com.htsm.lookbook.R;

public class SignUpFragment extends Fragment {

    private static final int PERMISSIONS_REQUEST_LOCATION = 3331;
    private static final int REQUEST_LOCATION = 3332;

    private static final String TAG = "SignUpFragment";

    private EditText mNameInput;
    private EditText mPasswordInput;
    private EditText mEmailInput;
    private EditText mNumberInput;
    private Button mLocationButton;
    private Button mSignUpButton;
    private GeoLocation mLocation;
    private UserController mUserController;
    private AlertDialog mAlertDialog;

    private String mSnackBarText;

    private static final String KEY_IS_UPDATE = "SignUpFragment.isUpdate";

    private boolean mIsUpdate;

    public static Fragment newInstance(boolean isUpdate) {
        SignUpFragment fragment = new SignUpFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean(KEY_IS_UPDATE, isUpdate);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserController = new UserController(getActivity());
        mIsUpdate = getArguments().getBoolean(KEY_IS_UPDATE);
        if(mIsUpdate)
            getActivity().setTitle("Update Profile");
        else
            getActivity().setTitle("Sign Up");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_sign_up, container, false);

        mSignUpButton = v.findViewById(R.id.id_btn_signup);
        mNameInput = v.findViewById(R.id.id_input_name);
        mEmailInput = v.findViewById(R.id.id_input_email);
        mPasswordInput = v.findViewById(R.id.id_input_pass);
        mNumberInput = v.findViewById(R.id.id_input_num);
        mLocationButton = v.findViewById(R.id.id_btn_location);

        mAlertDialog = new AlertDialog.Builder(getActivity())
                .setTitle("Creating Account...")
                .setView(new ProgressBar(getActivity()))
                .create();

        mAlertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if(mSnackBarText != null)
                    Snackbar.make(SignUpFragment.this.getView(), mSnackBarText, Snackbar.LENGTH_LONG).show();
            }
        });

        mLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mIsUpdate) {
                    showLocationDialog(new LatLng(mLocation.latitude, mLocation.longitude));
                } else {
                    requestLocationPermission();
                }
            }
        });

        mSignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(formIsValid()) {
                    mAlertDialog.show();
                    User user = new User(
                            mNameInput.getText().toString(),
                            mEmailInput.getText().toString(),
                            mNumberInput.getText().toString(),
                            mLocation);
                    if(!mIsUpdate) {
                        mUserController.signUpUser(user, mPasswordInput.getText().toString(), new UserController.OnTaskCompletedListener() {
                            @Override
                            public void onTaskSuccessful() {
                                mAlertDialog.dismiss();
                                startActivity(HomeActivity.newIntent(getActivity(), null));
                                getActivity().finish();
                                Log.i(TAG, "Account Created");
                                mSnackBarText = null;
                            }

                            @Override
                            public void onTaskFailed(Exception ex) {
                                mAlertDialog.dismiss();
                                mSnackBarText = "Account Creation failed";
                                Log.wtf(TAG, ex.toString());
                            }
                        });
                    } else {
                        mUserController.updateUserInfo(user, new UserController.OnTaskCompletedListener() {
                            @Override
                            public void onTaskSuccessful() {
                                mAlertDialog.dismiss();
                                mSnackBarText = "Account Info Updated";
                                Log.i(TAG, "Account Info Updated");
                            }

                            @Override
                            public void onTaskFailed(Exception ex) {
                                mAlertDialog.dismiss();
                                mSnackBarText = "Account Account Info Update failed";
                                Log.wtf(TAG, ex.toString());
                            }
                        });
                    }
                } else {
                    mSnackBarText = "Form is not valid!";
                }
            }
        });

        if(mIsUpdate)
            mSignUpButton.setText("Update");

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(mIsUpdate) {
            User user = mUserController.getUserInfoLocally();
            mNameInput.setText(user.getName());
            mEmailInput.setText(user.getEmail());
            mEmailInput.setEnabled(false);
            mPasswordInput.setEnabled(false);
            mPasswordInput.setText("*Can\'t Modify*");
            mNumberInput.setText(user.getNumber());
            mLocation = user.getLocation();
            updateLocationButton();
        }
    }

    private void updateLocationButton() {
        mLocationButton.setBackgroundResource(R.drawable.button_round);
        mLocationButton.setText("Update Location");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mLocationButton.setTextColor(getResources().getColor(R.color.white_shade, getActivity().getTheme()));
        } else {
            mLocationButton.setTextColor(getResources().getColor(R.color.white_shade));
        }
    }

    public void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_LOCATION);
        } else {
            fetchLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocation();
        }
    }

    private void fetchLocation() {
        new LocationHelper(getActivity()).requestLocation(new LocationHelper.LocationEnableListener() {
            @Override
            public void onLocationAvailable(Location location) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                showLocationDialog(latLng);

                Log.i(TAG, "Location fetched!\nLat: " + location.getLatitude() + " Lon: " + location.getLongitude());
                Toast.makeText(getActivity(), "Location fetched!\nLat: " + location.getLatitude() + " Lon: " + location.getLongitude(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLocationDisabled() {
                Log.wtf(TAG, "Location Disabled!");
            }
        });
    }

    private void showLocationDialog(LatLng latLng) {
        ChooseLocationDialog dialog = new ChooseLocationDialog();
        Bundle bundle = new Bundle();
        bundle.putDouble(ChooseLocationDialog.LATITUDE, latLng.latitude);
        bundle.putDouble(ChooseLocationDialog.LONGITUDE, latLng.longitude);
        dialog.setArguments(bundle);
        dialog.setTargetFragment(SignUpFragment.this, REQUEST_LOCATION);
        dialog.show(getFragmentManager(), TAG);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_LOCATION:
                    double latitude = data.getDoubleExtra(ChooseLocationDialog.LATITUDE, 0);
                    double longitude = data.getDoubleExtra(ChooseLocationDialog.LONGITUDE, 0);
                    mLocation = new GeoLocation(latitude, longitude);
                    updateLocationButton();
                    break;
                default:

            }
        }
    }

    public boolean formIsValid() {
        return mNameInput.getText().length() > 0 && mEmailInput.getText().length() > 0 &&
                mPasswordInput.getText().length() > 0 && mNumberInput.getText().length() > 0 &&
                mLocation != null;
    }
}
