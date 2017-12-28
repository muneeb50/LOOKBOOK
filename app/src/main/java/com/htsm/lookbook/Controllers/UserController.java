package com.htsm.lookbook.Controllers;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.htsm.lookbook.Constants.StringConsts;
import com.htsm.lookbook.Models.User;

import static com.htsm.lookbook.Constants.StringConsts.SharedPrefCredentials.Data.EMAIL;
import static com.htsm.lookbook.Constants.StringConsts.SharedPrefCredentials.Data.LATITUDE;
import static com.htsm.lookbook.Constants.StringConsts.SharedPrefCredentials.Data.LONGITUDE;
import static com.htsm.lookbook.Constants.StringConsts.SharedPrefCredentials.Data.NAME;

public class UserController {
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private Context mContext;

    private OnUserFetchedListener mOnUserFetchedListener;

    public interface OnUserFetchedListener {
        void onUserFetched(String userId, String userName,  GeoLocation location);
    }

    public UserController(Context context) {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        mContext = context;
    }

    public interface OnTaskCompletedListener {
        void onTaskSuccessful();
        void onTaskFailed(Exception ex);
    }

    public void signInUser(String email, String password, final OnTaskCompletedListener listener) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()) {
                    mDatabase.getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            String name = dataSnapshot.child(StringConsts.UserPropertyFirebase.NAME).getValue().toString();
                            String email = dataSnapshot.child(StringConsts.UserPropertyFirebase.EMAIL).getValue().toString();
                            String number = dataSnapshot.child(StringConsts.UserPropertyFirebase.NUMBER).getValue().toString();
                            String latitude = dataSnapshot.child("location").child(StringConsts.UserPropertyFirebase.LATITUDE).getValue().toString();
                            String longitude = dataSnapshot.child("location").child(StringConsts.UserPropertyFirebase.LONGITUDE).getValue().toString();

                            User user = new User(name, email, number, new GeoLocation(Float.parseFloat(latitude), Float.parseFloat(longitude)));
                            saveUserInfoLocally(user);
                            listener.onTaskSuccessful();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                } else {
                    listener.onTaskFailed(task.getException());
                }
            }
        });
    }

    public void signUpUser(final User user, String password, final OnTaskCompletedListener listener) {
        mAuth.createUserWithEmailAndPassword(user.getEmail(), password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()) {
                    mDatabase.getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            DatabaseReference geoDataLoc = null;
                            GeoFire geoFire;
                            if(task.isSuccessful()) {
                                geoDataLoc = mDatabase.getReference("geoLocation");
                                geoFire = new GeoFire(geoDataLoc);
                                geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), user.getLocation(), new GeoFire.CompletionListener() {
                                    @Override
                                    public void onComplete(String key, DatabaseError error) {
                                        saveUserInfoLocally(user);
                                        listener.onTaskSuccessful();
                                    }
                                });
                            } else {
                                mAuth.getCurrentUser().delete();
                                mAuth.signOut();
                                listener.onTaskFailed(task.getException());
                            }
                        }
                    });
                } else {
                    listener.onTaskFailed(task.getException());
                }
            }
        });
    }

    public void getUsersNearBy(GeoLocation center, double radius, OnUserFetchedListener listener) {
        mOnUserFetchedListener = listener;
        DatabaseReference geoLocRef = mDatabase.getReference("geoLocation");
        GeoFire usersGeoLoc = new GeoFire(geoLocRef);
        usersGeoLoc.queryAtLocation(center, radius).addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(final String key, final GeoLocation location) {
                mDatabase.getReference("users").child(key).child(StringConsts.UserPropertyFirebase.NAME).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(key.equals(FirebaseAuth.getInstance().getCurrentUser().getUid()))
                            return;
                        mOnUserFetchedListener.onUserFetched(key, dataSnapshot.getValue().toString(), location);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    public float getUserLongitude() {
        SharedPreferences userSharedPref = mContext.getSharedPreferences(StringConsts.SharedPrefCredentials.NAME, Context.MODE_PRIVATE);
        return Float.parseFloat(userSharedPref.getString(StringConsts.SharedPrefCredentials.Data.LONGITUDE, null));
    }

    public float getUserLatitude() {
        SharedPreferences userSharedPref = mContext.getSharedPreferences(StringConsts.SharedPrefCredentials.NAME, Context.MODE_PRIVATE);
        return Float.parseFloat(userSharedPref.getString(StringConsts.SharedPrefCredentials.Data.LATITUDE, null));
    }

    public void saveUserInfoLocally(User user) {
        SharedPreferences userSharedPref = mContext.getSharedPreferences(StringConsts.SharedPrefCredentials.NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor userSharedPrefEditor = userSharedPref.edit();
        userSharedPrefEditor.putString(EMAIL, user.getEmail());
        userSharedPrefEditor.putString(LATITUDE, Double.toString(user.getLocation().latitude));
        userSharedPrefEditor.putString(LONGITUDE, Double.toString(user.getLocation().longitude));
        userSharedPrefEditor.putString(NAME, user.getName());
        userSharedPrefEditor.commit();
    }
}
