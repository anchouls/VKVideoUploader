package com.example.vkvideouploader;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.vkvideouploader.ui.loading.LoadingFragment;
import com.example.vkvideouploader.ui.uploaded.UploadedFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;


public class UserActivity extends AppCompatActivity {

    FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        BottomNavigationView bottomNavigation = findViewById(R.id.navigation_view);
        bottomNavigation.setOnNavigationItemSelectedListener(navigationListener);

        fragmentManager = getSupportFragmentManager();


        Fragment uploaded = new UploadedFragment();
        Fragment loading = new LoadingFragment();
        fragmentManager.beginTransaction()
                .add(R.id.fragment_container, uploaded, "fragA")
                .add(R.id.fragment_container, loading, "fragB")
                .hide(loading)
                .show(uploaded)
                .commit();
    }

    @SuppressLint("NonConstantResourceId")
    private final BottomNavigationView.OnNavigationItemSelectedListener navigationListener =
            item -> {
                Fragment selectedFragment = null;
                Fragment hideFragment = null;
                switch (item.getItemId()) {
                    case R.id.navigation_uploaded:
                        selectedFragment = fragmentManager.findFragmentByTag("fragA");
                        hideFragment = fragmentManager.findFragmentByTag("fragB");
                        break;
                    case R.id.navigation_loading:
                        selectedFragment = fragmentManager.findFragmentByTag("fragB");
                        hideFragment = fragmentManager.findFragmentByTag("fragA");
                        break;
                }

                fragmentManager.beginTransaction()
                        .hide(hideFragment)
                        .show(selectedFragment)
                        .commit();

                return true;
            };

}
