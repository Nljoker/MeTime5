package com.superawesome.metime;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;

import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigServerException;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.gson.internal.bind.ObjectTypeAdapter;
import com.google.protobuf.StringValue;
import com.google.protobuf.Value;
import com.lorentzos.flingswipe.SwipeFlingAdapterView;

import org.w3c.dom.Document;
import org.w3c.dom.Text;

import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
private ArrayList<String> al;
private ArrayAdapter<String> arrayAdapter;
private int i;
private FirebaseAnalytics mFirebaseAnalytics;
private DatabaseReference swipeDb;
private FirebaseAuth mAuth;
private FirebaseUser mCurrentUser;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_menu, menu);
        return true; //show menu options

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true; // go back to the first screen

            case R.id.cardoverview:
                startActivity(new Intent(this, cardviewactivity.class));
                return true; //go to the screen where all the swiped right activities are stored


        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swipeDb = FirebaseDatabase.getInstance().getReference().child("Users");
        final FirebaseFirestore db = FirebaseFirestore.getInstance();



        //Anonymous user login to be registered in Firebase
        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser(); // save the UID of the user in users in Firebase without having to log in

        final String UID = mAuth.getCurrentUser().getUid();
        DatabaseReference currentUserDb = FirebaseDatabase.getInstance().getReference().child("Users").child("UID");
        currentUserDb.setValue(UID); //save the User UID to the Firebase Realtime database


        // Calling to the AdMob API to add advertisements to the banner in the main view.Gotta make that dough
        // Change this to big cards in the future
        final AdView adView = findViewById(R.id.adView);

        final AdRequest adRequest = new AdRequest.Builder()
                //.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();

        adView.loadAd(adRequest);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);


        al = new ArrayList<>();

       arrayAdapter = new ArrayAdapter<>(this, R.layout.item, R.id.name, al);

        SwipeFlingAdapterView flingContainer = (SwipeFlingAdapterView) findViewById(R.id.frame);

        final DocumentReference docRef = db.collection("Something").document("oneThing");
        docRef.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(final DocumentSnapshot documentSnapshot) {
                        Map<String, Object> map = documentSnapshot.getData();
                        if (documentSnapshot.exists()) {
                            for (int f = 1; f <= map.size(); f++) {
                                String title = documentSnapshot.getString(String.valueOf(f));
                                al.add(title);
                                arrayAdapter.notifyDataSetChanged();
                            } Collections.shuffle(al);

                        }
                    }
                });


        flingContainer.setAdapter(arrayAdapter);
        flingContainer.setFlingListener(new SwipeFlingAdapterView.onFlingListener() {
            @Override
            public void removeFirstObjectInAdapter() {
                // this is the simplest way to delete an object from the Adapter (/AdapterView)
                Log.d("LIST", "removed object!");
                al.remove(0);
                arrayAdapter.notifyDataSetChanged();
            }



            @Override
            public void onLeftCardExit(Object dataObject) {
                swipeDb.child(UID).child("SwipeLeft").setValue(true).toString();
                Map<String, Object> card = new HashMap<>();
                card.put(al.get(0), null);
                db.collection("SwipeLeft").document(UID)
                        .set(card, SetOptions.merge());
                //Do something on the left!
                //You also have access to the original object.
                //If you want to use it just cast it (String) dataObject
                Toast.makeText(MainActivity.this, "Not interested", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRightCardExit(Object dataObject) {
                Toast.makeText(MainActivity.this, "I'm going to do this!", Toast.LENGTH_SHORT).show();
                swipeDb.child(UID).child("SwipeRight").setValue(true).toString();
                Map<String, Object> card = new HashMap<>();
                card.put(al.get(0), null);
                db.collection("SwipeRight").document(UID)
                        .set(card, SetOptions.merge());
            }

            @Override
            public void onAdapterAboutToEmpty(int itemsInAdapter) {
                // Ask for more data here
                al.add("Do you know any fun ideas people should do? Send an email to themetimeapp@gmail.com");
                arrayAdapter.notifyDataSetChanged();
                i++;

        }

            @Override
            public void onScroll(float scrollProgressPercent) {
            }
        });

        // Optionally add an OnItemClickListener
        flingContainer.setOnItemClickListener(new SwipeFlingAdapterView.OnItemClickListener() {
            @Override
            public void onItemClicked(int itemPosition, Object dataObject) {
                Toast.makeText(MainActivity.this, "Dude, SWIPE left OR right", Toast.LENGTH_SHORT).show();
            }
        });
    }
}