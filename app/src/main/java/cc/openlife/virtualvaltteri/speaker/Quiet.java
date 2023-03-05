package cc.openlife.virtualvaltteri.speaker;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.openlife.virtualvaltteri.vmkarting.DriverState;

public class Quiet extends Speaker {
    public String init(String argument) {
        return "";
    }
    public String title(String words) {
        return "";
    }

    public String time_meta(String meta) {
        return "";
    }

    public String finish(String sessionType) {
        return "";
    }

    public String driver(DriverState d) {
        return "";
    }
    public String position(String position, DriverState d) {
        return "";
    }
    public String lap(String time, DriverState d) {
        return "";
    }
    public String sector(String sectorNr, String time, DriverState d) {
        return "";
    }
    public String gap(String time, DriverState d) {
        return "";
    }
}
