package com.totalcross.knowcode;

import totalcross.io.LineReader;
import totalcross.io.Stream;
import totalcross.sys.Vm;

public class ReadSensors {

    public String readTemp() {

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

        return input;
    }

}