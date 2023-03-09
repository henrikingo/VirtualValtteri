package com.virtualvaltteri.speaker;

import com.virtualvaltteri.vmkarting.DriverState;

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
            return "Session starting. ";
        } else if (argument.equals("r")) {
            return "Race starting! ";
        } else {
            return "New session. ";
        }
    }
    public String title(String words) {
        return words;
    }

    public String time_meta(String meta) {
        if(meta.equals("improved"))
            return "Time improved.";
        if(meta.equals("individual best"))
            return "Personal best time for this session.";
        if(meta.equals("best"))
            return "Best time for this session.";
        return "";
    }

    public String finish(String sessionType) {
        return String.format("%s finished.\n", sessionType);
    }

    private String latestDriver = "";
    public String driver(DriverState d) {
        if (d==null)
            return "";

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
        return String.format("%s in %s position. ", driver(d), pos(position));
    }
    public String lap(String time, DriverState d) {
        return String.format("%s lap %s. ", driver(d), cutDecimal(time));
    }
    public String sector(String sectorNr, String time, DriverState d) {
        return String.format("%s %s sector %s. ", driver(d), pos(sectorNr), cutDecimal(time));
    }
    public String gap(String time, DriverState d) {
        return String.format("%s gap %s. ", driver(d), cutDecimal(time));
    }
}
