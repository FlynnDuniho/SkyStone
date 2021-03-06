package org.firstinspires.ftc.team6220_2019;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.corningrobotics.enderbots.endercv.CameraViewDisplay;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.team6220_2019.ImageRecognition.Dogeforia6220;
import org.firstinspires.ftc.team6220_2019.ImageRecognition.SkystoneDetectionOpenCV;
import org.firstinspires.ftc.team6220_2019.ResourceClasses.ConcurrentOperation;
import org.firstinspires.ftc.team6220_2019.ResourceClasses.DriverInput;
import org.firstinspires.ftc.team6220_2019.ResourceClasses.PIDFilter;
import org.firstinspires.ftc.team6220_2019.ResourceClasses.Polynomial;
import org.firstinspires.ftc.team6220_2019.ResourceClasses.SequenceUtilities;

import java.util.ArrayList;
import java.util.List;

import static org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer.CameraDirection.BACK;

abstract public class MasterOpMode extends LinearOpMode
{
    // Remembers whether the grabber is open or closed.
    boolean isGrabberOpen = true;
    // Remembers whether the grabber arm is extended or retracted.
    boolean isGrabberArmRetracted = true;
    // Remembers whether the foundation servos are open or closed.
    boolean areFoundationServosOpen = true;
    // Determines whether the bottom hardstop for the lift motors is active.
    boolean isBottomHardStopOn = true;

    // Collector and lift run modes----------------------------------------------------------------
     // Tells us what drive mode the lift motor is in; by default, we use RUN_TO_POSITION for auto.
    boolean isLiftRunToPosMode = true;

    //---------------------------------------------------------------------------------------------

    // Distance (in inches) at which we wish to switch from rotating stone to collecting it.
    int collectionDistance = 26;

    // Create instance of Dogeforia to be used for both Vuforia and OpenCV image processing.
    // We need to pass in this opMode to be able to use some functionalities in that class.
    // Also:  Need to be public to be viewed in other packages!
    public Dogeforia6220 vuf;
    public SkystoneDetectionOpenCV skystoneDetector;
    public WebcamName webcamName;

    // Declare hardware devices---------------------------------------
    BNO055IMU imu;

    DcMotor motorFL, motorFR, motorBL, motorBR;
    DcMotor liftMotor1, liftMotor2;
    DcMotor collectorLeft, collectorRight;

    Servo grabberServo, grabberArmLeft, grabberArmRight;
    Servo foundationServoLeft, foundationServoRight;
    //----------------------------------------------------------------

    // Declare filters.  We currently have PID for turning and encoder navigation.------------------
    PIDFilter rotationFilter;
    PIDFilter translationFilter;

    // Stores orientation of robot
    double currentAngle = 0.0;

    // Need to be accessible outside of package!
    // Create drivers
    public DriverInput driver1;
    public DriverInput driver2;

    // Create a list of tasks to accomplish in order
    // Needs to be accessible outside of package!
    public List<ConcurrentOperation> callback = new ArrayList<>();

    // Need to be accessible outside of package!
    public ElapsedTime timer = new ElapsedTime();
    public double lTime = 0;

    // Allows us to use global rotation coordinates by specifying starting orientation.
    double startingOrientation;

    /*
    Polynomial for adjusting input from joysticks to allow for ease of driving.  A polynomial that
    is concave up in the 1st quadrant is preferable to a 1st degree polynomial, since a driver
    generally needs more control in the low speed range than the high range.
   */
    //                                             y = 0 + 0.5x + 0 + 0.5x^3
    Polynomial stickCurve = new Polynomial(new double[]{0, 0.5, 0, 0.5});

    // Variable that keeps track of the height of our tower. It tells autonomous and semi-autonomous drive how high the current tower is.
    int towerHeight = 0;


    // todo Add booleans to only init hardware that is currently on the robot for testing?
    // Init all hardware devices on the robot.  Also specify whether we are using Vuforia (not
    // initializing it saves a significant amount of time during testing).
    public void initialize(boolean isUsingVuforia)
    {
        driver1 = new DriverInput(gamepad1);
        driver2 = new DriverInput(gamepad2);

        // Only initialize Vuforia if we specify that we want to.
        if (isUsingVuforia)
            initVuforiaAndOpenCV();

        // Motor initializations--------------------------------------------
        motorFL = hardwareMap.dcMotor.get("motorFL");
        motorFR = hardwareMap.dcMotor.get("motorFR");
        motorBL = hardwareMap.dcMotor.get("motorBL");
        motorBR = hardwareMap.dcMotor.get("motorBR");

        liftMotor1 = hardwareMap.dcMotor.get("liftMotor1");
        liftMotor2 = hardwareMap.dcMotor.get("liftMotor2");

        collectorLeft = hardwareMap.dcMotor.get("collectorLeft");
        collectorRight = hardwareMap.dcMotor.get("collectorRight");


        motorFL.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorFR.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorBL.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorBR.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        motorFL.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorFR.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBL.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBR.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        collectorLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        collectorRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        // 1st lift motor runs using encoder, but 2nd one does not.
        liftMotor1.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        liftMotor1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        liftMotor2.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        motorFL.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorFR.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorBL.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorBR.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        collectorLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        collectorRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        liftMotor1.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        liftMotor2.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        stopDriveMotors();
        //-----------------------------------------------------------------


        // Servo initializations-------------------------------------------
        grabberServo = hardwareMap.servo.get("grabberServo");
        grabberArmLeft = hardwareMap.servo.get("grabberArmLeft");
        grabberArmRight = hardwareMap.servo.get("grabberArmRight");
        foundationServoLeft = hardwareMap.servo.get("foundationServoLeft");
        foundationServoRight = hardwareMap.servo.get("foundationServoRight");


        foundationServoLeft.setPosition(Constants.FOUNDATION_SERVO_LEFT_UP);
        foundationServoRight.setPosition(Constants.FOUNDATION_SERVO_RIGHT_UP);

        grabberServo.setPosition(Constants.GRABBER_OPEN);
        //-----------------------------------------------------------------


        // Retrieve and initialize the IMU. We expect the IMU to be attached to an I2C port
        // on a Core Device Interface Module, configured to be a sensor of type "AdaFruit IMU",
        // and named "imu". Certain parameters must be specified before using the imu.
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.calibrationDataFile = "AdafruitIMUCalibration.json"; // see the calibration sample opmode
        parameters.loggingEnabled = true;
        parameters.loggingTag = "IMU";
        imu = hardwareMap.get(BNO055IMU.class, "imu");
        imu.initialize(parameters);

        // Initialize PID filters
        rotationFilter = new PIDFilter(Constants.ROTATION_P, Constants.ROTATION_I, Constants.ROTATION_D);
        translationFilter = new PIDFilter(Constants.TRANSLATION_P, Constants.TRANSLATION_I, Constants.TRANSLATION_D);

        // Add necessary items to callback---------------
        callback.add(driver1);
        callback.add(driver2);
        //-----------------------------------------------
    }


    // Method to be called to initialize Vuforia and OpenCV via Dogeforia.
    public void initVuforiaAndOpenCV()
    {
        // todo Would like to find a way to avoid "chicken and the egg" problem and put this inside initVuforia().
        // Initializes standard Vuforia targets and camera stuff------------------------------------
        /*
         * Configure Vuforia by creating a Parameter object, and passing it to the Vuforia engine.
         * We can pass Vuforia the handle to a camera preview resource (on the RC phone);
         * If no camera monitor is desired, use the parameter-less constructor instead (commented out below).
         */
        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier(
                "cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters(cameraMonitorViewId);

        parameters.vuforiaLicenseKey = "AQdgAgj/////AAABmZGzg951/0AVjcK/+QiLWG1Z1PfbTwUouhED8hlwM6qrpAncj4xoMYYOUDxF+kreiazigY0q7OMa9XeMyxNlEQvyMFdefVUGSReIxJIXYhFaru/0IzldUlb90OUO3+J4mGvnzrqYMWG1guy00D8EbCTzzl5LAAml+XJQVLbMGrym2ievOij74wabsouyLb2HOab5nxk0FycYqTWGhKmS7/h4Ddd0UtckgnHDjNrMN4jqk0Q9HeTa8rvN3aQpSUToubAmfXe6Jgzdh2zNcxbaNIfVUe/6LXEe23BC5mYkLAFz0WcGZUPs+7oVRQb7ej7jTAJGA6Nvb9QKEa9MOdn0e8edlQfSBRASxfzBU2FIGH8a";;
        parameters.cameraDirection = BACK;
        /**
         * We also indicate which camera on the RC we wish to use.
         */
        // Default webcam name
        webcamName = hardwareMap.get(WebcamName.class, "Webcam 1");
        parameters.cameraName = webcamName;
        parameters.useExtendedTracking = false;

        // Instantiate the Vuforia engine with Dogeforia.
        vuf = new Dogeforia6220(parameters);
        vuf.enableConvertFrameToBitmap();
        //------------------------------------------------------------------------------------------

        // Initialize the skystoneDetector
        skystoneDetector = new SkystoneDetectionOpenCV();

        // fullscreen display:
        //   app crashes if screen orientation switches from portrait to landscape
        //   screen goes to sleep, and webcam turns off a few minutes after init, and after play
        skystoneDetector.init(hardwareMap.appContext,CameraViewDisplay.getInstance(), 0, true);

        // Set the skystoneDetector and enable it.
        vuf.setDogeCVDetector(skystoneDetector);
        vuf.enableDogeCV();
        // Starts Dogeforia thread.
        vuf.start();

        //vuf.activateTargets(); // Also remember to deactivate them if using Vuforia!
    }


    public void driveMecanum(double driveAngle, double drivePower, double w)
    {
        // Convert drive angle and power to x and y components
        double y = drivePower * Math.sin(Math.toRadians(driveAngle));
        double x = drivePower * Math.cos(Math.toRadians(driveAngle));

        // todo Did x and y sign flip work?
        // Signs for x, y, and w are based on the motor configuration and inherent properties of mecanum drive
        // Note:  Flipped x and y to flip front of robot.
        double powerFL = -x - y + w;
        double powerFR = -x + y + w;
        double powerBL = x - y + w;
        double powerBR = x + y + w;

        // Scale powers-------------------------
        /*
         Motor powers might be set above 1 (e.g., x + y = 1.0 and w = 1.0), so we must scale all of
         the powers to ensure they are proportional and within the range {-1.0, 1.0}
        */
        double powScalar = SequenceUtilities.getLargestMagnitude(new double[]
                {powerFL, powerFR, powerBL, powerBR});
        /*
         However, powScalar should only be applied if it is greater than 1. Otherwise, we could
         unintentionally increase powers or even divide by 0
        */
        if (powScalar > 1)
        {
            powerFL /= powScalar;
            powerFR /= powScalar;
            powerBL /= powScalar;
            powerBR /= powScalar;
        }

        motorFL.setPower(powerFL);
        motorFR.setPower(powerFR);
        motorBL.setPower(powerBL);
        motorBR.setPower(powerBR);
    }


    // Uses encoders to make the robot drive to a specified relative position.  Also makes use of the
    // imu to keep the robot at a constant heading during navigation.
    // Finally, we use the boolean isCollecting to give us the option to control the collector
    // while the robot is navigating.
    // **Note:  initDeltaX/Y are in inches.
    void navigateUsingEncoders(double initDeltaX, double initDeltaY, double maxPower, boolean isCollecting)
    {
        // Variables set every loop-------------------
        double deltaX = initDeltaX;
        double deltaY = initDeltaY;
        double initHeading = getAngularOrientationWithOffset();

        double driveAngle;
        double drivePower;
        double rotationPower;

        // Find distance and angle between robot and its destination
        double distanceToTarget = calculateDistance(deltaX, deltaY);

        currentAngle = getAngularOrientationWithOffset();
        double headingDiff = normalizeAngle(initHeading - currentAngle);
        //---------------------------------------------

        motorFL.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorFR.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorBL.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorBR.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        motorFL.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorFR.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBL.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBR.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // Check to see if robot has arrived at destination within angle and position tolerances
        while (((distanceToTarget > Constants.POSITION_TOLERANCE_IN) || (headingDiff > Constants.ANGLE_TOLERANCE_DEG)) && opModeIsActive())
        {
            // todo Why does sqrt(2) not show up in empirical distance tests?
            deltaX = initDeltaX - Constants.IN_PER_ANDYMARK_TICK * (motorFL.getCurrentPosition() -
                    motorBL.getCurrentPosition() + motorFR.getCurrentPosition() - motorBR.getCurrentPosition()) / (4 /* Math.sqrt(2)*/);
            deltaY = initDeltaY - Constants.IN_PER_ANDYMARK_TICK * (motorFL.getCurrentPosition() +
                    motorBL.getCurrentPosition() - motorFR.getCurrentPosition() - motorBR.getCurrentPosition()) / 4;

            // Calculate how far off robot is from its initial heading
            currentAngle = getAngularOrientationWithOffset();
            headingDiff = normalizeAngle(initHeading - currentAngle);

            // Recalculate drive angle and distance remaining every loop
            distanceToTarget = calculateDistance(deltaX, deltaY);
            driveAngle = Math.toDegrees(Math.atan2(deltaY, deltaX));

            // todo - signs should be properly accounted for.
            // Transform position and heading diffs to linear and rotation powers using filters----
            translationFilter.roll(-distanceToTarget);
            drivePower = translationFilter.getFilteredValue();

            // Ensure robot doesn't approach target position too slowly
            if (Math.abs(drivePower) < Constants.MINIMUM_DRIVE_POWER)
            {
                drivePower = Math.signum(drivePower) * Constants.MINIMUM_DRIVE_POWER;
            }
            // Ensure robot doesn't ever drive faster than we want it to
            else if (Math.abs(drivePower) > maxPower)
            {
                drivePower = Math.signum(drivePower) * maxPower;
            }

            // Additional factor is necessary to ensure turning power is large enough
            rotationFilter.roll(headingDiff);
            rotationPower = rotationFilter.getFilteredValue();
            //-------------------------------------------------------------------------------------

            // If it has been specified, collect while navigating.
            if (isCollecting)
            {
                // If we are within collectionDistance of stone, rotate collector.
                // Otherwise, collect the stone.
                if (Math.abs(initDeltaY - deltaY) < collectionDistance)
                    runCollector(1.0, true, false);
                else
                    runCollector(1.0, true, false);
            }

            driveMecanum(driveAngle, drivePower, rotationPower);

            telemetry.addData("Encoder Diff x: ", deltaX);
            telemetry.addData("Encoder Diff y: ", deltaY);
            telemetry.addData("Heading Diff: ", headingDiff);
            telemetry.update();
            idle();
        }

        // Turn off drive motors
        stopDriveMotors();

        // Turn off collector if we were running it.
        if (isCollecting)
        {
//            pauseWhileUpdating(1.0);
//
//            collectorLeft.setPower(0);
//            collectorRight.setPower(0);
        }
    }


    // Tell the robot to turn to a specified angle.  We can also limit the motor power while turning.
    public void turnTo(double targetAngle, double maxPower)
    {
        double turningPower;
        currentAngle = getAngularOrientationWithOffset();
        double angleDiff = normalizeAngle(targetAngle - currentAngle);

        // Robot only stops turning when it is within angle tolerance
        while (Math.abs(angleDiff) >= Constants.ANGLE_TOLERANCE_DEG && opModeIsActive())
        {
            currentAngle = getAngularOrientationWithOffset();

            // Give robot raw value for turning power
            angleDiff = normalizeAngle(targetAngle - currentAngle);

            // Send raw turning power through PID filter to adjust range and minimize oscillation
            rotationFilter.roll(angleDiff);
            turningPower = rotationFilter.getFilteredValue();

            // Make sure turningPower doesn't go above maximum power
            if (Math.abs(turningPower) > maxPower)
            {
                turningPower = maxPower * Math.signum(turningPower);
            }

            // Makes sure turningPower doesn't go below minimum power
            if (Math.abs(turningPower) < Constants.MINIMUM_TURNING_POWER)
            {
                turningPower = Math.signum(turningPower) * Constants.MINIMUM_TURNING_POWER;
            }

            // Turns robot
            driveMecanum(0.0, 0.0, turningPower);

            telemetry.addData("angleDiff: ", angleDiff);
            telemetry.addData("Turning Power: ", turningPower);
            telemetry.addData("Orientation: ", currentAngle);
            telemetry.update();
            idle();
        }

        stopDriveMotors();
    }

    // todo UNTESTED
    // Turn to a specified angle by driving one side of the robot.
    // Left side is with respect to collector in front.
    public void pivotTurn(boolean leftSide, double targetAngle, double maxPower)
    {
        double pivotPower;
        currentAngle = getAngularOrientationWithOffset();
        double angleDiff = normalizeAngle(targetAngle - currentAngle);

        // Robot only stops turning when it is within angle tolerance
        while ((Math.abs(angleDiff) >= 4 * Constants.ANGLE_TOLERANCE_DEG) && !isStopRequested())
        {
            currentAngle = getAngularOrientationWithOffset();

            // Give robot raw value for turning power
            angleDiff = normalizeAngle(targetAngle - currentAngle);

            // Send raw turning power through PID filter to adjust range and minimize oscillation
            rotationFilter.roll(angleDiff);
            pivotPower = rotationFilter.getFilteredValue();

            // Make sure turningPower doesn't go above maximum power
            if (Math.abs(pivotPower) > maxPower)
            {
                pivotPower = maxPower * Math.signum(pivotPower);
            }

            // Makes sure turningPower doesn't go below minimum power
            if (Math.abs(pivotPower) < Constants.MINIMUM_TURNING_POWER)
            {
                pivotPower = Math.signum(pivotPower) * Constants.MINIMUM_TURNING_POWER;
            }

            // Pivots robot; - signs convert between angle and drive power
            if (leftSide)   // Turn using left side of robot
            {
                motorFL.setPower(pivotPower);
                motorBL.setPower(pivotPower);
            }
            else            // Turn using right side of robot
            {
                motorFR.setPower(pivotPower);
                motorBR.setPower(pivotPower);
            }

            telemetry.addData("angleDiff: ", angleDiff);
            telemetry.addData("pivotPower: ", pivotPower);
            telemetry.addData("Orientation: ", currentAngle);
            telemetry.update();
            idle();
        }

        stopDriveMotors();
    }


    // todo Make sure this works
    // General method for driving collector (auto and TeleOp)
    public void runCollector(double powerFraction, boolean isCollectingIn, boolean isRotatingStone)
    {
        // Allows us to change direction of collector
        double powerSign = 1.0;

        // Reverse collector if we intend to spit out
        if (!isCollectingIn)
            powerSign = -1.0;

        if (isRotatingStone)    // We are trying to spin stone for easier collection
        {
            collectorLeft.setPower(powerSign * powerFraction * Constants.COLLECTOR_ROTATE_POWER);
            collectorRight.setPower(powerSign * powerFraction * Constants.COLLECTOR_ROTATE_POWER);
        }
        else    // We are collecting normally
        {
            collectorLeft.setPower(powerSign * powerFraction * -Constants.COLLECTOR_POWER);
            collectorRight.setPower(powerSign * powerFraction * Constants.COLLECTOR_POWER);
        }
    }


    // Toggle position of both foundation servos between open and closed.
    public void toggleFoundationServos()
    {
        if (areFoundationServosOpen)
        {
            foundationServoLeft.setPosition(Constants.FOUNDATION_SERVO_LEFT_DOWN);
            foundationServoRight.setPosition(Constants.FOUNDATION_SERVO_RIGHT_DOWN);
        } else
        {
            foundationServoLeft.setPosition(Constants.FOUNDATION_SERVO_LEFT_UP);
            foundationServoRight.setPosition(Constants.FOUNDATION_SERVO_RIGHT_UP);
        }
        areFoundationServosOpen = !areFoundationServosOpen;

        //telemetry.addData("foundationServoLeft Position: ", foundationServoLeft.getPosition());
        //telemetry.addData("foundationServoRight Position: ", foundationServoRight.getPosition());
    }


    // Toggle position of grabber between open and closed.
    public void toggleGrabber()
    {
        if (isGrabberOpen)
        {
            grabberServo.setPosition(Constants.GRABBER_CLOSED);
        } else
        {
            grabberServo.setPosition(Constants.GRABBER_OPEN);
        }
        isGrabberOpen = !isGrabberOpen;
    }


    // Toggle position of grabber arm between inside robot and placing position.
    public void toggleGrabberArm()
    {
        if (isGrabberArmRetracted)
        {
            grabberArmLeft.setPosition(Constants.GRABBER_ARM_SERVO_LEFT_EXTEND);
            grabberArmRight.setPosition(Constants.GRABBER_ARM_SERVO_RIGHT_EXTEND);
        } else
        {
            grabberArmLeft.setPosition(Constants.GRABBER_ARM_SERVO_LEFT_RETRACT);
            grabberArmRight.setPosition(Constants.GRABBER_ARM_SERVO_RIGHT_RETRACT);
        }
        isGrabberArmRetracted = !isGrabberArmRetracted;
    }


    // Runs the lift to a specified encoder position.
    public void runLiftToPosition(double position)
    {
        double currentPosition;
        ElapsedTime loopTimer = new ElapsedTime();      // Ensures we don't get stuck in loop.

        do
        {
            // Update lift position.
            currentPosition = liftMotor1.getCurrentPosition();

            if (currentPosition > position) // If greater, drive down
            {
                driveLift(-1 * Math.min(1, Math.abs(currentPosition - position) / Constants.LIFT_SCALE_HEIGHT));
            }
            else if (currentPosition < position)    // If less, drive up
            {
                driveLift(Math.min(1, Math.abs(currentPosition - position) / Constants.LIFT_SCALE_HEIGHT));
            }
            else{
                driveLift(0);
            }

            telemetry.addData("Lift Position: ", currentPosition);
            telemetry.addData("Target Position: ", position);
            telemetry.update();
            idle();
        }
        while ((Math.abs(currentPosition - position) > Constants.LIFT_MOTOR_TOLERANCE_ENC_TICKS) && opModeIsActive() && (loopTimer.seconds() < 5));    // Continue if outside tolerance.

        driveLift(0);
    }


    // Drives the lift motors.  If encoder position is below 0 or above max height, do not drive further outside operation limits.
    public void driveLift(double power)
    {
        if(!(((power < 0 && liftMotor1.getCurrentPosition() < Constants.LIFT_MOTOR_MIN_HEIGHT) && isBottomHardStopOn)
                || (power > 0 && liftMotor1.getCurrentPosition() > Constants.LIFT_MOTOR_MAX_HEIGHT)))
        {
            liftMotor1.setPower(-power * Constants.LIFT_POWER_FACTOR);
            liftMotor2.setPower(power * Constants.LIFT_POWER_FACTOR);
        }
        else
        {
            liftMotor1.setPower(0);
            liftMotor2.setPower(0);
        }
    }


    // Note:  not in use
    // Other opmodes must go through this method to prevent others from unnecessarily changing startingOrientation
    public void setRobotStartingOrientation(double newValue)
    {
        startingOrientation = newValue;
    }


    // Prevents an angle from being outside the range -180 to 180 degrees
    // Note:  Although it seems like it would, mod n doesn't work for this method.
    public double normalizeAngle(double rawAngle)
    {
        while (Math.abs(rawAngle) > 180)
        {
            rawAngle -= Math.signum(rawAngle) * 360;
        }

        return rawAngle;
    }


    // Takes into account startingOrientation to utilize global orientation
    public double getAngularOrientationWithOffset()
    {
        double correctedHeading = normalizeAngle(imu.getAngularOrientation().firstAngle + startingOrientation);

        return correctedHeading;
    }


    // Finds distance between 2 points
    public double calculateDistance(double dx, double dy)
    {
        double distance = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));

        return distance;
    }


    // Updates every item with elapsed time at the end of the main loop; ensures that operations
    // based on a timer are executed on time
    public void updateCallback(double eTime)
    {
        for (ConcurrentOperation item : callback)
        {
            item.update(eTime);
        }
    }


    // Note:  time parameter is in seconds
    // Waits for a specified time while giving each hardware system the ability to function simultaneously
    public void pauseWhileUpdating(double time)
    {
        lTime = timer.seconds();

        while (opModeIsActive() && (time > 0))
        {
            double eTime = timer.seconds() - lTime;
            lTime = timer.seconds();
            time -= eTime;

            updateCallback(eTime);
            //telemetry.addData("eTime:", eTime);
            //telemetry.addData("Seconds Remaining:", time);
            //telemetry.update();
            idle();
        }
    }


    public void stopDriveMotors()
    {
        motorFL.setPower(0);
        motorFR.setPower(0);
        motorBL.setPower(0);
        motorBR.setPower(0);
    }
}
