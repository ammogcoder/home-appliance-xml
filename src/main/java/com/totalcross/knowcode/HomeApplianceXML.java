
package com.totalcross.knowcode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.totalcross.knowcode.parse.XmlContainerFactory;
import com.totalcross.knowcode.parse.XmlContainerLayout;

import totalcross.io.ByteArrayStream;
import totalcross.io.IOException;
import totalcross.io.device.gpiod.GpiodChip;
import totalcross.io.device.gpiod.GpiodLine;
import totalcross.json.JSONArray;
import totalcross.json.JSONObject;
import totalcross.net.HttpStream;
import totalcross.net.URI;
import totalcross.net.ssl.SSLSocketFactory;
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
import totalcross.ui.event.UpdateListener;
import totalcross.ui.image.Image;
import totalcross.ui.image.ImageException;

public class HomeApplianceXML extends MainWindow {

	private ImageControl tempIcon;

	private ImageControl backgroundDay, backgroundNight;
	private Label insideTempLabel;
	boolean isDay = true;
	boolean changeDay = true;
	int newTemp = 20;
	private GpiodChip gpioChip;
	private GpiodLine pin;

	Button nightButton, dayButton;

	// Firebase Secret
	private static final String AUTH_KEY = "9xev12w1d3uGdsBiVjwXQkUov3WfJh7lojO96MB0";

	final String FIREBASE_URL = "https://webinarhomeappliance.firebaseio.com/databases/(default)/documents/commands.json?auth="
			+ AUTH_KEY;

	public HomeApplianceXML() {
		setUIStyle(Settings.FLAT_UI);
	}

	static {
		Settings.applicationId = "TCMT";
		Settings.appVersion = "1.0.0";
		Settings.iosCFBundleIdentifier = "com.totalcross.easytiful";
	}

	UpdateListener updateListener = new UpdateListener() {

		@Override
		public void updateListenerTriggered(int elapsedMilliseconds) {
			setDay(changeDay);
			insideTempLabel.setText(newTemp + "");
		}
	};

	public void initUI() {

		XmlContainerLayout xmlCont = (XmlContainerLayout) XmlContainerFactory.create("xml/homeApplianceXML2.xml");
		swap(xmlCont);

		if (Settings.platform.equalsIgnoreCase("linux_arm")) {

			// Open Gpio chip
			gpioChip = GpiodChip.open(0);

			// Get Gpio line
			pin = gpioChip.line(21);

			// Request line as output and set the initial state to low
			pin.requestOutput("CONSUMER", 0);
		}

		Button plus = (Button) xmlCont.getControlByID("@+id/plus");
		insideTempLabel = (Label) xmlCont.getControlByID("@+id/insideTempLabel");
		Label externalTempLabel = (Label) xmlCont.getControlByID("@+id/externalTempLabel");
		tempIcon = (ImageControl) xmlCont.getControlByID("@+id/tempIcon");

		new Thread() {
			public void run() {

				while (true) {
					blinkCloud();
					ReadSensors rs = new ReadSensors();
					String temp = rs.readTemp();

					if (temp != null && !temp.isEmpty())
						externalTempLabel.setText(temp);

					try {
						cloudAnimation = false;
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();

		plus.addPressListener(new PressListener() {

			@Override
			public void controlPressed(ControlEvent e) {

				try {

					String tempString = insideTempLabel.getText();
					int temp;
					temp = Convert.toInt(tempString);
					newTemp = ++temp;
					insideTempLabel.setText(newTemp + "");

				} catch (InvalidNumberException e1) {
					e1.printStackTrace();
				}

			}
		});

		Button minus = (Button) xmlCont.getControlByID("@+id/minus");
		minus.addPressListener(new PressListener() {

			@Override
			public void controlPressed(ControlEvent e) {

				try {
					String tempString = insideTempLabel.getText();
					int temp;
					temp = Convert.toInt(tempString);
					newTemp = --temp;
					insideTempLabel.setText(newTemp + "");

				} catch (InvalidNumberException e1) {
					e1.printStackTrace();
				}

			}
		});

		backgroundDay = (ImageControl) xmlCont.getControlByID("@+id/imageView1");
		backgroundNight = (ImageControl) xmlCont.getControlByID("@+id/imageView");

		nightButton = (Button) xmlCont.getControlByID("@+id/day");
		dayButton = (Button) xmlCont.getControlByID("@+id/night");
		if (dayButton != null) {

			dayButton.addPressListener(new PressListener() {

				@Override
				public void controlPressed(ControlEvent e) {
					changeDay = true;
					setDay(changeDay);
				}
			});
		}

		if (nightButton != null) {
			nightButton.addPressListener(new PressListener() {

				@Override
				public void controlPressed(ControlEvent e) {
					changeDay = false;
					setDay(changeDay);
				}

			});
		}

		new Thread() {

			public void run() {
				try {
					while (true) {

						listen();
						Thread.sleep(2000);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}.start();

		// MainWindow.getMainWindow().addUpdateListener(updateListener);

	}

	private void toggleImage(ImageControl day, ImageControl night) {

		for (int i = 0; i < 255; i += 25) {
			if (i + 5 == 255)
				i = 255;
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
					tempIcon.setVisible(false);
					Vm.sleep(500);
					tempIcon.setVisible(true);
					Vm.sleep(500);
				}
			}
		}.start();
	}

	private void listen() {
		try {
			final HttpStream.Options options = new HttpStream.Options();
			options.readTimeOut = 15000;
			options.socketFactory = new SSLSocketFactory();
			options.writeTimeOut = 15000;
			options.openTimeOut = 5000;
			options.httpType = HttpStream.GET;
			options.writeBytesSize = 4096;
			options.setContentType("application/json; charset=UTF-8");
			options.setCharsetEncoding("UTF-8");

			HttpStream hs = new HttpStream(new URI(FIREBASE_URL.concat("&orderBy=\"timestamp\"")), options);
			ByteArrayStream bas = new ByteArrayStream(4096);
			bas.readFully(hs, 10, 4096);
			hs.close();
			String result = new String(bas.getBuffer(), 0, bas.available());
			bas.close();

			if (result == null || result.equalsIgnoreCase("null"))
				return;

			JSONObject data = new JSONObject(result);

			List<JSONObject> listCommands = new ArrayList<JSONObject>();

			JSONArray ids = data.names();
			JSONArray array = data.toJSONArray(ids);
			for (int i = 0; i < array.length(); i++) {
				JSONObject command = array.getJSONObject(i);
				String id = ids.getString(i);
				command.put("id", id);
				listCommands.add(command);
			}

			Collections.sort(listCommands, new Comparator<JSONObject>() {

				@Override
				public int compare(JSONObject o1, JSONObject o2) {
					if (o1.getLong("timestamp") < o2.getLong("timestamp")) {
						return -1;
					}
					if (o1.getLong("timestamp") > o2.getLong("timestamp")) {
						return 1;
					}
					return 0;
				}

			});

			if (listCommands != null && !listCommands.isEmpty() && Settings.platform.equalsIgnoreCase("linux_arm"))
				pin.setValue(1);

			MainWindow.getMainWindow().addUpdateListener(updateListener);
			for (JSONObject command : listCommands) {

				boolean power = command.getBoolean("power");
				newTemp = command.getInt("temp");

				// setDay(power);
				changeDay = power;
				delete(command.getString("id"));
			}

			if (Settings.platform.equalsIgnoreCase("linux_arm"))
				pin.setValue(0);

			MainWindow.getMainWindow().removeUpdateListener(updateListener);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void delete(String id) throws Exception {
		final HttpStream.Options options = new HttpStream.Options();
		options.readTimeOut = 15000;
		options.socketFactory = new SSLSocketFactory();
		options.writeTimeOut = 15000;
		options.openTimeOut = 5000;
		options.httpType = HttpStream.DELETE;
		options.writeBytesSize = 4096;
		options.setContentType("application/json; charset=UTF-8");
		options.setCharsetEncoding("UTF-8");

		URI uri = new URI(FIREBASE_URL.replace(".json", "/".concat(id).concat(".json")));
		Vm.debug(uri.toString());
		try (final HttpStream hs = new HttpStream(uri, options)) {
			if (!hs.isOk()) {
				throw new Exception("Connection Error!");
			}
		}
	}

	private void setDay(boolean day) {
		if (day == isDay) {
			return;
		}
		if (day) {
			try {
				dayButton.setImage(new Image("drawable/day_on.png").getHwScaledInstance(dayButton.getWidth(),
						dayButton.getHeight()));
				nightButton.setImage(new Image("drawable/nigth_off.png").getHwScaledInstance(nightButton.getWidth(),
						nightButton.getHeight()));
			} catch (ImageException ex) {
				ex.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			isDay = true;
			toggleImage(backgroundDay, backgroundNight);

		} else {
			try {
				dayButton.setImage(new Image("drawable/day_off.png").getHwScaledInstance(dayButton.getWidth(),
						dayButton.getHeight()));
				nightButton.setImage(new Image("drawable/nigth_on.png").getHwScaledInstance(nightButton.getWidth(),
						nightButton.getHeight()));
			} catch (ImageException ex) {
				ex.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			isDay = false;
			toggleImage(backgroundNight, backgroundDay);

		}
	}

}
