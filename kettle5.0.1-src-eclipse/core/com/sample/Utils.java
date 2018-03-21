package com.sample;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class Utils {
	private static String[] names = new String[11];
	private static String[] codes = new String[11];

	static {
		names[0] = "宝马";
		codes[0] = "BMW";
		names[1] = "丰田";
		codes[1] = "Toyota";
		names[2] = "本田";
		codes[2] = "Honda";
		names[3] = "日产";
		codes[3] = "Nissan";
		names[4] = "马自达";
		codes[4] = "Mazda";
		names[5] = "奥迪";
		codes[5] = "Audi";
		names[6] = "大众";
		codes[6] = "Volkswagen";
		names[7] = "福特";
		codes[7] = "Ford";
		names[8] = "现代";
		codes[8] = "Hyundai";
		names[9] = "奔驰";
		codes[9] = "Bez";
		names[10] = "标致";
		codes[10] = "Peugeot";
	}

	public static String getRandomName() {
		return names[new Random().nextInt(10)];
	}

	public static String getRandomCode() {
		return codes[new Random().nextInt(10)];
	}

	public static double getRandomNumber(double min, double max) {
		return nextDouble(min - 1, max + 1);
	}

	public static double nextDouble(final double min, final double max) {
		if (min == max) {
			return min;
		}
		return min + ((max - min) * new Random().nextDouble());
	}

	public static int getRandomInteger(int scope) {
		return new Random().nextInt(scope);
	}

	public static Date getDateTime(int add) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, -5);
		cal.add(Calendar.MINUTE, add);
		return cal.getTime();
	}
}
