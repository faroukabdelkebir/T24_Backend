package com.t24extractor.utils;

import java.util.Date;

public  class DateUtils {
public static String differenceBetween(Date d1, Date d2) {
	
	long diff;

    if (d1.getTime() > d2.getTime()) {
        diff = (int) (d1.getTime() - d2.getTime());

    } else
        diff = (int) (d2.getTime() - d1.getTime());
	long timeDifference = diff / 1000;
    int h = (int) (timeDifference / (3600));
    int m = (int) ((timeDifference - (h * 3600)) / 60);
    int s = (int) (timeDifference - (h * 3600) - m * 60);

    return String.format("%02d:%02d:%02d", h, m, s);
}
}
