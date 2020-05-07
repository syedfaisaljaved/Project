package com.faisaljaved.myparking.WorkFlowActivities.ProfileActivites;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.faisaljaved.myparking.BaseActivity;
import com.faisaljaved.myparking.MessageActivity;
import com.faisaljaved.myparking.R;
import com.faisaljaved.myparking.WorkFlowActivities.ChatsActivity;
import com.faisaljaved.myparking.models.ChatUsers;
import com.faisaljaved.myparking.models.MyAdData;
import com.faisaljaved.myparking.models.UserDetails;
import com.faisaljaved.myparking.viewmodels.SingleAdViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SingleAdActivity extends BaseActivity {

    private static final String TAG = "SingleAdActivity";

    private SingleAdViewModel mViewModel;

    //Ui Components
    private TextView mTitle, mPrice, mVehicletype, mLocation, mTimeStamp, mDescription, mUsername, mYourAd;
    private AppCompatImageView mImage;
    private RelativeLayout mParentLayout;
    private LinearLayout mChatButton, mCallButton, mBottomView;


    //firebase
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private String userId;
    private FirebaseDatabase database;
    private DatabaseReference reference;

    private String mPhoneNumber;
    private MyAdData adData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_ad_view);

        initFirebase();
        mPrice = findViewById(R.id.view_ad_price);
        mTitle = findViewById(R.id.view_ad_title);
        mVehicletype = findViewById(R.id.view_ad_vehicle_type);
        mLocation = findViewById(R.id._view_ad_location);
        mTimeStamp = findViewById(R.id.view_ad_timestamp);
        mImage = findViewById(R.id.view_ad_image);
        mDescription = findViewById(R.id.view_ad_description);
        mParentLayout = findViewById(R.id.parent);
        mUsername = findViewById(R.id.view_ad_username);
        mChatButton = findViewById(R.id.view_ad_chat_button);
        mCallButton = findViewById(R.id.view_ad_call_button);
        mBottomView = findViewById(R.id.bottom_view);
        mYourAd = findViewById(R.id.your_ad);

        mViewModel = new ViewModelProvider(this).get(SingleAdViewModel.class);

        showProgressBar(true);
        subscribeObservers();
        getIncomingIntent();

        mCallButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + getPhoneNumber()));
                startActivity(intent);
            }
        });

        mChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                connectBothUsers();
                Intent intent = new Intent(SingleAdActivity.this, MessageActivity.class);
                intent.putExtra("uid",adData.getUID());
                startActivity(intent);
            }
        });
    }

    private void connectBothUsers(){
        if (adData != null){


            fetchUserDetails(new MyCallback() {
                @Override
                public void onCallback(final UserDetails seller) {

                    fetchUserDetails(new MyCallback() {
                        @Override
                        public void onCallback(UserDetails buyer) {
                            String adImage = adData.getImages().get(0);
                            String adTitle = adData.getTitle();
                            String adId = adData.getAdId();

                            //fetched username and userimage using callback
                            final ChatUsers usersList = new ChatUsers(adImage,seller.getImage(),seller.getUsername(),buyer.getUsername(),adTitle,adId,adData.getUID(),userId);

                            final DatabaseReference postReference = reference.child("sellers_buyers");
                            postReference.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (!dataSnapshot.child(userId).child("buying").child(adData.getUID()).child(adData.getAdId()).exists()) {

                                        postReference.child(userId).child("buying").child(adData.getUID()).child(adData.getAdId()).setValue(usersList);
                                        postReference.child(adData.getUID()).child("selling").child(adData.getAdId()).child(userId).setValue(usersList);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }
                    },userId);
                }
            },adData.getUID());

        }
    }

    private void fetchUserDetails(final MyCallback myCallback, String name) {
        DatabaseReference userRef = reference.child("userDetails").child(name);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                UserDetails user = dataSnapshot.getValue(UserDetails.class);

                myCallback.onCallback(user);
                Log.d(TAG, "fetchUserDetails: image "+user.getImage());
                Log.d(TAG, "fetchUserDetails: username "+user.getUsername());

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void initFirebase() {

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        userId = firebaseUser.getUid();
        database = FirebaseDatabase.getInstance();
        reference = database.getReference();

    }

    private void getIncomingIntent() {
        if (getIntent().hasExtra("adData")){
            adData = getIntent().getParcelableExtra("adData");
            mViewModel.loadSingleAdFromFirebase(adData.getUID(), adData.getAdId());
        }
    }


    private void subscribeObservers(){
        mViewModel.getData().observe(this, new Observer<MyAdData>() {
            @Override
            public void onChanged(MyAdData adData) {
                if (adData != null){
                    if (adData.getAdId().equals(mViewModel.getAdId())){
                        setViewAdProperties(adData);
                    }
                }
            }
        });
        mViewModel.getUserDetails().observe(this, new Observer<UserDetails>() {
            @Override
            public void onChanged(UserDetails userDetails) {
                if (userDetails != null){
                    mUsername.setText(userDetails.getUsername());
                    setPhoneNumber(userDetails.getNumber());
                }
            }
        });
    }

    private void setViewAdProperties(MyAdData adData){
        if (adData != null){

            RequestOptions requestOptions = new RequestOptions()
                    .placeholder(R.drawable.blank_image);

            Glide.with(this)
                    .setDefaultRequestOptions(requestOptions)
                    .load(adData.getImages().get(0))
                    .into(mImage);

            mTitle.setText(adData.getTitle());

            String price = NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(Integer.parseInt(adData.getPrice()));
            mPrice.setText(price);

            String vehicletype = "PARKING: "+ adData.getVehicleType();
            mVehicletype.setText(vehicletype);

            mLocation.setText(adData.getLocation());

            long time = adData.getTimestamp();
            CharSequence relativeDate = DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
            mTimeStamp.setText(relativeDate);

            mDescription.setText(adData.getDescription());

            if (!adData.getUID().equals(userId)){
                Log.d(TAG, "setViewAdProperties: "+ adData.getUID().equals(userId));
                mBottomView.setVisibility(View.VISIBLE);
                mYourAd.setVisibility(View.GONE);
            }else if(adData.getUID().equals(userId)) {
                mBottomView.setVisibility(View.GONE);
                mYourAd.setVisibility(View.VISIBLE);
            }

            Log.d(TAG, "setViewAdProperties: "+ adData.getUID() + " and "+ userId);

        }

        showParent();
        showProgressBar(false);
    }

    private void showParent(){
        mParentLayout.setVisibility(View.VISIBLE);
    }

    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    public void setPhoneNumber(String mPhoneNumber) {
        this.mPhoneNumber = mPhoneNumber;
    }

    public interface MyCallback {
        void onCallback(UserDetails value);
    }
}
