package org.firstinspires.ftc.team417_2019;

public class Robot {

    double curAngle = 0;
    double currentX = 0;
    double currentY = 0;
    private double initialHeading;

    MasterOpMode master;

    // instantiated masterOpMode to resolve static issues because we are making a new robot object
    public Robot(MasterOpMode masterOpMode){
        master = masterOpMode;
    }

    // correct our initial angle depending on where we are starting by the field
    public double getCorrectedHeading(){
        return initialHeading + master.imu.getAngularOrientation().firstAngle;
    }
    public void setCorrectedHeading(double initialHeading) {
        this.initialHeading = initialHeading;
    }

    public void updatePosition() {
        // divide by 2 because we are averaging them (scaling)
        // plug in motor values to check the equations below
        currentX = ( (float) (master.motorFL.getCurrentPosition() - master.motorBL.getCurrentPosition() ) ) / 2;
        currentY = ( (float) (master.motorFL.getCurrentPosition() + master.motorBL.getCurrentPosition() ) ) / 2;

        curAngle = getCorrectedHeading();
    }

}
