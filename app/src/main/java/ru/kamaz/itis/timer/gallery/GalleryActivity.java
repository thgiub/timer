package ru.kamaz.itis.timer.gallery;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;


//import androidx.appcompat.app.AppCompatActivity;
//
//import androidx.fragment.app.Fragment;
//import androidx.fragment.app.FragmentTransaction;




import ru.kamaz.itis.timer.R;
import ru.kamaz.itis.timer.gallery.ui.ui.main.MediaPhotoPagerFragment;

public class GalleryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Fragment fragment =MediaPhotoPagerFragment.newInstance();
        transaction.replace(R.id.gobedia2, fragment);

    }


}
