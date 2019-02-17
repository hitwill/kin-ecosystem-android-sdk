package com.kin.ecosystem.main.presenter;


import static com.kin.ecosystem.Kin.KEY_ECOSYSTEM_EXPERIENCE;
import static com.kin.ecosystem.main.Title.MARKETPLACE_TITLE;
import static com.kin.ecosystem.main.Title.ORDER_HISTORY_TITLE;

import android.os.Bundle;
import android.support.annotation.NonNull;
import com.kin.ecosystem.EcosystemExperience;
import com.kin.ecosystem.base.BasePresenter;
import com.kin.ecosystem.common.Observer;
import com.kin.ecosystem.common.exception.BlockchainException;
import com.kin.ecosystem.common.exception.ClientException;
import com.kin.ecosystem.common.model.Balance;
import com.kin.ecosystem.core.bi.EventLogger;
import com.kin.ecosystem.core.bi.events.GeneralEcosystemSdkError;
import com.kin.ecosystem.core.data.blockchain.BlockchainSource;
import com.kin.ecosystem.core.data.settings.SettingsDataSource;
import com.kin.ecosystem.core.util.StringUtil;
import com.kin.ecosystem.main.INavigator;
import com.kin.ecosystem.main.ScreenId;
import com.kin.ecosystem.main.Title;
import com.kin.ecosystem.main.view.IEcosystemView;
import java.math.BigDecimal;

public class EcosystemPresenter extends BasePresenter<IEcosystemView> implements IEcosystemPresenter {

	private static final String KEY_SCREEN_ID = "screen_id";
	private static final String KEY_CONSUMED_INTENT_EXTRAS = "consumed_intent_extras";
	private @ScreenId
	int visibleScreen;
	private @EcosystemExperience
	int experience;
	private boolean isConsumedIntentExtras;
	private final SettingsDataSource settingsDataSource;
	private final BlockchainSource blockchainSource;
	private final EventLogger eventLogger;
	private INavigator navigator;

	private Observer<Balance> balanceObserver;
	private Balance currentBalance;
	private String publicAddress;

	public EcosystemPresenter(@NonNull IEcosystemView view, @NonNull SettingsDataSource settingsDataSource,
		@NonNull final BlockchainSource blockchainSource, @NonNull EventLogger eventLogger,
		@NonNull INavigator navigator, Bundle savedInstanceState, Bundle extras) {
		this.view = view;
		this.settingsDataSource = settingsDataSource;
		this.blockchainSource = blockchainSource;
		this.eventLogger = eventLogger;
		this.navigator = navigator;
		this.currentBalance = blockchainSource.getBalance();
		try {
			this.publicAddress = blockchainSource.getPublicAddress();
		} catch (ClientException | BlockchainException e) {
			eventLogger.send(GeneralEcosystemSdkError.create("EcosystemPresenter blockchainSource.getPublicAddress() thrown an exception"));
		}

		// Must come before processIntentExtras, so we can define if the intent was consumed already.
		processSavedInstanceState(savedInstanceState);
		processIntentExtras(extras);

		this.view.attachPresenter(this);
	}

	private void processSavedInstanceState(Bundle savedInstanceState) {
		this.visibleScreen = getVisibleScreen(savedInstanceState);
		this.isConsumedIntentExtras = getIsConsumedIntentExtras(savedInstanceState);
	}

	private boolean getIsConsumedIntentExtras(Bundle savedInstanceState) {
		return savedInstanceState != null && savedInstanceState.getBoolean(KEY_CONSUMED_INTENT_EXTRAS);
	}

	private int getVisibleScreen(Bundle savedInstanceState) {
		return savedInstanceState != null ? savedInstanceState.getInt(KEY_SCREEN_ID, ScreenId.NONE) : ScreenId.NONE;
	}

	private void processIntentExtras(Bundle extras) {
		if (!isConsumedIntentExtras) {
			this.experience = getExperience(extras);
			this.isConsumedIntentExtras = true;
		}
	}

	private int getExperience(Bundle extras) {
		return extras != null ? extras.getInt(KEY_ECOSYSTEM_EXPERIENCE, EcosystemExperience.NONE)
			: EcosystemExperience.NONE;
	}

	@Override
	public void onAttach(IEcosystemView view) {
		super.onAttach(view);
		if (experience == EcosystemExperience.ORDER_HISTORY) {
			experience = EcosystemExperience.NONE;
			launchOrderHistory();
		} else {
			navigateToVisibleScreen(visibleScreen);
		}
	}

	private void launchOrderHistory() {
		if (view != null) {
			navigator.navigateToOrderHistory(false);
		}
	}

	private void navigateToVisibleScreen(int visibleScreen) {
		if (view != null) {
			switch (visibleScreen) {
				case ScreenId.ORDER_HISTORY:
					if (navigator != null) {
						navigator.navigateToOrderHistory(false);
					}
					break;
				case ScreenId.MARKETPLACE:
				case ScreenId.NONE:
				default:
					if (navigator != null) {
						navigator.navigateToMarketplace(false);
					}
					break;

			}
		}
	}

	@Override
	public void onStart() {
		blockchainSource.reconnectBalanceConnection();
		updateMenuSettingsIcon();
	}

	@Override
	public void onStop() {
		removeBalanceObserver();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(KEY_SCREEN_ID, visibleScreen);
		outState.putBoolean(KEY_CONSUMED_INTENT_EXTRAS, isConsumedIntentExtras);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		removeBalanceObserver();
	}

	private void addBalanceObserver() {
		removeBalanceObserver();
		balanceObserver = new Observer<Balance>() {
			@Override
			public void onChanged(Balance value) {
				currentBalance = value;
				if (isGreaterThenZero(value)) {
					updateMenuSettingsIcon();
				}
			}
		};
		blockchainSource.addBalanceObserver(balanceObserver, false);
	}

	private boolean isGreaterThenZero(Balance value) {
		return value.getAmount().compareTo(BigDecimal.ZERO) == 1;
	}

	private void updateMenuSettingsIcon() {
		if (!StringUtil.isEmpty(publicAddress)) {
			if (!settingsDataSource.isBackedUp(publicAddress)) {
				if (isGreaterThenZero(currentBalance)) {
					changeMenuTouchIndicator(true);
					removeBalanceObserver();
				} else {
					addBalanceObserver();
					changeMenuTouchIndicator(false);
				}
			} else {
				changeMenuTouchIndicator(false);
			}
		}
	}

	private void removeBalanceObserver() {
		if (balanceObserver != null) {
			blockchainSource.removeBalanceObserver(balanceObserver, false);
			balanceObserver = null;
		}
	}

	@Override
	public void balanceItemClicked() {
		if (view != null && visibleScreen != ScreenId.ORDER_HISTORY && navigator != null) {
			navigator.navigateToOrderHistory(false);
		}
	}

	@Override
	public void backButtonPressed() {
		if (view != null) {
			view.navigateBack();
		}
	}

	@Override
	public void visibleScreen(@ScreenId final int id) {
		visibleScreen = id;
		@Title final int title;
		switch (id) {
			case ScreenId.ORDER_HISTORY:
				title = ORDER_HISTORY_TITLE;
				break;
			case ScreenId.MARKETPLACE:
			default:
				title = MARKETPLACE_TITLE;
				break;
		}

		if (view != null) {
			view.updateTitle(title);
		}
	}

	private void changeMenuTouchIndicator(final boolean isVisible) {
		if (view != null) {
			view.showMenuTouchIndicator(isVisible);
		}
	}

	@Override
	public void settingsMenuClicked() {
		if (navigator != null) {
			navigator.navigateToSettings();
		}
	}

	@Override
	public void onMenuInitialized() {
		updateMenuSettingsIcon();
	}
}
