package awesome.console.config;

import com.intellij.openapi.options.Configurable;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AwesomeConsoleConfigurable implements Configurable {

	@Transient
	private AwesomeConsoleConfigForm form;

	/**
	 * Configurable
	 */
	@Nls
	@Override
	public String getDisplayName() {
		return "Awesome Console";
	}

	@Nullable
	@Override
	public String getHelpTopic() {
		return "help topic";
	}

	@Nullable
	@Override
	public JComponent createComponent() {
		form = new AwesomeConsoleConfigForm();
		initFromConfig();
		return form.mainpanel;
	}

	@Override
	public boolean isModified() {
		final String text = form.maxLengthTextField.getText().trim();
		if (text.length() < 1) {
			return true;
		}
		final int len;
		try {
			len = Integer.parseInt(text);
		} catch (final NumberFormatException nfe) {
			return true;
		}
		AwesomeConsoleConfig config = AwesomeConsoleConfig.getInstance();
		return form.limitLineMatchingByCheckBox.isSelected() != config.LIMIT_LINE_LENGTH
				|| len != config.LINE_MAX_LENGTH
				|| form.matchLinesLongerThanCheckBox.isSelected() != config.SPLIT_ON_LIMIT
				|| form.searchForURLsFileCheckBox.isSelected() != config.SEARCH_URLS;
	}

	@Override
	public void apply() {
		final String text = form.maxLengthTextField.getText().trim();
		if (text.length() < 1) {
			showErrorDialog();
			return;
		}
		final int maxLength;
		try {
			maxLength = Integer.parseInt(text);
		} catch (final NumberFormatException nfe) {
			showErrorDialog();
			return;
		}
		if (maxLength < 1) {
			showErrorDialog();
			return;
		}
		AwesomeConsoleConfig config = AwesomeConsoleConfig.getInstance();
		config.LIMIT_LINE_LENGTH = form.limitLineMatchingByCheckBox.isSelected();
		config.LINE_MAX_LENGTH = maxLength;
		config.SPLIT_ON_LIMIT = form.matchLinesLongerThanCheckBox.isSelected();
		config.SEARCH_URLS = form.searchForURLsFileCheckBox.isSelected();
//		config.loadState(config); TODO unnecessary?
	}

	@Override
	public void reset() {
		initFromConfig();
	}

	@Override
	public void disposeUIResources() {
		form = null;
	}

	private void showErrorDialog() {
		JOptionPane.showMessageDialog(form.mainpanel, "Error: Please enter a positive number.", "Invalid value", JOptionPane.ERROR_MESSAGE);
	}

	private void initFromConfig() {
		AwesomeConsoleConfig config = AwesomeConsoleConfig.getInstance();
		form.limitLineMatchingByCheckBox.setSelected(config.LIMIT_LINE_LENGTH);

		form.matchLinesLongerThanCheckBox.setEnabled(config.LIMIT_LINE_LENGTH);
		form.matchLinesLongerThanCheckBox.setSelected(config.SPLIT_ON_LIMIT);

		form.searchForURLsFileCheckBox.setSelected(config.SEARCH_URLS);

		form.maxLengthTextField.setText(String.valueOf(config.LINE_MAX_LENGTH));
		form.maxLengthTextField.setEnabled(config.LIMIT_LINE_LENGTH);
		form.maxLengthTextField.setEditable(config.LIMIT_LINE_LENGTH);
	}
}
