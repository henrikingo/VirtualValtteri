package cc.openlife.virtualvaltteri.speaker;

import cc.openlife.virtualvaltteri.vmkarting.DriverState;

public class VeryShort extends Speaker {
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
