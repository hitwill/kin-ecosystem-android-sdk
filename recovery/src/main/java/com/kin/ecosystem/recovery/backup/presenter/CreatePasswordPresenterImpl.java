package com.kin.ecosystem.recovery.backup.presenter;

import static com.kin.ecosystem.recovery.backup.view.BackupNextStepListener.KEY_ACCOUNT_KEY;
import static com.kin.ecosystem.recovery.backup.view.BackupNextStepListener.STEP_CLOSE;
import static com.kin.ecosystem.recovery.backup.view.BackupNextStepListener.STEP_SAVE_AND_SHARE;

import android.os.Bundle;
import android.support.annotation.NonNull;
import com.kin.ecosystem.recovery.KeyStoreProvider;
import com.kin.ecosystem.recovery.backup.view.BackupNextStepListener;
import com.kin.ecosystem.recovery.backup.view.CreatePasswordView;
import com.kin.ecosystem.recovery.base.BasePresenterImpl;
import com.kin.ecosystem.recovery.exception.BackupException;

public class CreatePasswordPresenterImpl extends BasePresenterImpl<CreatePasswordView> implements
	CreatePasswordPresenter {

	private final BackupNextStepListener backupNextStepListener;
	private final KeyStoreProvider keyStoreProvider;
	private boolean isPasswordRulesOK = false;
	private boolean isPasswordsMatches = false;
	private boolean isIUnderstandChecked = false;

	public CreatePasswordPresenterImpl(@NonNull final BackupNextStepListener backupNextStepListener, @NonNull final
	KeyStoreProvider keyStoreProvider) {
		this.backupNextStepListener = backupNextStepListener;
		this.keyStoreProvider = keyStoreProvider;
	}

	@Override
	public void onBackClicked() {
		backupNextStepListener.setStep(STEP_CLOSE, null);
	}

	@Override
	public void enterPasswordChanged(String password) {
		if (keyStoreProvider.validatePassword(password)) {
			isPasswordRulesOK = true;
			if (view != null) {
				view.setEnterPasswordIsCorrect(true);
			}

		} else {
			isPasswordRulesOK = false;
			if (view != null) {
				view.setEnterPasswordIsCorrect(false);
			}

		}
		checkAllCompleted();
	}

	@Override
	public void confirmPasswordChanged(String mainPassword, String confirmPassword) {
		if (mainPassword.equals(confirmPassword)) {
			isPasswordsMatches = true;
			if (view != null) {
				view.setConfirmPasswordIsCorrect(true);
			}
		} else {
			isPasswordsMatches = false;
			if (view != null) {
				view.setConfirmPasswordIsCorrect(false);
			}
		}
		checkAllCompleted();
	}

	@Override
	public void iUnderstandChecked(boolean isChecked) {
		isIUnderstandChecked = isChecked;
		checkAllCompleted();
	}

	@Override
	public void nextButtonClicked(String password) {
		try {
			final String key = keyStoreProvider.exportAccount(password);
			final Bundle data = new Bundle();
			data.putString(KEY_ACCOUNT_KEY, key);
			backupNextStepListener.setStep(STEP_SAVE_AND_SHARE, data);
		} catch (BackupException e) {
			if (view != null) {
				view.showBackupFailed();
			}
		}
	}

	private void checkAllCompleted() {
		if (isPasswordRulesOK && isPasswordsMatches && isIUnderstandChecked) {
			enableNextButton();
		} else {
			disableNextButton();
		}
	}

	private void disableNextButton() {
		if (view != null) {
			view.disableNextButton();
		}
	}

	private void enableNextButton() {
		if (view != null) {
			view.enableNextButton();
		}
	}
}
