
package com.totalcross.knowcode;

import com.totalcross.knowcode.parse.XmlContainerFactory;
import com.totalcross.knowcode.parse.XmlContainerLayout;

import totalcross.io.IOException;
import totalcross.sys.Convert;
import totalcross.sys.InvalidNumberException;
import totalcross.sys.Settings;
import totalcross.sys.Vm;
import totalcross.ui.Button;
import totalcross.ui.ImageControl;
import totalcross.ui.Label;
import totalcross.ui.MainWindow;
import totalcross.ui.event.ControlEvent;
import totalcross.ui.event.PressListener;
import totalcross.ui.image.Image;
import totalcross.ui.image.ImageException;

public class HomeApplianceXML extends MainWindow {

	private ImageControl cloudImage;

	public HomeApplianceXML() {
		setUIStyle(Settings.MATERIAL_UI);
	}

	static {
		Settings.applicationId = "TCMT";
		Settings.appVersion = "1.0.0";
		Settings.iosCFBundleIdentifier = "com.totalcross.easytiful";
	}

	boolean isDay = true;

	public void initUI() {

		XmlContainerLayout xmlCont = (XmlContainerLayout) XmlContainerFactory.create("xml/homeApplianceXML.xml");
		swap(xmlCont);

		Button plus = (Button) xmlCont.getControlByID("@+id/plus");
		Label insideTempLabel = (Label) xmlCont.getControlByID("@+id/insideTempLabel");
		Label externalTempLabel = (Label) xmlCont.getControlByID("@+id/externalTempLabel");
		cloudImage = (ImageControl) xmlCont.getControlByID("@+id/nuvem");

		new Thread() {
			public void run() {

				while (true) {
					blinkCloud();
					ReadSensors rs = new ReadSensors();
					String temp = rs.readTemp();

					Vm.debug("Recebedno info do dht11 " + temp);
					if (temp != null && !temp.isEmpty() && !temp.equalsIgnoreCase("error"))
						externalTempLabel.setText(temp);

					try {
						cloudAnimation = false;
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}.start();

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


		ImageControl backgroundDay = (ImageControl) xmlCont.getControlByID("@+id/imageView1");
		ImageControl backgroundNight = (ImageControl) xmlCont.getControlByID("@+id/imageView");
		Button night = (Button) xmlCont.getControlByID("@+id/day");
		Button day = (Button) xmlCont.getControlByID("@+id/night");
		day.addPressListener(new PressListener(){

			@Override
			public void controlPressed(ControlEvent e) {
				if (isDay) {
					return;
				}
				try {
					day.setImage(new Image("drawable/day_on.png").getHwScaledInstance(day.getWidth(), day.getHeight()));
					night.setImage(new Image("drawable/nigth_off.png").getHwScaledInstance(day.getWidth(), day.getHeight()));
				} catch (ImageException ex) {
					ex.printStackTrace();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
				toggleImage(backgroundDay, backgroundNight);
				isDay = !isDay;
			}
			
		});


		night.addPressListener(new PressListener(){

			@Override
			public void controlPressed(ControlEvent e) {
				if (!isDay) {
					return;
				}
				try {
					day.setImage(new Image("drawable/day_off.png").getHwScaledInstance(day.getWidth(), day.getHeight()));
					night.setImage(new Image("drawable/nigth_on.png").getHwScaledInstance(day.getWidth(), day.getHeight()));
				} catch (ImageException ex) {
					ex.printStackTrace();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
				toggleImage(backgroundNight, backgroundDay);
				isDay = !isDay;
			}
			
		});
	}

	private void toggleImage(ImageControl day, ImageControl night) {
		for (int i = 0 ; i < 255 ; i+=25) {
			day.getImage().alphaMask = Math.min(i, 255);
			night.getImage().alphaMask = Math.max(0, 255 - i);
			day.repaintNow();
			night.repaintNow();
		}
	}

	boolean cloudAnimation = false;

	private void blinkCloud() {
		cloudAnimation = true;

		new Thread() {
			public void run() {
				while (cloudAnimation) {
					cloudImage.setVisible(false);
					Vm.sleep(500);
					cloudImage.setVisible(true);
					Vm.sleep(500);
				}
			}
		}.start();
	}

}
