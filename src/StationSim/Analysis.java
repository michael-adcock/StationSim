/* Created by Micahel Adcock on 17/04/2018.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package StationSim;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Bag;
import sim.util.Double2D;


import java.io.*;
import java.time.LocalDateTime;
import java.util.*;


/** Class containing various method for analysis of the simulation.
 *  Implements Steppable and is added to the schedule as an agent.
 */
public class Analysis implements Steppable {
    private static final long serialVersionUID = 1;

    // parameters to divide continuous space into grid
    private int numRows = 20;
    private int numCols = 40;
    int[][] occupancyMatrix = new int[numRows][numCols];
    int[][] temporalOccupancyMatrix = new int[numRows][numCols];


    private List<List<String>> stateDataFrame;
    private List<List<String>> aggregateDataFrame;

    private int writeInterval = 50;
    private int TemporalFileNumber = 0;
    //private int stateFileNumber;


    Station station;

    public Analysis(Station station) {
        this.station = station;
        List<String> header;

        // Set up data frame
        stateDataFrame = new ArrayList<>();

        // Set up aggregateDataFrame
        aggregateDataFrame = new ArrayList<>();


        // Set up alternative to using data frame
        //createStateFile();
        //stateFileNumber = 1;
    }

    /**
     * Calls analysis methods each step and end the simulation when
     * all people have exited
     *
     * @param state
     */
    public void step(SimState state) {
        station = (Station) state;
        updateOccupancy();
        //writeTemporalOccupancy();
        updateStateDataFrame();
        updateAggregateDataFrame();
        //writeState(false);
        // End simulation when all people have left
        if (station.area.getAllObjects().size() == 0 && station.getWriteResults() == true) {
            System.out.printf("Writing data out");
            long sysTime = System.currentTimeMillis();
            writeParameters("parameters_" + sysTime + ".html");
            System.out.printf(".");
            writeAverageOccupancy();
            System.out.printf(".");
            writeDataFrame(stateDataFrame, "state_data" + sysTime + ".txt",
                    "step,agent,x_pos,y_pos,speed,exit");

            System.out.printf(".");
            writeDataFrame(aggregateDataFrame, "aggregate_data" + sysTime + ".txt",
                    "step,mean_speed,min_speed,max_speed,num_of_people");
            System.out.printf(".");
            station.finish();
            System.out.println("Finished!");
        }
    }

    /**
     * Increment each occupancy matrix position by the number of
     * people in a grid space for a step.
     */
    public void updateOccupancy() {
        int i, j;
        Bag people = station.area.getAllObjects();
        for (int x = 0; x < people.size(); x++) {
            Double2D location = ((Person) people.get(x)).getLocation();
            i = (int) (location.getY() / (station.area.getHeight() / numRows));
            j = (int) (location.getX() / (station.area.getWidth() / numCols));
            if (j >= numCols) {
                j = numCols - 1;
            }
            occupancyMatrix[i][j]++;
        }
    }

    /**
     * Calculate the average number of people in each grid space
     * over the course of the simulation and print the result as a
     * matrix to the terminal.
     */
    public void printAverageOccupancy() {
        long steps = station.schedule.getSteps();
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                System.out.printf(occupancyMatrix[i][j] / (double) steps + "");
                if (j != numCols - 1) {
                    System.out.printf(",");
                }
            }
            System.out.printf("\n");
        }
        System.out.println("\n");
    }

    /**
     * Calculate the average number of people in each grid space
     * over the course of the simulation and write the result as a
     * matrix to the csv.
     */
    public void writeAverageOccupancy() {
        Writer writer = null;
        long steps = station.schedule.getSteps();

        String dirName = "simulation_outputs";
        File dir = new File(dirName);
        if (!dir.exists()) {
            dir.mkdir();
        }

        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(dirName + "/average_occupancy.txt"), "utf-8"));
            for (int i = 0; i < numRows; i++) {
                for (int j = 0; j < numCols; j++) {
                    writer.write(occupancyMatrix[i][j] / (double) steps + "");
                    if (j != numCols - 1) {
                        writer.write(",");
                    }
                }
                writer.write("\n");
            }
        } catch (IOException ex) {
            System.out.println("Error writing to file");
        } finally {
            try {
                writer.close();
            } catch (Exception ex) {
                System.out.println("Error closing file");
            }
        }
    }

    /**
     * Calculate the average number of people in each grid space
     * of the course of a step interval in the simulation
     * and write the result as a matrix to the csv.
     */
    public void writeTemporalOccupancy() {
        String dirName = "simulation_outputs";
        File dir = new File(dirName);
        if (!dir.exists()) {
            dir.mkdir();
        }

        if (station.schedule.getSteps() % writeInterval == 0) {
            Writer writer = null;

            try {
                writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(dirName + "/average_occupancy_timepoint_" + TemporalFileNumber + ".txt"),
                        "utf-8"));
                for (int i = 0; i < numRows; i++) {
                    for (int j = 0; j < numCols; j++) {
                        writer.write(temporalOccupancyMatrix[i][j] / (double) writeInterval + "");
                        if (j != numCols - 1) {
                            writer.write(",");
                        }
                    }
                    writer.write(System.lineSeparator());
                }
            } catch (IOException ex) {
                System.out.println("Error writing to file");
            } finally {
                try {
                    writer.close();
                } catch (Exception ex) {
                    System.out.println("Error closing file");
                }
            }
            temporalOccupancyMatrix = new int[numRows][numCols];
            TemporalFileNumber += 1;
        }

        int i, j;
        Bag people = station.area.getAllObjects();
        for (int x = 0; x < people.size(); x++) {
            Double2D location = ((Person) people.get(x)).getLocation();
            i = (int) (location.getY() / (station.area.getHeight() / numRows));
            j = (int) (location.getX() / (station.area.getWidth() / numCols));
            if (j >= numCols) {
                j = numCols - 1;
            }
            temporalOccupancyMatrix[i][j]++;
        }
    }


    /**
     * Write out the parameters used to html
     */
    public void writeParameters(String fileName) {
        String dirName = "simulation_outputs";
        File dir = new File(dirName);
        if (!dir.exists()) {
            dir.mkdir();
        }

        Writer writer = null;
        long steps = station.schedule.getSteps();
        LocalDateTime now = LocalDateTime.now();
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(dirName + "/" + fileName), "utf-8"));
            writer.write("<html><head><title>Parameters</title></head><body><p><h2>Parameter Settings</h2>");
            writer.write("<h3>" + "Simulation run on " +
                    now.getDayOfWeek() + " " + now.getDayOfMonth() + " " + now.getMonth() + " " + now.getYear() +
                    " at " + now.getHour() + ":" + now.getMinute() + ":" + now.getSecond() +
                    "</h3>");
            writer.write("<p>" + "Person Size : " + station.getPersonSize() + "</p>");
            writer.write("<p>" + "Number of People: " + station.getNumPeople() + "</p>");
            writer.write("<p>" + "Number of Entrances: " + station.getNumEntrances() + "</p>");
            writer.write("<p>" + "Size of Entrances: " + station.getEntranceSize() + "</p>");
            writer.write("<p>" + "Number of Exits: " + station.getNumExits() + "</p>");
            writer.write("<p>" + "Size of Exits: " + station.getExitSize() + "</p>");
            writer.write("<p>" + "Exit choice probability: " + station.getExitProb() + "</p>");
            writer.write("<p>" + "Person Size : " + station.getPersonSize() + "</p>");
            writer.write("<p>" + "Seed : " + station.seed() + "</p>");
            writer.write("<h3>" + "Entrance to exit assignments" + "</h3>");
            Bag entrances = station.getEntrances();
            for (int i = 0; i < entrances.size(); i++) {
                writer.write("<p>" + entrances.get(i) + " - " + ((Entrance) entrances.get(i)).getExit() + "</p>");
            }
            writer.write("</body></html>");


        } catch (IOException ex) {
            System.out.println("Error writing to file");
        } finally {
            try {
                writer.close();
            } catch (Exception ex) {
                System.out.println("Error closing file");
            }
        }
    }

    public void updateStateDataFrame() {
        Person person;
        List<String> row;
        long step = station.schedule.getSteps();

        Bag people = station.area.getAllObjects();
        for (int i = 0; i < people.size(); i++) {
            person = (Person) people.get(i);
            row = Arrays.asList(
                    Long.toString(step),
                    person.toString(),
                    Double.toString(person.getLocation().getX()),
                    Double.toString(person.getLocation().getY()),
                    Double.toString(person.getCurrentSpeed()),
                    person.getExit().toString()
            );
            stateDataFrame.add(row);
        }
    }

    public void updateAggregateDataFrame() {
        Bag people = station.area.getAllObjects();
        double speed;
        double minSpeed = Double.POSITIVE_INFINITY;
        double maxSpeed = 0.0;
        double speedSum = 0.0;
        long numPeople = people.size();
        for (int i = 0; i < numPeople; i++) {
            speed = ((Person) people.get(i)).getCurrentSpeed();
            speedSum += speed;
            if (speed > maxSpeed) {
                maxSpeed = speed;
            }
            if (speed < minSpeed) {
                minSpeed = speed;
            }
        }

        aggregateDataFrame.add(
                Arrays.asList(
                        Long.toString(station.schedule.getSteps()),
                        Double.toString(speedSum / numPeople),
                        Double.toString(minSpeed),
                        Double.toString(maxSpeed),
                        Long.toString(numPeople))
        );
    }

    public void writeDataFrame(List<List<String>> dataFrame, String fileName, String header) {

        String dirName = "simulation_outputs";
        File dir = new File(dirName);
        if (!dir.exists()) {
            dir.mkdir();
        }

        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(dirName + "/" + fileName),
                    "utf-8"));

            writer.write(header + System.lineSeparator());
            for (List<String> line : dataFrame) {
                writer.write(String.join(",", line) + System.lineSeparator());
            }
        } catch (IOException ex) {
            System.out.println("Error writing to file");
        } finally {
            try {
                writer.close();
            } catch (Exception ex) {
                System.out.println("Error closing file");
            }
        }
    }

    public Map<Integer, Integer> getGridDensity(Double minX, Double maxX, Double minY, Double maxY) {

        List<List<String>> subset = new ArrayList<>();
        for (List<String> line : stateDataFrame) {
            if (Double.parseDouble(line.get(2)) > minX &&
                    Double.parseDouble(line.get(2)) < maxX &&
                    Double.parseDouble(line.get(3)) > minY &&
                    Double.parseDouble(line.get(3)) < maxY) {
                subset.add(line);
            }
        }

        Map<Integer, Integer> gridDensity = new HashMap<>();

        int count = 0;
        int step = Integer.parseInt(subset.get(0).get(0));
        int currentStep;
        for (List<String> line : subset) {
            currentStep = Integer.parseInt(line.get(0));
            if (step == currentStep) {
                count++;
            } else {
                gridDensity.put(step, count);
                step = currentStep;
                count = 1;
            }
        }
        gridDensity.put(step, count);

        return gridDensity;

    }
}