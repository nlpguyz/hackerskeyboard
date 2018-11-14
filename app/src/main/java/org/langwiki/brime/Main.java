/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.langwiki.brime;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v13.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.BufferType;

import org.langwiki.brime.schema.SchemaManager;

import static android.support.v4.app.ActivityCompat.startActivityForResult;

public class Main extends AppCompatActivity {

    private final static String MARKET_URI = "market://search?q=pub:\"Klaus Weidner\"";
    private SchemaManager mSchemaManager;

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(IMEConfig.TAG,"Permission is granted");
                return true;
            } else {
                Log.v(IMEConfig.TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[] {
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(IMEConfig.TAG,"Permission is granted");
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0]== PackageManager.PERMISSION_GRANTED){
            Log.v(IMEConfig.TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
            //resume tasks needing this permission
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isStoragePermissionGranted();
        isOverlayPermissionGranted();

        setContentView(R.layout.main);
        String html = getString(R.string.main_body);
        html += "<p><i>Version: " + getString(R.string.auto_version) + "</i></p>";
        Spanned content = Html.fromHtml(html);
        TextView description = findViewById(R.id.main_description);
        description.setMovementMethod(LinkMovementMethod.getInstance());
        description.setText(content, BufferType.SPANNABLE);


        final Button setup1 = findViewById(R.id.main_setup_btn_configure_imes);
        setup1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS), 0);
            }
        });

        final Button setup2 = findViewById(R.id.main_setup_btn_set_ime);
        setup2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                mgr.showInputMethodPicker();
            }
        });
        
        final AppCompatActivity that = this;

        final Button setup4 = findViewById(R.id.main_setup_btn_input_lang);
        setup4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(new Intent(that, InputLanguageSelection.class), 0);
            }
        });

        LatinIME.setContext(this);
        final Button setup5 = findViewById(R.id.main_setup_btn_setup_rime);
        setup5.setOnClickListener(new View.OnClickListener() {
                                      public void onClick(View v) {
                                          startActivityForResult(new Intent(that, RimePreference.class), 0);
                                      }
                                  });

        mSchemaManager = SchemaManager.getInstance();
        mSchemaManager.redeploy(this, true, true);

        /*
        final Button setup3 = (Button) findViewById(R.id.main_setup_btn_get_dicts);
        setup3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent it = new Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_URI));
                try {
                	startActivity(it);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(
                            		R.string.no_market_warning), Toast.LENGTH_LONG)
                            .show();
                }
            }
        }); */
        // PluginManager.getPluginDictionaries(getApplicationContext()); // why?
    }

    private void isOverlayPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 1234);
            }
        }
    }
}

