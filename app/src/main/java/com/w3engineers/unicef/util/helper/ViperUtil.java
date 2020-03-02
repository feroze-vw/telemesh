package com.w3engineers.unicef.util.helper;
 
/*
============================================================================
Copyright (C) 2019 W3 Engineers Ltd. - All Rights Reserved.
Unauthorized copying of this file, via any medium is strictly prohibited
Proprietary and confidential
============================================================================
*/

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;
import com.w3engineers.ext.strom.util.helper.data.local.SharedPref;
import com.w3engineers.mesh.application.data.ApiEvent;
import com.w3engineers.mesh.application.data.AppDataObserver;
import com.w3engineers.mesh.application.data.local.DataPlanConstants;
import com.w3engineers.mesh.application.data.local.dataplan.DataPlanManager;
import com.w3engineers.mesh.application.data.model.ConfigSyncEvent;
import com.w3engineers.mesh.application.data.model.DataAckEvent;
import com.w3engineers.mesh.application.data.model.DataEvent;
import com.w3engineers.mesh.application.data.model.PeerRemoved;
import com.w3engineers.mesh.application.data.model.PermissionInterruptionEvent;
import com.w3engineers.mesh.application.data.model.ServiceUpdate;
import com.w3engineers.mesh.application.data.model.TransportInit;
import com.w3engineers.mesh.application.data.model.UserInfoEvent;
import com.w3engineers.mesh.application.data.model.WalletLoaded;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.DialogUtil;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.lib.mesh.DataManager;
import com.w3engineers.mesh.util.lib.mesh.HandlerUtil;
import com.w3engineers.mesh.util.lib.mesh.ViperClient;
import com.w3engineers.models.ConfigurationCommand;
import com.w3engineers.models.PointGuideLine;
import com.w3engineers.unicef.TeleMeshApplication;
import com.w3engineers.unicef.telemesh.BuildConfig;
import com.w3engineers.unicef.telemesh.R;
import com.w3engineers.unicef.telemesh.data.helper.AppCredentials;
import com.w3engineers.unicef.telemesh.data.helper.constants.Constants;
import com.w3engineers.unicef.telemesh.data.local.usertable.UserModel;
import com.w3engineers.unicef.telemesh.ui.main.MainActivity;
import com.w3engineers.unicef.util.helper.model.MeshControlConfig;
import com.w3engineers.unicef.util.helper.model.ViperData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class ViperUtil {

    private ViperClient viperClient;
    private String myUserId;
    private Context context;
    private String DEVICE_NAME = "xiaomi";

    protected ViperUtil(UserModel userModel) {
        try {
            context = MainActivity.getInstance() != null ? MainActivity.getInstance() : TeleMeshApplication.getContext();
            String appName = context.getResources().getString(R.string.app_name);


            String AUTH_USER_NAME = AppCredentials.getInstance().getAuthUserName();
            String AUTH_PASSWORD = AppCredentials.getInstance().getAuthPassword();
            String FILE_REPO_LINK = AppCredentials.getInstance().getFileRepoLink();
            String PARSE_APP_ID = AppCredentials.getInstance().getParseAppId();
            String PARSE_URL = AppCredentials.getInstance().getParseUrl();
            String CONFIG_DATA = AppCredentials.getInstance().getConfiguration();
            String SIGNAL_SERVER_URL = AppCredentials.getInstance().getSignalServerUrl();


            SharedPref sharedPref = SharedPref.getSharedPref(context);
            String address = sharedPref.read(Constants.preferenceKey.MY_WALLET_ADDRESS);
            String publicKey = sharedPref.read(Constants.preferenceKey.MY_PUBLIC_KEY);
            String networkSSID = sharedPref.read(Constants.preferenceKey.NETWORK_PREFIX);

            initObservers();

            if (TextUtils.isEmpty(networkSSID)) {
                networkSSID = context.getResources().getString(R.string.def_ssid);
            }

            /*MeshControlConfig meshControlConfig = new MeshControlConfig().setAppDownloadEnable(true)
                    .setMessageEnable(true).setDiscoveryEnable(true).setBlockChainEnable(true);

            String meshControlConfigData = new Gson().toJson(meshControlConfig);*/

            viperClient = ViperClient.on(context, context.getPackageName(), userModel.getName(),
                    address, publicKey, userModel.getImage(), userModel.getTime(), true, CONFIG_DATA)
                    .setConfig(AUTH_USER_NAME, AUTH_PASSWORD, FILE_REPO_LINK, PARSE_URL, PARSE_APP_ID, SIGNAL_SERVER_URL, BuildConfig.VERSION_CODE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initObservers() {

        AppDataObserver.on().startObserver(ApiEvent.TRANSPORT_INIT, event -> {
            TransportInit transportInit = (TransportInit) event;

            if (transportInit.success) {
                myUserId = transportInit.nodeId;

                onMesh(myUserId);
            }
        });

        AppDataObserver.on().startObserver(ApiEvent.WALLET_LOADED, event -> {
            WalletLoaded walletLoaded = (WalletLoaded) event;

            if (walletLoaded.success) {
                myUserId = walletLoaded.walletAddress;
                onMeshPrepared(walletLoaded.walletAddress);
            }
        });

       /* AppDataObserver.on().startObserver(ApiEvent.PEER_ADD, event -> {
            PeerAdd peerAdd = (PeerAdd) event;
            peerDiscoveryProcess(peerAdd.peerId, true);
        });*/

        AppDataObserver.on().startObserver(ApiEvent.PEER_REMOVED, event -> {
            PeerRemoved peerRemoved = (PeerRemoved) event;
            peerDiscoveryProcess(peerRemoved.peerId, false);
        });

        AppDataObserver.on().startObserver(ApiEvent.DATA, event -> {

            DataEvent dataEvent = (DataEvent) event;

            dataReceive(dataEvent.peerId, dataEvent.data);
        });

        AppDataObserver.on().startObserver(ApiEvent.DATA_ACKNOWLEDGEMENT, event -> {

            DataAckEvent dataAckEvent = (DataAckEvent) event;

            onAck(dataAckEvent.dataId, dataAckEvent.status);

        });

        AppDataObserver.on().startObserver(ApiEvent.USER_INFO, event -> {

            UserInfoEvent userInfoEvent = (UserInfoEvent) event;

            UserModel userModel = new UserModel().setName(userInfoEvent.getUserName())
                    .setImage(userInfoEvent.getAvatar())
                    .setTime(userInfoEvent.getRegTime())
                    .setConfigVersion(userInfoEvent.getConfigVersion());

            peerAdd(userInfoEvent.getAddress(), userModel);
        });

        AppDataObserver.on().startObserver(ApiEvent.CONFIG_SYNC, event -> {

            ConfigSyncEvent configSyncEvent = (ConfigSyncEvent) event;

            if (configSyncEvent != null) {
                configSync(configSyncEvent.isUpdate(), configSyncEvent.getConfigurationCommand());
            }
        });

        AppDataObserver.on().startObserver(ApiEvent.SERVICE_UPDATE, event -> {


            ServiceUpdate serviceUpdate = (ServiceUpdate) event;
            MeshLog.v("SERVICE_UPDATE  " + serviceUpdate.isNeeded);
            if (serviceUpdate.isNeeded) {
                showServiceUpdateAvailable(MainActivity.getInstance());
            }

        });

        AppDataObserver.on().startObserver(ApiEvent.PERMISSION_INTERRUPTION, event -> {

            Log.v("MIMO_SAHA::", "Permission<><> 2");
            PermissionInterruptionEvent permissionInterruptionEvent = (PermissionInterruptionEvent) event;
            if (permissionInterruptionEvent != null) {
                HandlerUtil.postForeground(() -> showPermissionEventAlert(permissionInterruptionEvent.hardwareState, permissionInterruptionEvent.permissions, MainActivity.getInstance()));
            }
        });

    }

    public void showPermissionEventAlert(int hardwareEvent, List<String> permissions, Activity activity) {

        if (activity == null) return;
        android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        @SuppressLint("InflateParams")
        View dialogView = inflater.inflate(R.layout.alert_hardware_permission, null);
        dialogBuilder.setView(dialogView);

        android.app.AlertDialog alertDialog = dialogBuilder.create();

        TextView title = dialogView.findViewById(R.id.interruption_title);
        TextView message = dialogView.findViewById(R.id.interruption_message);
        Button okay = dialogView.findViewById(R.id.okay_button);

        String finalTitle = "", finalMessage = "";

        boolean isPermission = false;

        if (permissions == null || permissions.isEmpty()) {

            String event = "";

            if (hardwareEvent == DataPlanConstants.INTERRUPTION_EVENT.USER_DISABLED_BT) {
                event = "Bluetooth";
            } else if (hardwareEvent == DataPlanConstants.INTERRUPTION_EVENT.USER_DISABLED_WIFI) {
                event = "Wifi";
            } else if (hardwareEvent == DataPlanConstants.INTERRUPTION_EVENT.LOCATION_PROVIDER_OFF) {
                event = "Location ";
            }

            if (!TextUtils.isEmpty(event)) {
                finalMessage = String.format(activity.getResources().getString(R.string.hardware_interruption), event);
                finalTitle = String.format(activity.getResources().getString(R.string.interruption_title), "Hardware");
            }

        } else {

            String event = "";
            for (String permission : permissions) {
                if (!TextUtils.isEmpty(permission)) {
                    if (permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        event = "Location";
                    } else if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        event = "Storage";
                    }
                }
            }

            if (!TextUtils.isEmpty(event)) {
                finalMessage = String.format(activity.getResources().getString(R.string.permission_interruption), event);
                finalTitle = String.format(activity.getResources().getString(R.string.interruption_title), "Permission");
            }

            isPermission = true;

        }

        boolean finalIsPermission = isPermission;
        okay.setOnClickListener(v -> {
            if (isPermissionNeeded(DEVICE_NAME)) {
                showPermissionPopupForXiaomi(MainActivity.getInstance());
            } else if (finalIsPermission) {
                DataManager.on().allowMissingPermission(permissions);
                alertDialog.dismiss();
            } else {
                alertDialog.dismiss();
            }
        });

        if (!TextUtils.isEmpty(finalTitle) && !TextUtils.isEmpty(finalMessage)) {
            title.setText(finalTitle);
            message.setText(finalMessage);

            alertDialog.setCancelable(false);
            alertDialog.show();
        }
    }

    private void peerDiscoveryProcess(String nodeId, boolean isActive) {
        HandlerUtil.postBackground(() -> {

            int userConnectivityStatus = isActive ? getUserActiveStatus(nodeId) : 0;
            boolean isUserExist = isNodeAvailable(nodeId, userConnectivityStatus);

            if (!isUserExist) {
                if (isActive) {
//                    pingedNodeId(nodeId);
                } else {
                    peerRemove(nodeId);
                }
            }
        });
    }

    /*********************Ping*************************/

    /*private void pingedNodeId(String nodeId) {
        if (!TextUtils.isEmpty(nodeId) && nodeId.equals(myUserId))
            return;

        sendProfilePing(nodeId);
    }*/

    /*private void sendProfilePing(String nodeId) {
        ViperData viperData = ViperDataProcessor.getInstance().getPingForProfile();

        if (viperData != null) {
            String sendId = UUID.randomUUID().toString();
            sendDataToMesh(nodeId, viperData, sendId);
        }
    }*/

    /*********************Ping*************************/

    private void dataReceive(String senderId, byte[] frameData) {
        if (frameData != null) {

            ViperData viperData = ViperDataProcessor.getInstance().setDataFormatFromJson(frameData);

            if (viperData != null) {

                if (viperData.rawData != null) {
                    onData(senderId, viperData);
                }

                /*if (ViperDataProcessor.getInstance().isProfilePing(viperData)) {

                    myProfileSend(senderId);

                } else if (ViperDataProcessor.getInstance().isProfileData(viperData)) {

                    peerAdd(senderId, frameData);

                } else {

                    if (viperData.rawData != null) {
                        onData(senderId, viperData);
                    }
                }*/
            }
        }
    }

    /*private void myProfileSend(String nodeId) {

        if (!TextUtils.isEmpty(nodeId) && nodeId.equals(myUserId))
            return;

        HandlerUtil.postBackground(() -> {
            sendMyInfo(nodeId);
        });
    }*/

    /**
     * Send my info after discovering him
     *
     * @param nodeId - The discovered node id
     */
    /*private void sendMyInfo(String nodeId) {

        ViperData viperData = ViperDataProcessor.getInstance().getMyProfileMeshData();

        if (viperData != null) {
            String sendId = UUID.randomUUID().toString();
            sendDataToMesh(nodeId, viperData, sendId);
        }
    }*/
    private void sendDataToMesh(String nodeId, ViperData viperData, String sendId) {
        byte[] data = ViperDataProcessor.getInstance().getDataFormatToJson(viperData);

        boolean isNotificationEnable = viperData.isNotificationEnable;

        try {
            viperClient.sendMessage(myUserId, nodeId, sendId, data, isNotificationEnable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getUserActiveStatus(String nodeId) {
        try {
            return viperClient.getLinkTypeById(nodeId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public String sendMeshData(String peerId, ViperData viperData) {
        if (viperData != null) {
            String sendId = UUID.randomUUID().toString();
            sendDataToMesh(peerId, viperData, sendId);
            return sendId;
        }
        return null;
    }

    public List<String> getAllSellers() {
        List<String> allInternetSellers = null;
        try {
            allInternetSellers = viperClient.getInternetSellers();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (allInternetSellers == null || allInternetSellers.size() == 0) {
            return new ArrayList<>();
        } else {
            return allInternetSellers;
        }
    }

    public void stopMeshService() {
        try {
            viperClient.stopMesh();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void restartMeshService() {
        try {
            int myCurrentRole = DataPlanManager.getInstance().getDataPlanRole();
            viperClient.restartMesh(myCurrentRole);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // TODO SSID_Change
    /*public void destroyMeshService() {
        if (viperClient != null) {
            viperClient.destroyMeshService();
        }
    }

    public void resetViperInstance() {
        if (viperClient != null) {
            viperClient.resetViperInstance();
        }
    }*/

    public void sendConfigToViper(ConfigurationCommand configurationCommand) {
        if (configurationCommand != null) {
            if (viperClient != null) {
                viperClient.sendConfigForUpdate(configurationCommand);
            }
        }
    }

    public void saveUserInfo(UserModel userModel) {

        try {
            SharedPref sharedPref = SharedPref.getSharedPref(context);

            String address = sharedPref.read(Constants.preferenceKey.
                    MY_WALLET_ADDRESS);
            String publicKey = sharedPref.read(Constants.preferenceKey.MY_PUBLIC_KEY);

            viperClient.saveUserInfo(address, userModel.getImage(), userModel.getTime(), true,
                    userModel.getName(), publicKey, "com.w3engineers.unicef.telemesh");
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*if (viperClient != null) {
            SharedPref sharedPref = SharedPref.getSharedPref(context);

            String address = sharedPref.read(Constants.preferenceKey.
                    MY_WALLET_ADDRESS);
            String publicKey = sharedPref.read(Constants.preferenceKey.MY_PUBLIC_KEY);

            viperClient.saveUserInfo(address, userModel.getImage(), userModel.getTime(), true,
                    userModel.getName(), publicKey, "com.w3engineers.unicef.telemesh");
        }*/
    }

    public void saveOtherUserInfo(UserModel userModel) {

        if (viperClient != null) {
            viperClient.saveOtherUserInfo(userModel.getName(), userModel.getImage(), userModel.getUserId(), context.getPackageName());
        }
    }


    public PointGuideLine requestTokenGuideline() {
        if (viperClient != null) {
            return viperClient.requestPointGuideline();
        }
        return null;
    }

    public void sendTokenGuidelineInfoToViper(String guideLine) {
        if (guideLine != null && viperClient != null) {
            viperClient.sendPointGuidelineForUpdate(guideLine);
        }
    }

    protected void checkUserConnectionStatus(String userId) {
        try {
            viperClient.checkConnectionStatus(userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showServiceUpdateAvailable(Activity activity) {
        if (activity == null) return;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setCancelable(false);
                builder.setTitle(Html.fromHtml("<b>" + activity.getString(R.string.service_app_alert_title_text) + "</b>"));
                builder.setMessage(activity.getString(R.string.service_app_update_message));
                builder.setPositiveButton(Html.fromHtml("<b>" + activity.getString(R.string.button_postivive) + "<b>"), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg1) {

                        Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.w3engineers.meshservice");
                        if (intent != null) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        }
                    }
                });

                builder.setNegativeButton(Html.fromHtml("<b>" + activity.getString(R.string.button_later) + "<b>"), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg1) {

                    }
                });

                builder.setCancelable(false);
                builder.create();
                builder.show();
            }
        });

    }

    public boolean isPermissionNeeded(String deviceName) {
        String manufacturer = android.os.Build.MANUFACTURER;
        boolean isPermissionNeeded = false;
        try {

            if (deviceName.equalsIgnoreCase(manufacturer)) {
                isPermissionNeeded = !SharedPref.getSharedPref(context).readBoolean(Constants.preferenceKey.IS_SETTINGS_PERMISSION_DONE);
            }
        } catch (Exception e) { e.printStackTrace(); }

        return isPermissionNeeded;
    }

    public void showPermissionPopupForXiaomi(Activity activity) {
        if (activity == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(false);
        builder.setTitle(Html.fromHtml("<b>" + "<font color='#FF7F27'>Please allow permissions</font>" + "</b>"));
        builder.setMessage(activity.getString(R.string.permission_xiomi));
        builder.setPositiveButton(Html.fromHtml("<b>" + activity.getString(com.w3engineers.mesh.R.string.ok) + "<b>"), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                SharedPref.getSharedPref(context).write(Constants.preferenceKey.IS_SETTINGS_PERMISSION_DONE, true);
                activity.startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 100);
            }
        });
        builder.create();
        builder.show();
    }

    ////////////////////////////////////////////////////////////////////////////////////

    protected abstract void onMesh(String myMeshId);

    protected abstract void onMeshPrepared(String myWalletAddress);

    protected abstract void peerAdd(String peerId, byte[] peerData);

    protected abstract void peerAdd(String peerId, UserModel userModel);

    protected abstract void peerRemove(String nodeId);

    protected abstract void onData(String peerId, ViperData viperData);

    protected abstract void onAck(String messageId, int status);

    protected abstract boolean isNodeAvailable(String nodeId, int userActiveStatus);

    protected abstract void configSync(boolean isUpdate, ConfigurationCommand configurationCommand);



    /*private String loadJSONFromAsset(Context context) {
        String json = null;
        try {
            InputStream is = context.getAssets().open("config.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;

    }*/
}
