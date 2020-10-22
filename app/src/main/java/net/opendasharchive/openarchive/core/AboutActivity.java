package net.opendasharchive.openarchive.core;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import net.opendasharchive.openarchive.R;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ((TextView)findViewById(R.id.about_view)).setMovementMethod(LinkMovementMethod.getInstance());

    }



}
