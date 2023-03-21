package com.virtualvaltteri.vmkarting;

import com.virtualvaltteri.sensors.RaceEventSensor;
import com.virtualvaltteri.sensors.SensorWrapper;
import com.virtualvaltteri.speaker.Speaker;
import com.virtualvaltteri.speaker.VeryShort;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.virtualvaltteri.sensors.Collect;

public class MessageHandler {
    /**
     * Translate messages from vmkarting live data stream websocket into English.
     * @param message One or more rows, where an individual row might look like `init|p|`
     * @return English words to be spoken to the driver, like "New practice session".
     */
    public HashMap<String, DriverState> driverLookup;
    public HashMap<String, String> driverIdLookup;
    public Set<String> followDriverNames;
    private String sessionType = "Session";
    private String latestDriver = "";
    private Collect collect;
    private RaceEventSensor raceEventSensor;

    public MessageHandler(Set<String> followDriverNames, Collect collect){
        driverLookup = new HashMap<String, DriverState>();
        driverIdLookup = new HashMap<String, String>();
        this.followDriverNames = followDriverNames;
        this.collect=collect;
    }
    public Speaker speaker = new VeryShort();

    private DriverState getDriverState(String driverId, Map<String, String> englishMessageMap) {
        DriverState d = driverLookup.get("r" + driverId);
        if (latestDriver != null && !latestDriver.equals(d.id)) {
            latestDriver = d.id;
            englishMessageMap.put("driverChanged", "true");
            raceEventSensor.triggerEvent("driver", d.carNr, "r"+driverId, d.name);
        }
        return d;
    }
    public Map<String, String> message(String message) {
        //System.out.println(message);
        Map<String,String> englishMessageMap = new HashMap<>();
        StringBuilder englishMessage = new StringBuilder("");
        String[] lines = message.split("\n");

        for (String line : lines) {
            String[] parts = line.split("\\|");
            if (parts.length >= 2) {
                String command = parts[0];
                String argument = parts[1];
                System.out.println(line);// + "\n" + command + ", " + argument + ", " + (parts.length >= 3 ? parts[2]: ""));
                switch (command) {
                    case "init":
                        englishMessage.append(speaker.init(argument));
                        if (argument.equals("p")) {
                            sessionType = "Session";
                        } else if (argument.equals("r")) {
                            sessionType = "Race";
                        } else {
                            sessionType = "Session";
                        }
                        englishMessageMap.put("sessionType", sessionType);

                        collect.raceStarted();
                        this.raceEventSensor = (RaceEventSensor) collect.getSensor(SensorWrapper.TYPE_RACE_EVENT);
                        raceEventSensor.triggerEvent("init", sessionType);


                        break;
                    case "title1":
                    case "title2":
                        // Session title
                        if (parts.length >= 3) {
                            englishMessage.append(speaker.title(parts[2])).append(" ");
                            englishMessageMap.put(command, parts[2]);
                        }
                        break;
                    case "grid":
                        // This is a complex HTML table that initializes the live resutls web page.
                        // However, it does contain valuable information about the drivers, in particular their names.
                        driverLookup.clear();
                        driverIdLookup.clear();
                        parseInitHtml(parts[2]);
                        System.out.println(driverIdLookup.toString());
                        System.out.println(driverLookup.toString());
                        englishMessageMap.put("driversCount", "" + driverIdLookup.keySet().size());
                        break;
                    case "com":
                        if(parts.length >= 3 && parts[2].contains("<span data-flag=\"chequered\"></span>Finish")){
                            englishMessage.append(speaker.finish(sessionType));
                            englishMessageMap.put("finish", sessionType);
                            DriverState dd = driverLookup.get(latestDriver);
                            if(dd!=null)
                                raceEventSensor.triggerEvent("finish", sessionType, dd.carNr, dd.bestLap);
                            else
                                raceEventSensor.triggerEvent("finish", sessionType);
                            collect.raceStopped();
                        }

                        // Can't clear the lists this early. Sector times keep dropping in after the cheqcuered flag.
                        // And if we don't have the lists, you can no longer select a name, and then it just reads all the.
                        // times without knowing the driver they belong to.
                        // driverLookup.clear();
                        // driverIdLookup.clear();
                        break;
                }
                if (command.startsWith("r")){
                    //System.out.println(command);
                    Pattern justDriverPattern = Pattern.compile("r(\\d+)");
                    Pattern driverAndCPattern = Pattern.compile("r(\\d+)c(\\d+)");
                    Matcher justDriverMatcher = justDriverPattern.matcher(command);
                    Matcher driverAndCMatcher = driverAndCPattern.matcher(command);
                    //System.out.println("match: "+justDriverMatcher.matches() + " " + driverAndCMatcher.matches());
                    if(justDriverMatcher.matches()){
                        String driverId = justDriverMatcher.group(1);
                        DriverState d = getDriverState(driverId, englishMessageMap);
                        if(argument.equals("#") && followThisDriver(d)){
                            d.rank = parts[2];
                            String newMessage = speaker.position(d.rank, d);
                            englishMessage.append(newMessage);
                            raceEventSensor.triggerEvent("driver", "position", d.carNr, d.name, d.rank);
                        }
                    }
                    if(driverAndCMatcher.matches()){
                        String driverId = driverAndCMatcher.group(1);
                        String c = driverAndCMatcher.group(2);
                        DriverState d = getDriverState(driverId, englishMessageMap);

                        if(argument.equals("ib") || argument.equals("in"))
                            continue;

                        if(!englishMessageMap.containsKey("carNr") && d!=null)
                            englishMessageMap.put("carNr", d.carNr);

                        if((c.equals("8")||c.equals("9")) && followThisDriver(d)){
                            englishMessage.append(speaker.lap(parts[2], d));
                            englishMessageMap.put("lap", parts[2]);
                            englishMessageMap.put("carNr", d.carNr);
                            // s2 and lap time come together. We need to unset whatever was there for s2.
                            englishMessageMap.remove("time_meta");
                            setTimeMeta(argument, d, englishMessageMap, englishMessage);
                            raceEventSensor.triggerEvent("driver", "lap", d.carNr, d.name, parts[2]);
                        }
                        if(c.equals("6") && followThisDriver(d)){
                            englishMessage.append(speaker.sector("1", parts[2], d));
                            if(d.carNr.equals(englishMessageMap.get("carNr"))) {
                                englishMessageMap.put("s1", parts[2]);
                                englishMessageMap.put("carNr", d.carNr);
                                raceEventSensor.triggerEvent("driver", "s1", d.carNr, d.name, parts[2]);
                            }
                            setTimeMeta(argument, d, englishMessageMap, englishMessage);
                        }
                        if(c.equals("7") && followThisDriver(d)){
                            englishMessage.append(speaker.sector("2", parts[2], d));
                            englishMessageMap.put("s2", parts[2]);
                            englishMessageMap.put("carNr", d.carNr);
                            setTimeMeta(argument, d, englishMessageMap, englishMessage);
                            raceEventSensor.triggerEvent("driver", "s2", d.carNr, d.name, parts[2]);
                        }
                        if(c.equals("10") && parts.length>2 && followThisDriver(d)){
                            englishMessage.append(speaker.gap(parts[2], d));
                            englishMessageMap.put("gap", parts[2]);
                            englishMessageMap.put("carNr", d.carNr);
                            setTimeMeta(argument, d, englishMessageMap, englishMessage);
                        }
                        if(d.rank != null && !d.rank.equals(""))
                            if( !englishMessageMap.containsKey("carNr") || d.carNr.equals(englishMessageMap.get("carNr")))
                                englishMessageMap.put("position", d.rank);
                    }
                }
            }
        }
        System.out.println(englishMessage);
        englishMessageMap.put("message", englishMessage.toString());
        System.out.println(englishMessageMap);
        return englishMessageMap;
    }
    private void setTimeMeta(String argument, DriverState d, Map<String, String> englishMessageMap, StringBuilder englishMessage){
        if(followThisDriver(d)){
            switch (argument){
                case "tn":
                    englishMessageMap.put("time_meta", "");
                    break;
                case "ti":
                    englishMessageMap.put("time_meta", "improved");
                    englishMessage.append(speaker.time_meta("improved"));
                    break;
                case "tb":
                    englishMessageMap.put("time_meta", "best");
                    englishMessage.append(speaker.time_meta("best"));
                    break;
/*
                case "ib":
                    // If we already have best time overall, don't need to also say it's personal best
                    if(!englishMessageMap.containsKey("time_meta") || !englishMessageMap.get("time_meta").equals("best")){
                        englishMessageMap.put("time_meta", "individual best");
                        englishMessage.append(speaker.time_meta("individual best"));
                    }
                    break;

 */
            }
        }
    }
    private boolean followThisDriver(DriverState d){
        // If no filter specified, just read out everything
        if (d==null || followDriverNames.isEmpty())
            return true;

        for(String driverName: followDriverNames){
            if(d.name.startsWith(driverName))
                return true;
        }
        return false;
    }

    private void parseInitHtml(String html){
        String validHtml = "<html><head></head><body><table>"+html+"</table></body></html>";
        Document table = Jsoup.parse(validHtml);
        //System.out.println("JSOUP: "+table);
        Elements rows = table.getElementsByTag("tr");
        int rowcount = 0;
        //System.out.println("html table rows: "+rows);
        for (Element row: rows) {
            rowcount++;
            // Skip first row, we will boldly hard code the headers into an enum
            /*
                <tr data-id="r0" class="head" data-pos="0">
                <td data-id="c1" data-type="grp" data-pr="6"></td>
                <td data-id="c2" data-type="sta" data-pr="1"></td>
                <td data-id="c3" data-type="rk" data-pr="1">Rnk</td>
                <td data-id="c4" data-type="no" data-pr="1">Kart</td>
                <td data-id="c5" data-type="dr" data-pr="1" data-width="25" data-min="16">Driver</td>
                <td data-id="c6" data-type="s1" data-pr="3" data-width="11" data-min="6">S1</td>
                <td data-id="c7" data-type="s2" data-pr="3" data-width="11" data-min="6">S2</td>
                <td data-id="c8" data-type="llp" data-pr="2" data-width="16" data-min="7">Last lap</td>
                <td data-id="c9" data-type="blp" data-pr="1" data-width="16" data-min="7">Best lap</td>
                <td data-id="c10" data-type="gap" data-pr="4" data-width="11" data-min="7">Gap</td>
                <td data-id="c11" data-type="tlp" data-pr="5" data-width="6" data-min="4">Laps</td>
                </tr>
             */
            if (rowcount == 1){
                continue;
            }
            DriverState driver = new DriverState();
            driver.id = row.attr("data-id");
            Elements c4 = row.select("[data-id="+driver.id+"c4]");
            driver.carNr = c4.first().text();
            driver.name = row.select("[data-id="+driver.id+"c5]").first().text().toLowerCase();
            driver.rank = row.select("[data-id="+driver.id+"c3]").first().text().toLowerCase();
            driverIdLookup.put(driver.name, driver.id);
            driverLookup.put(driver.id, driver);
        }
    }

}
