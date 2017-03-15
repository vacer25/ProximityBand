package com.queensu.apsc_100_mod_3_group_h.proximityband;

/**
 * Created by Admin on 3/10/2017.
 */

public class KalmanFilter {

    public double Q = 0.000001;
    public double R = 0.0001;
    public double P = 1, X = 0, K;

    //public KalmanFilter() {
    //P = 1.0f;
    //X = 0.0f;
    //K = 0.0f;
    //}

    private void measurementUpdate() {
        K = (P + Q) / (P + Q + R);
        P = R * (P + Q) / (R + P + Q);
    }

    public float update(float measurement) {
        measurementUpdate();
        double result = X + (measurement - X) * K;
        X = result;
        return (float)result;
    }

}
