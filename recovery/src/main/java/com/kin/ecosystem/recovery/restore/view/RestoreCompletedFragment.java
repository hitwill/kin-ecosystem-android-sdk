package com.kin.ecosystem.recovery.restore.view;


import static com.kin.ecosystem.recovery.restore.presenter.RestorePresenterImpl.KEY_ACCOUNT_INDEX;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import com.kin.ecosystem.recovery.R;
import com.kin.ecosystem.recovery.base.BaseToolbarActivity;
import com.kin.ecosystem.recovery.restore.presenter.RestoreCompletedPresenter;
import com.kin.ecosystem.recovery.restore.presenter.RestoreCompletedPresenterImpl;


public class RestoreCompletedFragment extends Fragment implements RestoreCompletedView {

	private RestoreCompletedPresenter presenter;

	public static RestoreCompletedFragment newInstance(Integer accountIndex) {
		RestoreCompletedFragment fragment = new RestoreCompletedFragment();
		if (accountIndex != -1) {
			Bundle bundle = new Bundle();
			bundle.putInt(KEY_ACCOUNT_INDEX, accountIndex);
			fragment.setArguments(bundle);
		}
		return fragment;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
		@Nullable Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.kinrecovery_fragment_restore_completed, container, false);

		int accountIndex = extractAccountIndex(savedInstanceState);
		injectPresenter(accountIndex);
		presenter.onAttach(this, ((RestoreActivity) getActivity()).getPresenter());

		initToolbar();
		initViews(root);
		return root;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		presenter.onSaveInstanceState(outState);
		super.onSaveInstanceState(outState);
	}

	private int extractAccountIndex(@Nullable Bundle savedInstanceState) {
		Bundle bundle = savedInstanceState != null ? savedInstanceState : getArguments();
		if (bundle == null) {
			throw new IllegalStateException("Bundle is null, can't extract required accountIndex data.");
		}
		int accountIndex = bundle.getInt(KEY_ACCOUNT_INDEX, -1);
		if (accountIndex == -1) {
			throw new IllegalStateException("Can't find accountIndex data inside Bundle.");
		}
		return accountIndex;
	}

	private void injectPresenter(int accountIndex) {
		presenter = new RestoreCompletedPresenterImpl(accountIndex);
	}

	private void initToolbar() {
		BaseToolbarActivity toolbarActivity = (BaseToolbarActivity) getActivity();
		toolbarActivity.setNavigationIcon(R.drawable.kinrecovery_ic_back_black);
		toolbarActivity.setToolbarColor(R.color.kinrecovery_white);
		toolbarActivity.setToolbarTitle(R.string.kinrecovery_restore_completed_title);
		toolbarActivity.setNavigationClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				presenter.onBackClicked();
			}
		});
	}

	private void initViews(View root) {
		root.findViewById(R.id.kinrecovery_v_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				presenter.close();
			}
		});
	}

}
