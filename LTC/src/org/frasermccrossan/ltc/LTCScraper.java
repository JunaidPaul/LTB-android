package org.frasermccrossan.ltc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;

// everything required to load the LTC_supplied data into the database
public class LTCScraper {

	BusDb db;
	ScrapingStatus status;
	Context c;
	static final String ROUTE_URL = "http://teuchter.lan:8000/routes.html";
	static final String DIRECTION_URL = "http://teuchter.lan:8000/direction%s.html";
	static final String STOPS_URL = "http://teuchter.lan:8000/direction%sd%d.html";
	
	LTCScraper(Context c, ScrapingStatus s) {
		db = new BusDb(c);
		status = s;
	}
	
	public void close() {
		db.close();
	}
	
	public void loadAll() {
		LoadTask task = new LoadTask();
		task.execute();
	}
	
	public ArrayList<LTCRoute> loadRoutes() throws ScrapeException, IOException {
		ArrayList<LTCRoute> routes = new ArrayList<LTCRoute>();
		Connection conn = Jsoup.connect(ROUTE_URL);
		Document doc = conn.get();
		Elements routeLinks = doc.select("a.ada");
		Pattern numFinder = Pattern.compile("r=(\\d{1,2})$");
		for (Element routeLink : routeLinks) {
			Attributes attrs = routeLink.attributes();
			String name = attrs.get("title");
			String href = attrs.get("href");
			Matcher m = numFinder.matcher(href);
			if (m.find()) {
				String number = m.group(1);
				LTCRoute route = new LTCRoute(number, name, href);
				routes.add(route);
			}
			else {
				throw new ScrapeException("unrecognized route URL format");
			}
		}
		return routes;
	}
	
	ArrayList<LTCDirection> loadDirections(String routeNum) throws ScrapeException, IOException {
		ArrayList<LTCDirection> directions = new ArrayList<LTCDirection>(2); // probably 2
		String url = String.format(DIRECTION_URL, routeNum);
		Connection conn = Jsoup.connect(url);
		Document doc = conn.get();
		Elements dirLinks = doc.select("a.ada");
		Pattern numFinder = Pattern.compile("d=(\\d{1,2})$");
		for (Element dirLink : dirLinks) {
			Attributes attrs = dirLink.attributes();
			String name = attrs.get("title");
			String href = attrs.get("href");
			Matcher m = numFinder.matcher(href);
			if (m.find()) {
				Integer number = Integer.valueOf(m.group(1));
				LTCDirection dir = new LTCDirection(number, name);
				directions.add(dir);
			}
			else {
				throw new ScrapeException("unrecognized route URL format");
			}
		}
		return directions;

	}
	
	ArrayList<LTCStop> loadStops(String routeNum, int direction) throws ScrapeException, IOException {
		ArrayList<LTCStop> stops = new ArrayList<LTCStop>();
		String url = String.format(STOPS_URL, routeNum, direction);
		Connection conn = Jsoup.connect(url);
		Document doc = conn.get();
		Elements stopLinks = doc.select("a.ada");
		Pattern numFinder = Pattern.compile("s=(\\d+)$");
		for (Element stopLink : stopLinks) {
			Attributes attrs = stopLink.attributes();
			String name = attrs.get("title");
			String href = attrs.get("href");
			Matcher m = numFinder.matcher(href);
			if (m.find()) {
				Integer number = Integer.valueOf(m.group(1));
				LTCStop stop = new LTCStop(number, name);
				stops.add(stop);
			}
			else {
				throw new ScrapeException("unrecognized route URL format");
			}
		}
		return stops;

	}
	
    private class LoadTask extends AsyncTask<Void, LoadProgress, Void> {

    	protected Void doInBackground(Void... thing) {
    		ArrayList<LTCRoute> routes; // all routes
    		// all distinct directions (should only end up with four)
    		HashMap<Integer, LTCDirection> allDirections = new HashMap<Integer, LTCDirection>(4);
    		// all distinct stops
    		HashMap<Integer, LTCStop> allStops = new HashMap<Integer, LTCStop>();
    		// all stops that each route stops at in each direction
    		ArrayList<RouteStopLink> links = new ArrayList<RouteStopLink>();
    		publishProgress(new LoadProgress("Loading route names", 0));
    		try {
    			routes = loadRoutes();
    			if (routes.size() == 0) {
    				publishProgress(new LoadProgress("No routes found", 100));
    			}
    			else {
    				for (LTCRoute route: routes) {
        				publishProgress(new LoadProgress("Loading directions for " + route.name, 100));
        				ArrayList<LTCDirection> routeDirections = loadDirections(route.number);
        				for (LTCDirection dir: routeDirections) {
        					if (!allDirections.containsKey(dir.number)) {
        						allDirections.put(dir.number, dir);
        					}
            				publishProgress(new LoadProgress("Loading stops for " + route.name + " " + dir.name, 100));
        					ArrayList<LTCStop> stops = loadStops(route.number, dir.number);
        					for (LTCStop stop: stops) {
        						if (!allStops.containsKey(stop.number)) {
        							allStops.put(stop.number, stop);
        						}
        						links.add(new RouteStopLink(route.number, dir.number, stop.number));
        					}
        				}
    				}
    				publishProgress(new LoadProgress(String.valueOf(routes.size()) + " routes "
    						+ String.valueOf(allDirections.size()) + " directions "
    						+ String.valueOf(allStops.size()) + " stops "
    						+ String.valueOf(links.size() + " links"), 100));
    				db.saveBusData(routes, allDirections.values(), allStops.values(), links);
    			}
    		}
    		catch (IOException e) {
    			publishProgress(new LoadProgress(e.getMessage(), -1));
    		}
    		catch (ScrapeException e) {
    			publishProgress(new LoadProgress(e.getMessage(), -1));
    		}
    		catch (SQLiteException e) {
    			publishProgress(new LoadProgress(e.getMessage(), -1));
    		}

    		return null;
        }

        protected void onProgressUpdate(LoadProgress... progress) {
            status.update(progress[0]);
        }

    }

}
