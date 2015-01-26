/*
 * Copyright 2014 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.realm.entities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Thread;
import java.text.DecimalFormat;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;

public class TimeMeasurement {

    private String time_Unit = "ns";

    //Size of data for testing.
    public static final int DATA_SIZE = 1000;

    private DecimalFormat decimalFormat = new DecimalFormat("##.###");

    //Clears data from realm.
    public void clearRealm(Realm testRealm) {
        testRealm.beginTransaction();
        testRealm.clear(Performance.class);
        testRealm.commitTransaction();
    }

    //Creates data for realm with argument size
    public void addObjectToTestRealm(int objects, Realm testRealm) {
        testRealm.beginTransaction();
        for (int i = 0; i < objects; ++i) {
            Performance performance = testRealm.createObject(Performance.class);
            performance.setString("test data " + i);
            performance.setString_index("index data " + i);
            performance.setInteger(i);
        }
        testRealm.commitTransaction();
    }

    //Sets up data. Measures timing for method. Clears Data.
    public void timer(String name, Realm testRealm, int times_to_warm_up, int times_to_execute,
                      TimeUnit timeUnit, ExecutePerformance executePerformance) {
        setTimeUnit(timeUnit);
        String fileName_test = name + "_in_" + time_Unit;
        String fileName_warm_up = "warm_up_" + name + "_in_" + time_Unit;
        deleteFile(fileName_test);
        deleteFile(fileName_warm_up);

        for (int i = 0; i < times_to_execute + times_to_warm_up; i++) {
            addObjectToTestRealm(DATA_SIZE, testRealm);
            long start = System.nanoTime();
            executePerformance.execute();
            long stop = System.nanoTime();
            double time = ((double) (stop - start) / 1.0);
            if (time != 0) {
                if (timeUnit != TimeUnit.NANOSECONDS) {
                    time = timeConverting(stop, start, timeUnit);
                }

                if (i < times_to_warm_up) {
                    write(fileName_warm_up, decimalFormat.format(time));
                } else {
                    write(fileName_test, decimalFormat.format(time));
                }
            } else {
            }
            clearRealm(testRealm);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        setStatistics(getFile(fileName_test), name);
    }

    //Creates and writes to file
    public void write(String fileName, String content) {
        try {
            String file_path = "/data/data/io.realm.test/files/" + fileName + ".txt";
            File file = new File(file_path);

            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.newLine();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Deletes file.
    public void deleteFile(String fileName) {
        String file_path = "/data/data/io.realm.test/files/" + fileName + ".txt";
        File file = new File(file_path);
        file.delete();
    }

    //Fetches file.
    public File getFile(String fileName) {
        File file = new File("/data/data/io.realm.test/files/" + fileName + ".txt");
        return file;
    }

    //Converts tests time to other time unit.
    public double timeConverting(long stop, long start, TimeUnit timeUnit) {
        double time = 0;
        switch (timeUnit) {
            case MICROSECONDS: {
                 time = (double)(stop - start) / 1000.0;
            }
            break;
            case MILLISECONDS: {
                time = ((double) stop / 1000000.0) - ((double) start/ 1000000.0);
            }
            break;
            case SECONDS: {
                time = (double)(stop - start) / 1000000000.0;
            }
            break;

            default:
                break;
        }
        return time;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        switch (timeUnit) {
            case MICROSECONDS: {
                time_Unit = "μs";
            }
            break;
            case MILLISECONDS: {
                time_Unit = "ms";
            }
            break;
            case SECONDS: {
                time_Unit = "s";
            }
            break;
            default:
                break;
        }

    }

    //Finds fastest time.
    public double minimum(File file) {
        double current = 0;
        double min = 0;
        try {
            Scanner scanner = new Scanner(file);
            try {
                min = Double.valueOf(scanner.nextLine());
                while (scanner.hasNextLine()) {
                    current = Double.valueOf(scanner.nextLine());
                    if (current < min) {
                        min = current;
                    }
                }
            } catch (Exception e) {
            }

        } catch (FileNotFoundException e) {
        }

        return min;
    }

    //Finds longest time.
    public double maximum(File file) {
        double current = 0;
        double max = 0;
        try {
            Scanner scanner = new Scanner(file);
            max = Double.valueOf(scanner.nextLine());
            try {
                max = Double.valueOf(scanner.nextLine());
                while (scanner.hasNextLine()) {
                    current = Double.valueOf(scanner.nextLine());
                    if (current > max) {
                        max = current;
                    }
                }
            } catch (Exception e) {
            }
        } catch (FileNotFoundException e) {

        }
        return max;
    }

    //Calculates average time.
    public double average(File file) {
        double sum = 0;
        int count = 0;
        try {
            Scanner scanner = new Scanner(file);
            try {
                while (scanner.hasNextLine()) {
                    sum += Double.valueOf(scanner.nextLine());
                    count++;
                }
            } catch (Exception e) {
            }
        } catch (FileNotFoundException e) {

        }
        return sum / count;
    }

    //Calculates variance.
    public double variance(File file) {
        double avg = average(file);
        double temp = 0;
        double current = 0;
        int count = 0;
        try {
            Scanner scanner = new Scanner(file);
            try {
                while (scanner.hasNextLine()) {
                    current = Double.valueOf(scanner.nextLine());
                    temp += ((avg - current) * (avg - current));
                    count++;
                }
            } catch (Exception e) {
            }
        } catch (FileNotFoundException e) {

        }
        return temp / count;
    }

    //Calculates standard deviation.
    public double stdDev(File file) {
        return Math.sqrt(variance(file));
    }

    //Calculates the highest percent difference.
    public double minMaxPercentDifference(File file) {
        double min = minimum(file);
        double max = maximum(file);
        double percent = ((max - min) / min) *100.0;

        return percent;
    }

    //Write Statistics to file.
    public void setStatistics(File file, String name) {
        String fileName = "Statistics_for_" + name + "_in_" + time_Unit;
        deleteFile(fileName);
        write(fileName, String.valueOf(decimalFormat.format(minimum(file))));
        write(fileName, String.valueOf(decimalFormat.format(maximum(file))));
        write(fileName, String.valueOf(decimalFormat.format(average(file))));
        write(fileName, String.valueOf(decimalFormat.format(variance(file))));
        write(fileName, String.valueOf(decimalFormat.format(stdDev(file))));
        write(fileName, String.valueOf(decimalFormat.format(minMaxPercentDifference(file))) + "%");
    }
}