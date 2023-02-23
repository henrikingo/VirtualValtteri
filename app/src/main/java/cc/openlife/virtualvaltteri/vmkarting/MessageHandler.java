package cc.openlife.virtualvaltteri.vmkarting;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.openlife.virtualvaltteri.speaker.Speaker;
import cc.openlife.virtualvaltteri.speaker.VeryShort;

public class MessageHandler {
    /**
     * Translate messages from vmkarting live data stream websocket into English.
     * @param message One or more rows, where an individual row might look like `init|p|`
     * @return English words to be spoken to the driver, like "New practice session".
     */
    HashMap<String, DriverState> driverLookup;
    HashMap<String, String> driverIdLookup;
    public Set<String> followDriverNames;
    private String sessionType = "Session";
    public MessageHandler(Set<String> followDriverNames){
        driverLookup = new HashMap<String, DriverState>();
        driverIdLookup = new HashMap<String, String>();
        this.followDriverNames = followDriverNames;
    }
    private Speaker speaker = new VeryShort();

    public String message(String message) {
        //System.out.println(message);
        StringBuilder englishMessage = new StringBuilder("");
        String[] lines = message.split("\n");

        for (String line : lines) {
            String[] parts = line.split("\\|");
            if (parts.length >= 2) {
                String command = parts[0];
                String argument = parts[1];
//                System.out.println("                 " + command + ", " + argument);
                switch (command) {
                    case "init":
                        englishMessage.append(speaker.init(argument));
                        if (argument.equals("p")) {
                            sessionType = "Practice session";
                        } else if (argument.equals("r")) {
                            sessionType = "Race";
                        } else {
                            sessionType = "Session";
                        }
                        break;
                    case "title1":
                    case "title2":
                        // Session title
                        if (parts.length >= 3) {
                            englishMessage.append(speaker.title(parts[2])).append("\n");
                        }
                        break;
                    case "grid":
                        // This is a complex HTML table that initializes the live resutls web page.
                        // However, it does contain valuable information about the drivers, in particular their names.
                        parseInitHtml(parts[2]);
                        System.out.println(driverIdLookup.toString());
                        System.out.println(driverLookup.toString());
                        break;
                    case "com":
                        if(parts.length >= 3 && parts[2].contains("<span data-flag=\"chequered\"></span>Finish")){
                            englishMessage.append(speaker.finish(sessionType));
                        }
                        //Free up resources
                        //Note that the driverId is constant through the day. In the future we may want to keep the drivers in the follow list.
                        driverLookup.clear();
                        driverIdLookup.clear();
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
                        DriverState d = driverLookup.get("r"+driverId);
                        // Don't repeat driver name for each statistic. Just say it when it changes.
                        if(argument.equals("#") && followThisDriver(d)){
                            String newMessage = speaker.position(parts[2], d);
                            englishMessage.append(newMessage);
                        }
                    }
                    if(driverAndCMatcher.matches()){
                        String driverId = driverAndCMatcher.group(1);
                        String c = driverAndCMatcher.group(2);
                        DriverState d = driverLookup.get("r"+driverId);
                        String driverEnglish = "";
                        if(c.equals("8") && followThisDriver(d)){
                            englishMessage.append(speaker.lap(parts[2], d));
                        }
                        if(c.equals("6") && followThisDriver(d)){
                            englishMessage.append(speaker.sector("1", parts[2], d));
                        }
                        if(c.equals("7") && followThisDriver(d)){
                            englishMessage.append(speaker.sector("2", parts[2], d));
                        }
                        if(c.equals("10") && parts.length>2 && followThisDriver(d)){
                            englishMessage.append(speaker.gap(parts[2], d));
                        }
                    }
                }
            }
        }
        System.out.println(englishMessage);
        return englishMessage.toString();
    }

    private boolean followThisDriver(DriverState d){
        // If no filter specified, just read out everything
        if (d!=null && followDriverNames.isEmpty())
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
            driverIdLookup.put(driver.name, driver.id);
            driverLookup.put(driver.id, driver);
        }
    }
}
