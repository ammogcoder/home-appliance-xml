
package com.totalcross.knowcode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.totalcross.knowcode.parse.XmlContainerFactory;
import com.totalcross.knowcode.parse.XmlContainerLayout;

import totalcross.io.ByteArrayStream;
import totalcross.io.device.gpiod.GpiodChip;
import totalcross.io.device.gpiod.GpiodLine;
import totalcross.json.JSONArray;
import totalcross.json.JSONObject;
import totalcross.net.HttpStream;
import totalcross.net.URI;
import totalcross.net.ssl.SSLSocketFactory;
import totalcross.sys.Convert;
import totalcross.sys.Settings;
import totalcross.sys.Vm;
import totalcross.ui.Button;
import totalcross.ui.ImageControl;
import totalcross.ui.Label;
import totalcross.ui.MainWindow;
import totalcross.ui.event.ControlEvent;
import totalcross.ui.event.PressListener;
import totalcross.ui.event.UpdateListener;
import totalcross.ui.image.ImageException;

public class HomeApplianceXML extends MainWindow {

	// Firebase Realtime Database Secret Key
	private static final String AUTH_KEY = "9xev12w1d3uGdsBiVjwXQkUov3WfJh7lojO96MB0";

	// Firebase Realtime Database document URL
	private static final String FIREBASE_URL = "https://webinarhomeappliance.firebaseio.com/databases/(default)/documents/commands.json?auth="
			+ AUTH_KEY;

	/** Instance of GpiodChip, uset to initialize #pin for embedded devices */
	private GpiodChip gpioChip;

	/**
	 * Pin of {@link #gpioChip} that will be used to flash led when receiving a
	 * command from Firebase integration
	 */
	private GpiodLine pin;

	/* Controls obtained by KnowCodeXML */
	private ImageControl backgroundDay;
	private ImageControl backgroundNight;
	private ImageControl externalTempIcon;

	private Label insideTempLabel;

	private Button nightButton;
	private Button dayButton;
	/**/

	/*
	 * State vars
	 */

	/**
	 * State boolean that controls cloudBlink thread
	 * 
	 * @see #blinkCloud()
	 */
	boolean cloudAnimation = false;

	/**
	 * State boolean value that represents the actual state of background.
	 */
	private boolean isDay;

	/**
	 * State boolean value used in {@link #setDay(boolean)} to toggle background
	 */
	private boolean day = true;

	/**
	 * State int value used in {@link #updateListener} to show temp in
	 * {@link #insideTempLabel}
	 */
	private int insideTemp = 20;
	/**/

	/**
	 * There is a update listener, that will set insideTempLabel to insideTemp and
	 * background to day or night, according with insideTemp and isDay state vars,
	 * respectively.
	 */
	UpdateListener updateListener = new UpdateListener() {

		@Override
		public void updateListenerTriggered(int elapsedMilliseconds) {
			// Call method that will toggle background and buttons
			setDay();

			// Its a very newbie comment, but note that before doing anything with controls
			// it's a good practice to check if is not null to prevent
			// NullPointerException if it's not defined in xml.
			if (insideTempLabel != null)
				insideTempLabel.setText(insideTemp + "");
		}

	};

	/*
	 * This static initializer block runs before MainWindow Constructor Here is the
	 * better place to set TC fields
	 */
	static {
		Settings.applicationId = "TCMT";
		Settings.appVersion = "1.0.0";
		Settings.iosCFBundleIdentifier = "com.totalcross.easytiful";
	}

	public HomeApplianceXML() {
		// UI Style can be set just here, in MainWindow constructor
		setUIStyle(Settings.MATERIAL_UI);
	}

	// initUI is called by setRect, after the constructor
	public void initUI() {
		/*
		 * Creating the XmlContainerLayout based in the xml created in AndroidStudio.
		 * Note that images specified in android:background tag are placed in
		 * src/main/java/resources/drawable
		 */
		final XmlContainerLayout xmlCont = (XmlContainerLayout) XmlContainerFactory.create("xml/homeApplianceXML2.xml");
		// Swapping from MainWindow to XmlContainerLayout.
		swap(xmlCont);

		backgroundDay = (ImageControl) xmlCont.getControlByID("@+id/imageView1");
		backgroundNight = (ImageControl) xmlCont.getControlByID("@+id/imageView");
		/**/

		/**
		 * Checking if it's a embedded device like Raspberry or Toradex before
		 * initialize gpio chip and pin
		 */
		if (Settings.platform.equalsIgnoreCase("linux_arm")) {

			// Opening Gpio chip
			gpioChip = GpiodChip.open(0);

			// Gets Gpio line
			pin = gpioChip.line(21);

			// Request line as output and set the initial state to low
			pin.requestOutput("CONSUMER", 0);
		}
		/**/

		/*
		 * Reads external temp and blinks cloud
		 */

		// Getting Label instance that matches with "android:id" tag in xml.
		// This label will show the external temp, that is read by sensor.
		final Label externalTempLabel = (Label) xmlCont.getControlByID("@+id/externalTempLabel");

		// Getting ImageControl instance that matches with "android:id" tag in xml.
		// This ImageControl is the cloud, that will blink.
		externalTempIcon = (ImageControl) xmlCont.getControlByID("@+id/tempIcon");

		// Plays the cloud blink animation while ReadSensors is reading value from the
		// sensor.
		// It's very important to create a separated Thread for this, another way the
		// app will freeze.
		new Thread() {

			public void run() {
				while (true) {
					// Calling blink animation
					blinkCloud();

					// Reading temp from sensor
					final ReadSensors readSensors = new ReadSensors();
					final String externalTemp = readSensors.readTemp();

					// Its a very newbie comment, but note that before doing anything with string
					// it's a good practice to check if is not null to prevent
					// NullPointerException
					if (externalTempLabel != null && externalTemp != null && !externalTemp.isEmpty())
						externalTempLabel.setText(externalTemp);

					try {
						// It stops reading and cloud animation by five seconds.
						cloudAnimation = false;
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

		}.start();
		/**/

		/*
		 * Increasing and decreasing insideTemp
		 */

		// Getting Label instance that matches with "android:id" tag in xml.
		// This label will show the temp inside selector
		insideTempLabel = (Label) xmlCont.getControlByID("@+id/insideTempLabel");

		// Getting Button instance that matches with "android:id" tag in xml.
		// This button will increase temp inside selector
		final Button plusButton = (Button) xmlCont.getControlByID("@+id/plus");

		// Its a very newbie comment, but note that before doing anything with controls
		// it's a good practice to check if is not null to prevent
		// NullPointerException if it's not defined in xml.

		if (plusButton != null) {
			// Handling events for plusButton
			plusButton.addPressListener(new PressListener() {

				@Override
				public void controlPressed(ControlEvent e) {
					if (insideTempLabel != null) {
						// Sets label text increasing temp
						insideTempLabel.setText(Convert.toString(++insideTemp));
					}
				}

			});
		}

		// Getting Button instance that matches with "android:id" tag in xml.
		// This button will decrease temp inside selector
		final Button minusButton = (Button) xmlCont.getControlByID("@+id/minus");

		// Its a very newbie comment, but note that before doing anything with controls
		// it's a good practice to check if is not null to prevent
		// NullPointerException if it's not defined in xml.
		if (minusButton != null) {
			// Handling events for minusButton
			minusButton.addPressListener(new PressListener() {

				@Override
				public void controlPressed(ControlEvent e) {
					if (insideTempLabel != null) {
						// Sets label text decreasing temp
						insideTempLabel.setText(Convert.toString(--insideTemp));
					}
				}

			});
		}
		/**/

		/*
		 * Togglers
		 */

		// Getting Button instance that matches with "android:id" tag in xml.
		// This button will set isDay to true
		dayButton = (Button) xmlCont.getControlByID("@+id/night");

		// Its a very newbie comment, but note that before doing anything with controls
		// it's a good practice to check if is not null to prevent
		// NullPointerException if it's not defined in xml.

		if (dayButton != null) {

			// Handling events for minusButton
			dayButton.addPressListener(new PressListener() {

				@Override
				public void controlPressed(ControlEvent e) {
					day = true;
				}

			});
		}

		// Getting Button instance that matches with "android:id" tag in xml.
		// This button will isDay to false
		nightButton = (Button) xmlCont.getControlByID("@+id/day");

		// Its a very newbie comment, but note that before doing anything with controls
		// it's a good practice to check if is not null to prevent
		// NullPointerException if it's not defined in xml.

		if (nightButton != null) {

			// Handling events for minusButton
			nightButton.addPressListener(new PressListener() {

				@Override
				public void controlPressed(ControlEvent e) {
					day = false;
				}
			});
		}
		/**/

		// Starts the Firebase integration, that will query every to seconds for state
		// defined in remote control application.
		// It's very important to create a separated Thread for this, another way the
		// app will freeze.
		new Thread() {

			public void run() {
				try {
					while (true) {
						// Calls the integration
						listen();
						// Wait for two seconts until call again
						Thread.sleep(2000);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}.start();

		// After entire UI is initiated, adding a UpdateListener will help to monitore
		// state variables and perform actions based in their values
		MainWindow.getMainWindow().addUpdateListener(updateListener);
	}

	/**
	 * Toggles visibility of before and after with transition effect
	 * 
	 * @param before
	 * @param after
	 */
	private void toggleImage(ImageControl before, ImageControl after) {
		// Its a very newbie comment, but note that before doing anything with controls
		// and nullable vars
		// it's a good practice to check if is not null to prevent
		// NullPointerException if it's not defined in xml.

		if (before != null && after != null) {
			for (int i = 0; i < 255; i += 25) {
				if (i + 5 == 255)
					i = 255;
				before.getImage().alphaMask = Math.min(i, 255);
				after.getImage().alphaMask = Math.max(0, 255 - i);
				before.repaintNow();
				after.repaintNow();
			}
		}
	}

	/**
	 * Blinks cloud icon, keeping it visible by 500ms and invisible by 500ms while
	 * {@link #cloudAnimation} is true
	 */
	private void blinkCloud() {
		cloudAnimation = true;

		// Its a very newbie comment, but note that before doing anything with controls
		// and nullable vars
		// it's a good practice to check if is not null to prevent
		// NullPointerException if it's not defined in xml.
		if (externalTempIcon != null) {
			new Thread() {

				public void run() {
					while (cloudAnimation) {
						externalTempIcon.setVisible(false);
						Vm.sleep(500);
						externalTempIcon.setVisible(true);
						Vm.sleep(500);
					}
				}

			}.start();
		}
	}

	/**
	 * Queries Firebase Realtime Database for new commands
	 */
	private void listen() {
		try {
			/*
			 * Setting http options
			 */
			final HttpStream.Options options = new HttpStream.Options();
			options.readTimeOut = 15000;
			options.socketFactory = new SSLSocketFactory(); // This is very important because Firebase uses HTTPS
			options.writeTimeOut = 15000;
			options.openTimeOut = 5000;
			options.httpType = HttpStream.GET;
			options.writeBytesSize = 4096;
			options.setContentType("application/json; charset=UTF-8");
			options.setCharsetEncoding("UTF-8");
			/**/

			/*
			 * Performing query
			 */
			HttpStream hs = new HttpStream(new URI(FIREBASE_URL.concat("&orderBy=\"timestamp\"")), options);
			ByteArrayStream bas = new ByteArrayStream(4096);
			bas.readFully(hs, 10, 4096);
			hs.close();
			String result = new String(bas.getBuffer(), 0, bas.available());
			bas.close();
			/**/

			if (result == null || result.equalsIgnoreCase("null"))
				return; // returning to prevent NullPointerException

			/*
			 * Parsing response in JSon Object
			 */
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
			/**/

			/*
			 * Sorting to grant that the last comand sent will be executed last
			 */
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
			/**/

			// If it's running in embedded device with supports gpio, light up a pilot led
			// to
			// indicate that it's being controlled remotely with Firebase integration
			if (listCommands != null && !listCommands.isEmpty() && Settings.platform.equalsIgnoreCase("linux_arm")
					&& pin != null)
				pin.setValue(1);

			// Execute all comands in list sorted by timestamp, setting state vars, that
			// will be handled by updateListener
			for (JSONObject command : listCommands) {

				day = command.getBoolean("power");

				insideTemp = command.getInt("temp");

				// Delete actual command in Firebase, to prevent re-execution of it
				delete(command.getString("id"));
			}

			// Turns off the pilot led
			if (Settings.platform.equalsIgnoreCase("linux_arm") && pin != null)
				pin.setValue(0);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Delete command in Firebase Realtime Database, to prevent re-execution of same
	 * 
	 * @param id
	 * @throws Exception
	 */
	public void delete(String id) throws Exception {
		/*
		 * Setting http options
		 */
		final HttpStream.Options options = new HttpStream.Options();
		options.readTimeOut = 15000;
		options.socketFactory = new SSLSocketFactory(); // This is very important because Firebase uses HTTPS
		options.writeTimeOut = 15000;
		options.openTimeOut = 5000;
		options.httpType = HttpStream.DELETE;
		options.writeBytesSize = 4096;
		options.setContentType("application/json; charset=UTF-8");
		options.setCharsetEncoding("UTF-8");

		// Building URL
		URI uri = new URI(FIREBASE_URL.replace(".json", "/".concat(id).concat(".json")));

		/*
		 * Performing delete
		 */
		try (final HttpStream hs = new HttpStream(uri, options)) {
			if (!hs.isOk()) {
				throw new Exception("Connection Error!");
			}
		}
	}

	/**
	 * Toggle buttons and backgroung between night and day mode, according with
	 * {@link #day} var
	 */
	private void setDay() {
		// This check is to prevent performance leak by changing background
		// unnecessarily
		if (day == isDay)
			return;

		// Its a very newbie comment, but note that before doing anything with controls
		// and nullable vars
		// it's a good practice to check if is not null to prevent
		// NullPointerException if it's not defined in xml.

		try {
			if (day) {
				if (dayButton != null && Images.dayOnImage != null)
					dayButton.setImage(
							Images.dayOnImage.getHwScaledInstance(dayButton.getWidth(), dayButton.getHeight()));
				if (nightButton != null && Images.nightOffImage != null)
					nightButton.setImage(
							Images.nightOffImage.getHwScaledInstance(nightButton.getWidth(), nightButton.getHeight()));
			} else {
				if (dayButton != null && Images.dayOffImage != null)
					dayButton.setImage(
							Images.dayOffImage.getHwScaledInstance(dayButton.getWidth(), dayButton.getHeight()));
				if (nightButton != null && Images.nightOnImage != null)
					nightButton.setImage(
							Images.nightOnImage.getHwScaledInstance(nightButton.getWidth(), nightButton.getHeight()));
			}
		} catch (ImageException ex) {
			ex.printStackTrace();
		}

		// Toggle image from actual background to new background.
		// for example:
		// if actually is day, will pass day in before and night in after and vice versa
		toggleImage(day ? backgroundDay : backgroundNight, day ? backgroundNight : backgroundDay);

		// Set actual state value to prevent method running unnecessarily
		isDay = day;

	}

}
