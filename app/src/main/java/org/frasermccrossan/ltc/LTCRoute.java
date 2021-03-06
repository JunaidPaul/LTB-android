package org.frasermccrossan.ltc;

public class LTCRoute {

	public String number; // string because parts of website require leading zeros e.g. "02"
	public String name;
	public int direction; // not part of the table, but here for convenience
	public String directionName; // cache of the direction letter
	
	public int NORTHBOUND_IMG = R.drawable.northbound;
	public int SOUTHBOUND_IMG = R.drawable.southbound;
	public int EASTBOUND_IMG = R.drawable.eastbound;
	public int WESTBOUND_IMG = R.drawable.westbound;
	
	//public String url;
	
	LTCRoute(String number, String name/*, String url*/) {
		this.number = number;
		this.name = name;
		//this.url = url;
	}
	
	LTCRoute(String number, String name, int dir) {
		this.number = number;
		this.name = name;
		this.direction = dir;
	}
	
	LTCRoute(String number, String name, int dir, String dirName) {
		this.number = number;
		this.name = name;
		this.direction = dir;
		this.directionName = dirName;
	}
	
	String getRouteNumber() {
		return number.replaceFirst("^0+", "");
	}
	
	String getNameWithNumber() {
		return String.format("%d %s", Integer.valueOf(number), name);
	}
	
	String getShortRouteDirection() {
		return String.format("%d%s", Integer.valueOf(number), directionName == null ? "" : directionName.substring(0,1));
	}
	
	String getRouteDirection() {
		return String.format("%s %s", name, directionName == null ? "" : directionName);
	}
	
	String getOneLetterDirection() {
		return directionName.substring(0,1);
	}
	
	String getTwoLetterDirection() {
		return getOneLetterDirection() + "B";
	}
	
	char dirChar() {
		return directionName.charAt(0);
	}
	
	int getDirectionDrawableRes() {
		switch (dirChar()) {
		case 'N':
		case 'n':
			return NORTHBOUND_IMG;
		case 'S':
		case 's':
			return SOUTHBOUND_IMG;
		case 'E':
		case 'e':
			return EASTBOUND_IMG;
		case 'W':
		case 'w':
			return WESTBOUND_IMG;
		}
		return 0;
	}
	
	@Override
	public String toString() {
		return getShortRouteDirection(); // ew, dirty hack
	}
}
