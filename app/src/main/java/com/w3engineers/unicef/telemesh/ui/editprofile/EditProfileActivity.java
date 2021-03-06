package com.w3engineers.unicef.telemesh.ui.editprofile;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.w3engineers.ext.strom.util.helper.Toaster;
import com.w3engineers.ext.strom.util.helper.data.local.SharedPref;
import com.w3engineers.mesh.application.data.BaseServiceLocator;
import com.w3engineers.mesh.application.ui.base.TelemeshBaseActivity;
import com.w3engineers.unicef.telemesh.R;
import com.w3engineers.unicef.telemesh.data.helper.constants.Constants;
import com.w3engineers.unicef.telemesh.data.local.usertable.UserEntity;
import com.w3engineers.unicef.telemesh.data.provider.ServiceLocator;
import com.w3engineers.unicef.telemesh.databinding.ActivityEditProfileBinding;
import com.w3engineers.unicef.telemesh.ui.chooseprofileimage.ProfileImageActivity;
import com.w3engineers.unicef.telemesh.ui.createuser.CreateUserActivity;
import com.w3engineers.unicef.util.helper.CommonUtil;
import com.w3engineers.unicef.util.helper.LanguageUtil;
import com.w3engineers.unicef.util.helper.uiutil.UIHelper;

public class EditProfileActivity extends TelemeshBaseActivity {

    private ActivityEditProfileBinding mBinding;
    private EditProfileViewModel mViewModel;
    private int PROFILE_IMAGE_REQUEST = 1;

    public static int INITIAL_IMAGE_INDEX = -1;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_edit_profile;
    }

    @Override
    protected int statusBarColor() {
        return R.color.colorPrimaryDark;
    }

    @Override
    public BaseServiceLocator a() {
        return null;
    }

    @Override
    public void startUI() {
        super.startUI();
        mBinding = (ActivityEditProfileBinding) getViewDataBinding();
        mViewModel = getViewModel();

        initAllText();

        setClickListener(mBinding.imageViewBack, mBinding.buttonUpdate, mBinding.imageProfile, mBinding.imageViewCamera);

        UserEntity userEntity = getIntent().getParcelableExtra(UserEntity.class.getName());
        mBinding.setUser(userEntity);

        mViewModel.textChangeLiveData.observe(this, this::nextButtonControl);
        mViewModel.textEditControl(mBinding.editTextName);

        mBinding.editTextName.setSelection(mBinding.editTextName.getText().toString().length());
    }

    @Override
    public void onClick(@NonNull View view) {
        super.onClick(view);

        int id = view.getId();
        switch (id) {
            case R.id.button_update:
                goNext();
                break;
            case R.id.image_profile:
            case R.id.image_view_camera:
                UIHelper.hideKeyboardFrom(this, mBinding.editTextName);
                Intent intent = new Intent(this, ProfileImageActivity.class);
                int currentImageIndex = mViewModel.getImageIndex();

                SharedPref sharedPref = SharedPref.getSharedPref(this);
                int oldImageIndex = sharedPref.readInt(Constants.preferenceKey.IMAGE_INDEX);
                if (currentImageIndex < 0) {
                    currentImageIndex = oldImageIndex;
                }

                intent.putExtra(CreateUserActivity.IMAGE_POSITION, currentImageIndex);
                startActivityForResult(intent, PROFILE_IMAGE_REQUEST);
                break;
            case R.id.image_view_back:
                finish();
                break;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null && requestCode == PROFILE_IMAGE_REQUEST && resultCode == RESULT_OK) {
            mViewModel.setImageIndex(data.getIntExtra(CreateUserActivity.IMAGE_POSITION, INITIAL_IMAGE_INDEX));

            int id = getResources().getIdentifier(Constants.drawables.AVATAR_IMAGE + mViewModel.getImageIndex(), Constants.drawables.AVATAR_DRAWABLE_DIRECTORY, getPackageName());
            mBinding.imageProfile.setImageResource(id);

            nextButtonControl(mBinding.editTextName.getText().toString());
        }
    }

    private void nextButtonControl(String nameText) {
        if (!TextUtils.isEmpty(nameText) &&
                nameText.length() >= Constants.DefaultValue.MINIMUM_TEXT_LIMIT) {

            mBinding.buttonUpdate.setBackgroundResource(R.drawable.ractangular_gradient);
            mBinding.buttonUpdate.setTextColor(getResources().getColor(R.color.white));
        } else {

            mBinding.buttonUpdate.setBackgroundResource(R.drawable.ractangular_white);
            mBinding.buttonUpdate.setTextColor(getResources().getColor(R.color.new_user_button_color));
        }
    }

    public void goNext() {
        UIHelper.hideKeyboardFrom(this, mBinding.editTextName);

        if (CommonUtil.isValidName(mBinding.editTextName.getText().toString(), this)) {
            if (isNeedToUpdate()) {
                if (mViewModel.storeData(mBinding.editTextName.getText() + "")) {
                    Toaster.showShort(LanguageUtil.getString(R.string.profile_updated_successfully));
                    mViewModel.sendUserInfoToAll();
                    finish();
                }
            } else {
                finish();
            }
        }

        /*if (TextUtils.isEmpty(mBinding.editTextName.getText())) {
            Toaster.showShort(LanguageUtil.getString(R.string.please_enter_your_name));
        } else if (mBinding.editTextName.getText().toString().length() < 2) {
            Toaster.showShort(LanguageUtil.getString(R.string.enter_valid_name));
        } else if (mViewModel.storeData(mBinding.editTextName.getText() + "")) {
            Toaster.showShort(LanguageUtil.getString(R.string.profile_updated_successfully));
            mViewModel.sendUserInfoToAll();
            finish();
        }*/
    }

    private boolean isNeedToUpdate() {
        SharedPref sharedPref = SharedPref.getSharedPref(this);
        String oldName = sharedPref.read(Constants.preferenceKey.USER_NAME);
        int oldImageIndex = sharedPref.readInt(Constants.preferenceKey.IMAGE_INDEX);

        int currentImageIndex = mViewModel.getImageIndex();

        if (currentImageIndex < 0) {
            currentImageIndex = oldImageIndex;
        }

        if (!oldName.equals(mBinding.editTextName.getText().toString().trim()) || (currentImageIndex != oldImageIndex)) {
            return true;
        }

        return false;
    }


    private EditProfileViewModel getViewModel() {
        return ViewModelProviders.of(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) ServiceLocator.getInstance().getEditProfileViewModel(getApplication());
            }
        }).get(EditProfileViewModel.class);
    }

    private void initAllText() {
        mBinding.textViewCreateProfile.setText(LanguageUtil.getString(R.string.update_profile));
        mBinding.buttonUpdate.setText(LanguageUtil.getString(R.string.update));
        mBinding.editTextName.setHint(LanguageUtil.getString(R.string.enter_first_name));
        mBinding.nameLayout.setHint(LanguageUtil.getString(R.string.enter_first_name));
    }
}
