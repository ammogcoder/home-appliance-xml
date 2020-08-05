package com.totalcross.knowcode;

import totalcross.io.LineReader;
import totalcross.io.Stream;
import totalcross.sys.Convert;
import totalcross.sys.InvalidNumberException;
import totalcross.sys.Settings;
import totalcross.sys.Vm;

public class ReadSensors {

    public String readTemp() {

        if (!Settings.platform.equalsIgnoreCase("linux_arm"))
            return "18";

        // Process initialization
        Process process = null;

        // Input from program
        String input = "error";

        try {
            Vm.debug("starting read sensor");
            process = Runtime.getRuntime().exec("python3 dht.py");
            process.getOutputStream().write("".getBytes(), 0, "".getBytes().length); // write output into

            process.waitFor();

            LineReader lineReader = new LineReader(Stream.asStream(process.getInputStream()));
            input = lineReader.readLine();

            Vm.debug(input);
            Vm.debug("finishing read sensor");

        } catch (

        java.io.IOException e) {
            e.printStackTrace();
            input = "40";
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return validaTemp(input);
    }

    private String validaTemp(String temp) {
        try {
            if (temp == null || temp.equalsIgnoreCase("error") || temp.trim().equals("") || Convert.toInt(temp) < 20)
                return null;
        } catch (InvalidNumberException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return temp;
    }

}