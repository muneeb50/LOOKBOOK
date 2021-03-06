package com.htsm.lookbook.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.htsm.lookbook.Controllers.UserController;
import com.htsm.lookbook.Fragments.MapFragment;
import com.htsm.lookbook.R;

public class HomeActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener{

    private FragmentManager mFragmentManager;
    private Fragment mFragment;
    private NavigationView mNavigationView;
    private DrawerLayout mDrawerLayout;

    private TextView mUserName;

    private static final String FRAGMENT_EXTRA = "HomeActivity.Fragment";

    public static Intent newIntent(Context context, Integer fragmentId) {
        Intent i = new Intent(context, HomeActivity.class);
        i.putExtra(FRAGMENT_EXTRA, fragmentId == null ? -1 : fragmentId);
        return i;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(SignInActivity.newIntent(this));
            finish();
            return;
        }
        setContentView(R.layout.activity_nav_view);
        mDrawerLayout = findViewById(R.id.id_nav_drawer);
        mNavigationView = findViewById(R.id.id_nav_view);
        mUserName = mNavigationView.getHeaderView(0).findViewById(R.id.id_user_name);

        UserController userController = new UserController(this);
        mUserName.setText(userController.getUserInfoLocally().getName());

        mFragmentManager = getSupportFragmentManager();
        mNavigationView.setNavigationItemSelectedListener(this);

        if(getIntent().getExtras() != null) {
            int fragmentId = getIntent().getExtras().getInt(FRAGMENT_EXTRA);
            if(fragmentId != -1) {
                chooseNavItem(fragmentId);
            } else {
                setFragmentView(MapFragment.newInstance());
            }
        } else {
            setFragmentView(MapFragment.newInstance());
        }
    }

    public void setFragmentView(Fragment fragment) {
        mFragment = mFragmentManager.findFragmentById(R.id.nav_fragment_container);
        if(mFragment == null) {
            mFragment = fragment;
            mFragmentManager.beginTransaction()
                    .add(R.id.nav_fragment_container, fragment)
                    .commit();
        } else {
            mFragment = fragment;
            mFragmentManager.beginTransaction()
                    .replace(R.id.nav_fragment_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        chooseNavItem(item.getItemId());
        return true;
    }

    public void chooseNavItem(int id) {
        switch (id) {
            case R.id.menu_home:
                setFragmentView(MapFragment.newInstance());
                break;
            case R.id.menu_add_book:
                startActivity(AddBookActivity.newIntent(this, null, null));
                break;
            case R.id.menu_update_profile:
                startActivity(SignUpActivity.newIntent(this, true));
                break;
            case R.id.menu_manage_books:
                startActivity(ViewAllBooksActivity.newIntent(this, FirebaseAuth.getInstance().getCurrentUser().getUid()));
                break;
            case R.id.menu_logout:
                FirebaseAuth.getInstance().signOut();
                startActivity(SignInActivity.newIntent(this));
                finish();
                break;
            default:
                setFragmentView(MapFragment.newInstance());
        }
        mDrawerLayout.closeDrawers();
    }
}
