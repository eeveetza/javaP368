package test;

import main.P368;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.apache.commons.math3.complex.Complex;



public class P368Test {
    // the results are compared to the reference results of Recommendation ITU-R P.368-10
    // published at ITU-R SG3 web page.
    // the test is passed when the results for transmission loss are within 1e-6 dB of difference


    //     Rev   Date        Author                          Description
    //     -------------------------------------------------------------------------------
    //     v0    21NOV23     Ivica Stevanovic, OFCOM         Initial version in Java


    TestUtil util;

    @Before
    public void setup() {

        util = new TestUtil(0.001);

    }

    @Test
    public void test1() {
        P368 calculator = new P368();
        int sizeY;
        List<String> lines = new ArrayList<>();


        try {

            InputStream inputStream = getClass().getResourceAsStream("ValidationExampleLFMFSmoothEarth.csv");

            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while (null != (line = br.readLine())) {
                lines.add(line);

            }

            sizeY = lines.size();

        } catch (Exception ex) {

            throw new IllegalArgumentException("Could not load the file: '" + "ValidationExampleLFMFSmoothEarth.csv" + "'");
        }

        for (int i = 1; i < sizeY; i++) { /* DO */

            double[] data = ParseLine(lines.get(i));

            double htx = data[0];
            double hrx = data[1];
            double fmhz = data[2];
            double ptx = data[3];
            double ns = data[4];
            double dkm = data[5];
            double eps = data[6];
            double sigma = data[7];
            int pol = (int) data[8];

            double ref = data[9];

            P368.Result result = calculator.tl_p368(htx, hrx, fmhz, ptx, ns, dkm, eps, sigma, pol);


            Assert.assertEquals(ref, result.A_btl__db, 1e-6);

        }


    }

    double[] ParseLine(String line) {
        //String[] parts = line.trim().split("\\s+");
        String[] parts = line.trim().split(",");
        double[] data = new double[parts.length];

        for (int i = 0; i < parts.length; i++) {
            data[i] = Double.parseDouble(parts[i]);
        }
        return data;
    }

}