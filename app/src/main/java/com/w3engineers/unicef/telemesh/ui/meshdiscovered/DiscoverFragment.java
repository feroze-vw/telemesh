package com.w3engineers.unicef.telemesh.ui.meshdiscovered;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.w3engineers.ext.strom.application.ui.base.BaseFragment;
import com.w3engineers.unicef.telemesh.R;
import com.w3engineers.unicef.telemesh.data.helper.constants.Constants;
import com.w3engineers.unicef.telemesh.data.local.usertable.UserEntity;
import com.w3engineers.unicef.telemesh.data.provider.ServiceLocator;
import com.w3engineers.unicef.telemesh.databinding.FragmentDiscoverBinding;
import com.w3engineers.unicef.telemesh.ui.chat.ChatActivity;
import com.w3engineers.unicef.telemesh.ui.main.MainActivity;
import com.w3engineers.unicef.util.helper.LanguageUtil;
import com.w3engineers.unicef.util.helper.uiutil.UIHelper;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.internal.util.AppendOnlyLinkedArrayList;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class DiscoverFragment extends BaseFragment {

    private FragmentDiscoverBinding fragmentDiscoverBinding;
    @Nullable
    public DiscoverViewModel discoverViewModel;
    @Nullable
    public List<UserEntity> userEntityList;
    @Nullable
    public MenuItem mSearchItem;
    private String title;
    private boolean isLoaded = false;
    private SearchView mSearchView;
    private DiscoverAdapter meshContactAdapter;

    Handler loaderHandler = new Handler(Looper.getMainLooper());

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_discover;
    }

    @Override
    protected void startUI() {
        fragmentDiscoverBinding = (FragmentDiscoverBinding)
                getViewDataBinding();

        setHasOptionsMenu(true);
        controlEmptyLayout();
        setTitle(title);

        init();

        userDataOperation();

        openUserMessage();

        changeFavouriteStatus();

    }

    private void userDataOperation() {

        if (discoverViewModel != null) {

            discoverViewModel.startUserObserver();

            discoverViewModel.nearbyUsers.observe(this, userEntities -> {
                if (userEntities != null) {
                    getAdapter().submitList(userEntities);
                    userEntityList = userEntities;

                    isLoaded = false;

                    if (userEntityList != null && userEntityList.size() > 0) {
                        if (fragmentDiscoverBinding.emptyLayout.getVisibility() == View.VISIBLE) {
                            fragmentDiscoverBinding.emptyLayout.setVisibility(View.GONE);
                        }
                    }
                }
                if (mSearchItem != null)
                    searchViewControl(userEntities);
            });

            discoverViewModel.getGetFilteredList().observe(this, userEntities -> {

                setTitle(LanguageUtil.getString(R.string.title_discoverd_fragment));
                if (userEntities != null && userEntities.size() > 0) {
                    fragmentDiscoverBinding.notFoundView.setVisibility(View.GONE);
                    fragmentDiscoverBinding.emptyLayout.setVisibility(View.GONE);
                    //  getAdapter().clear();
                    meshContactAdapter.submitList(userEntities);
                    isLoaded = false;

                } else {
                    if (!isLoaded) {
                        fragmentDiscoverBinding.emptyLayout.setVisibility(View.VISIBLE);
                        //enableLoading();

                        isLoaded = true;
                        Runnable runnable = () -> {
                            fragmentDiscoverBinding.tvMessage.setText("No User Found");
                            enableEmpty();
                            fragmentDiscoverBinding.loadingView.setVisibility(View.GONE);

                        };
                        loaderHandler.postDelayed(runnable, Constants.AppConstant.LOADING_TIME_SHORT);
                    }
                }
            });

            discoverViewModel.backUserEntity.observe(this, userEntities -> {
                userEntityList = userEntities;
            });

        }
    }

    private void openUserMessage() {
        if (discoverViewModel != null) {
            discoverViewModel.openUserMessage().observe(this, userEntity -> {

                if (getActivity() != null) {
                    ((MainActivity) getActivity()).hideSearchBar();

                    Intent intent = new Intent(getActivity(), ChatActivity.class);
                    intent.putExtra(UserEntity.class.getName(), userEntity.meshId);
                    startActivity(intent);
                }

            });
        }
    }


    private void changeFavouriteStatus() {
        if (discoverViewModel != null) {
            discoverViewModel.changeFavourite().observe(this, userEntity -> {
                if (userEntity.getIsFavourite() == Constants.FavouriteStatus.UNFAVOURITE) {
                    discoverViewModel.updateFavouriteStatus(userEntity.getMeshId(), Constants.FavouriteStatus.FAVOURITE);
                } else if (userEntity.getIsFavourite() == Constants.FavouriteStatus.FAVOURITE) {
                    discoverViewModel.updateFavouriteStatus(userEntity.getMeshId(), Constants.FavouriteStatus.UNFAVOURITE);
                }

            });
        }
    }

/*    private void initSearchView(SearchView searchView) {

        getCompositeDisposable().add(UIHelper.fromSearchView(searchView)
                .debounce(1, TimeUnit.SECONDS, Schedulers.computation())
                .filter((AppendOnlyLinkedArrayList.NonThrowingPredicate<String>) s -> (s.length() > 1 || s.length() == 0))
                .distinctUntilChanged()*//*.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())*//*
                .subscribeWith(searchContacts()));

    }*/

/*    public DisposableObserver<String> searchContacts() {
        return new DisposableObserver<String>() {

            boolean isSearchStart = false;

            @Override
            public void onNext(String string) {

                if (!isSearchStart) {
                    //searchLoading();
                    isSearchStart = true;
                }

                if (discoverViewModel != null) {
                    Timber.d("Search query: %s", string);
                    discoverViewModel.startSearch(string, discoverViewModel.getCurrentUserList());
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.e("onError: %s", e.getMessage());
            }

            @Override
            public void onComplete() {
                Timber.e("onError: Complete");
            }
        };
    }*/

    public void searchContacts(String query) {
        if (discoverViewModel != null) {
            Timber.tag("SearchIssue").d("Discover page Search query: %s", query);
            discoverViewModel.startSearch(query, discoverViewModel.getCurrentUserList());
        }
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {

        if (getActivity() != null) {
            getActivity().getMenuInflater().inflate(R.menu.menu_search_contact, menu);

            mSearchItem = menu.findItem(R.id.action_search);
            // Resolve search option visibility problem when contact is appeared from starting point
 /*           searchViewControl(userEntityList);

            mSearchView = mSearchItem.getActionView().findViewById(R.id.search_view);


            // mSearchView = (SearchView) mSearchItem.getActionView();
            mSearchView.setQueryHint(getString(R.string.search));

            mSearchView.setIconified(true);

            ImageView searchClose = mSearchView.findViewById(android.support.v7.appcompat.R.id.search_close_btn);
            searchClose.setImageResource(R.mipmap.ic_cross_grey);

            // Getting EditText view from search view and change cursor color
            AutoCompleteTextView searchTextView = mSearchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
            try {
                // Fixed value for getting cursor drawable from Edit text or search view
                Field mCursorDrawableRes = TextView.class.getDeclaredField(getString(R.string.cursordrawable));
                mCursorDrawableRes.setAccessible(true);
                mCursorDrawableRes.set(searchTextView, R.drawable.search_cursor); //This sets the cursor resource ID to 0 or @null which will make it visible on white background
            } catch (Exception e) {
                e.printStackTrace();
            }

            searchCollapseListener(mSearchItem, mSearchView);

            initSearchView(mSearchView);*/
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_search) {
            if (getActivity() != null) {
                ((MainActivity) getActivity()).showSearchBar();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*private void searchCollapseListener(MenuItem searchItem, SearchView searchView) {
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                searchView.setBackgroundColor(getResources().getColor(R.color.white));
                searchView.setMaxWidth(Integer.MAX_VALUE);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                //searchView.setBackgroundColor(-1);
                return true;
            }
        });
    }*/

    private void searchViewControl(List<UserEntity> userEntities) {
        boolean isSearchVisible = userEntities != null && userEntities.size() > 0;
        mSearchItem.setVisible(isSearchVisible);

        if (isSearchVisible) {
            setTitle(LanguageUtil.getString(R.string.title_discoverd_fragment));
        }
    }

    private void controlEmptyLayout() {
        if (!Constants.IS_LOADING_ENABLE) {
            Handler handler = new Handler(Looper.getMainLooper());
            enableLoading();
            title = LanguageUtil.getString(R.string.discovering_users);

            Runnable runnable = () -> {
                if (fragmentDiscoverBinding.emptyLayout.getVisibility() == View.VISIBLE) {
                    try {
                        enableEmpty();
                        setTitle(LanguageUtil.getString(R.string.title_discoverd_fragment));
                    } catch (Exception e) { e.printStackTrace(); }
                }
            };

            Constants.IS_LOADING_ENABLE = true;
            handler.postDelayed(runnable, Constants.AppConstant.LOADING_TIME);
        } else {
            enableEmpty();
            title = LanguageUtil.getString(R.string.title_discoverd_fragment);
        }
    }

    private void enableLoading() {
        //fragmentMeshcontactBinding.loadingText.setText(getResources().getString(R.string.this_may_take_while));
        fragmentDiscoverBinding.notFoundView.setVisibility(View.GONE);
        fragmentDiscoverBinding.loadingView.setVisibility(View.VISIBLE);
        fragmentDiscoverBinding.rippleBackground.startRippleAnimation();

        /*if (getActivity() != null) {
            ((MainActivity) getActivity()).enableLoading();
        }*/
    }

    public void enableEmpty() {
        fragmentDiscoverBinding.notFoundView.setVisibility(View.VISIBLE);
        fragmentDiscoverBinding.loadingView.setVisibility(View.GONE);
        fragmentDiscoverBinding.rippleBackground.stopRippleAnimation();

        /*if (getActivity() != null) {
            ((MainActivity) getActivity()).disableLoading();
        }*/
    }

    /*protected void searchLoading() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                //fragmentMeshcontactBinding.loadingText.setText(getResources().getString(R.string.searching));
                fragmentDiscoverBinding.notFoundView.setVisibility(View.GONE);
                fragmentDiscoverBinding.loadingView.setVisibility(View.VISIBLE);
                fragmentDiscoverBinding.rippleBackground.startRippleAnimation();

                // ((MainActivity) getActivity()).enableLoading();
            });
        }
    }*/

    // General API's and initialization area
    private void init() {
        initAllText();
        discoverViewModel = getViewModel();

        fragmentDiscoverBinding.contactRecyclerView.setItemAnimator(null);
        //   fragmentDiscoverBinding.contactRecyclerView.setHasFixedSize(true);
        fragmentDiscoverBinding.contactRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    //    ((SimpleItemAnimator)fragmentDiscoverBinding.contactRecyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

    //    fragmentDiscoverBinding.contactRecyclerView.setItemAnimator(null);

        meshContactAdapter = new DiscoverAdapter(discoverViewModel);
        fragmentDiscoverBinding.contactRecyclerView.setAdapter(meshContactAdapter);
    }

    private DiscoverAdapter getAdapter() {
        return (DiscoverAdapter) fragmentDiscoverBinding
                .contactRecyclerView.getAdapter();
    }

    private DiscoverViewModel getViewModel() {
        return ViewModelProviders.of(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) ServiceLocator.getInstance().getDiscoveViewModel(getActivity().getApplication());
            }
        }).get(DiscoverViewModel.class);
    }

    private void initAllText() {
        fragmentDiscoverBinding.tvMessage.setText(LanguageUtil.getString(R.string.no_contact_available));
        fragmentDiscoverBinding.textViewSearching.setText(LanguageUtil.getString(R.string.searching));
    }
}
