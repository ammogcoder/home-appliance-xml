package com.totalcross.knowcode;

import totalcross.ui.image.Image;

public class Images {

	// Declaring the image variables.

	public static Image dayOffImage;
	public static Image dayOnImage;

	public static Image nightOffImage;
	public static Image nightOnImage;

	public static void loadImages() {
		try {
			// Trying the initialize images.
			dayOffImage = new Image("drawable/day_off.png");
			dayOnImage = new Image("drawable/day_on.png");

			nightOffImage = new Image("drawable/nigth_off.png");
			nightOnImage = new Image("drawable/nigth_on.png");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
