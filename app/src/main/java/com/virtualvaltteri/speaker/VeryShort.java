package com.virtualvaltteri.speaker;

import com.virtualvaltteri.vmkarting.DriverState;

public class VeryShort extends Speaker {
    final public String type = "VeryShort";
    @Override
    public String title(String words) {
        return "";
    }

    @Override
    public String driver(DriverState d) {
            return "";
    }

    public String position(String position, DriverState d) {
        return String.format("P %s ", position);
    }
    @Override
    public String time_meta(String meta) {return "";}
    public String lap(String time, DriverState d) {
        return String.format("%s. ", cutDecimal(time));
    }
    public String sector(String sectorNr, String time, DriverState d) {
        return String.format("%s. ", cutDecimal(time));
    }
    public String gap(String time, DriverState d) {
        return String.format("%s gap %s. ", driver(d), cutDecimal(time));
    }
    public String rank(String time, DriverState d) {
        return String.format("%s rank %s. ", driver(d), cutDecimal(time));
    }
}
