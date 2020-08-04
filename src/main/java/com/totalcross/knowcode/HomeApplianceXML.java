
package com.totalcross.knowcode;

import com.totalcross.knowcode.parse.XmlContainerFactory;
import com.totalcross.knowcode.parse.XmlContainerLayout;

import totalcross.sys.Convert;
import totalcross.sys.InvalidNumberException;
import totalcross.sys.Settings;
import totalcross.ui.Button;
import totalcross.ui.Label;
import totalcross.ui.MainWindow;
import totalcross.ui.event.ControlEvent;
import totalcross.ui.event.PressListener;

public class HomeApplianceXML extends MainWindow {

	public HomeApplianceXML() {
		setUIStyle(Settings.MATERIAL_UI);
	}

	static {
		Settings.applicationId = "TCMT";
		Settings.appVersion = "1.0.0";
		Settings.iosCFBundleIdentifier = "com.totalcross.easytiful";
	}

	public void initUI() {

		/*
		 * Creating the XmlContainerLayout based in the xml created in AndroidStudio.
		 * Note that images specified in android:background tag are placed in
		 * src/main/java/resources/drawable
		 */
		final XmlContainerLayout xmlCont = (XmlContainerLayout) XmlContainerFactory.create("xml/homeApplianceXML.xml");

		// Swapping from MainWindow to XmlContainerLayout.
		swap(xmlCont);

		// Getting Label instance that matches with "android:id" tag in xml.
		final Label insideTempLabel = (Label) xmlCont.getControlByID("@+id/insideTempLabel");

		// Getting Button instance that matches with "android:id" tag in xml.
		final Button plusButton = (Button) xmlCont.getControlByID("@+id/plus");

		// Handling events for gotten button
		plusButton.addPressListener(new PressListener() {

			@Override
			public void controlPressed(ControlEvent e) {
				try {
					final String tempString = insideTempLabel.getText();

					int temp = Convert.toInt(tempString);

					// Sets label text increasing temp
					insideTempLabel.setText(Convert.toString(++temp));
				} catch (InvalidNumberException exception) {
					exception.printStackTrace();
				}

			}
		});

		// Getting Button instance that matches with "android:id" tag in xml.
		final Button minusButton = (Button) xmlCont.getControlByID("@+id/minus");

		// Handling events for gotten button
		minusButton.addPressListener(new PressListener() {

			@Override
			public void controlPressed(ControlEvent e) {
				try {
					final String tempString = insideTempLabel.getText();

					int temp = Convert.toInt(tempString);

					// Sets label text decreasing temp
					insideTempLabel.setText(Convert.toString(--temp));
				} catch (InvalidNumberException exception) {
					exception.printStackTrace();
				}

			}
		});

	}
}
