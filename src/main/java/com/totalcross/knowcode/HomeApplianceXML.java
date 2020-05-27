
package com.totalcross.knowcode;

import com.totalcross.knowcode.parse.XmlContainerLayout;
import com.totalcross.knowcode.parse.XmlContainerFactory;

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

		XmlContainerLayout xmlCont = (XmlContainerLayout) XmlContainerFactory.create("xml/homeApplianceXML.xml");
		swap(xmlCont);

		Button plus = (Button) xmlCont.getControlByID("@+id/plus");
		Label insideTempLabel = (Label) xmlCont.getControlByID("@+id/insideTempLabel");

		plus.addPressListener(new PressListener() {

			@Override
			public void controlPressed(ControlEvent e) {
				// TODO

				try {

					String tempString = insideTempLabel.getText();
					int temp;
					temp = Convert.toInt(tempString);
					insideTempLabel.setText(Convert.toString(++temp));

				} catch (InvalidNumberException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}
		});

		Button minus = (Button) xmlCont.getControlByID("@+id/minus");
		minus.addPressListener(new PressListener() {

			@Override
			public void controlPressed(ControlEvent e) {
				// TODO

				try {
					String tempString = insideTempLabel.getText();
					int temp;
					temp = Convert.toInt(tempString);
					insideTempLabel.setText(Convert.toString(--temp));

				} catch (InvalidNumberException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}
		});

	}
}
