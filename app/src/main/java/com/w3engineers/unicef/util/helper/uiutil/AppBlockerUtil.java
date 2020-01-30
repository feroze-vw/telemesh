package com.w3engineers.unicef.util.helper.uiutil;

import android.app.Activity;
import android.databinding.DataBindingUtil;
import android.os.Process;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import com.w3engineers.ext.strom.util.helper.Toaster;
import com.w3engineers.unicef.telemesh.R;
import com.w3engineers.unicef.telemesh.data.helper.constants.Constants;
import com.w3engineers.unicef.telemesh.databinding.DialogAppBlockerBinding;
import com.w3engineers.unicef.telemesh.ui.main.MainActivity;
import com.w3engineers.unicef.util.helper.LanguageUtil;

public class AppBlockerUtil {

    public static void openAppBlockerDialog(Activity activity, String versionName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(false);

        LayoutInflater inflater = LayoutInflater.from(activity);
        DialogAppBlockerBinding binding = DataBindingUtil.inflate(inflater, R.layout.dialog_app_blocker, null, false);
        builder.setView(binding.getRoot());

        AlertDialog dialog = builder.create();

        String versionText = "Update are Available: " + versionName;
        binding.textViewVersion.setText(versionText);

        binding.textViewUpdate.setOnClickListener(v -> {

            if (!Constants.IS_DATA_ON) {
                Toaster.showShort(LanguageUtil.getString(R.string.no_internet_connection));
                return;
            }
            if (MainActivity.getInstance() != null) {
                dialog.dismiss();
                MainActivity.getInstance().checkPlayStoreAppUpdate(Constants.AppUpdateType.BLOCKER, "");
            }
        });

        binding.textViewCancel.setOnClickListener(v -> Process.killProcess(Process.myPid()));

        dialog.show();
    }
}