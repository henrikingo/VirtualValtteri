package cc.openlife.virtualvaltteri.speaker;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.openlife.virtualvaltteri.vmkarting.DriverState;

public class Speaker {
    protected String pos(String position){
        switch (position){
            case "1":
                return "1st";
            case "2":
                return "2nd";
            case "3":
                return "3rd";
            default:
                return position + "th";
        }
    }
    protected String cutDecimal(String d){
        int dot = d.indexOf(".");
        if (dot >= 1){
            return d.substring(0,dot+2);
        }
        return d;
    }
    public String init(String argument) {
        if (argument.equals("p")) {
            return "Practice session starting.\n";
        } else if (argument.equals("r")) {
            return "Race starting!\n";
        } else {
            return "New session.\n";
        }
    }
    public String title(String words) {
        return words;
    }

    public String finish(String sessionType) {
        return String.format("%s finished.\n", sessionType);
    }

    private String latestDriver = "";
    public String driver(DriverState d) {
        String driverString =  String.format("Car %s %s ", d.carNr, d.name);
        if(!latestDriver.equals(d.id)) {
            latestDriver = d.id;
            return String.format("Car %s %s ", d.carNr, d.name);
        }
        else {
            return "";
        }
    }
    public String position(String position, DriverState d) {
        return String.format("%s in %s position.\n", driver(d), pos(position));
    }
    public String lap(String time, DriverState d) {
        return String.format("%s lap %s.\n", driver(d), cutDecimal(time));
    }
    public String sector(String sectorNr, String time, DriverState d) {
        return String.format("%s %s sector %s.\n", driver(d), pos(sectorNr), cutDecimal(time));
    }
    public String gap(String time, DriverState d) {
        return String.format("%s gap %s.\n", driver(d), cutDecimal(time));
    }
}
