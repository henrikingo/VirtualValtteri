package com.virtualvaltteri.vmkarting;

public class DriverState {
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
    public String id;
    public String rank;
    public String carNr;
    public String name;
    public String S1;
    public String S2;
    public String lastLap;
    public String bestLap;
    public String gap;
    public String laps;
    public String lapTimes[];
    public String S1Times[];
    public String S2Times[];

    public String toString(){
        return String.format("%s: %s %s", id, carNr, name) ;
    }

}
