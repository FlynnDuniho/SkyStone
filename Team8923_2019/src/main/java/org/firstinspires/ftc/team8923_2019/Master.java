package org.firstinspires.ftc.team8923_2019;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.navigation.Orientation;

abstract class Master extends LinearOpMode
{
    DcMotor motorFL;
    DcMotor motorFR;
    DcMotor motorBL;
    DcMotor motorBR;

    DcMotor intakeLeft;
    DcMotor intakeRight;

    DcMotor motorLift;
    DcMotor motorLift2;

    Servo servoFoundationLeft;
    Servo servoFoundationRight;
    Servo servoCapstone;

    Servo servoJoint;
    Servo servoClaw;

    int initialLiftTicks;

    BNO055IMU imu;

    double powerFL;
    double powerFR;
    public double powerBL;
    public double powerBR;



    double slowModeDivisor = 1.0;

    boolean reverseDrive = false;



    public void initHardware()
    {
        motorFL = hardwareMap.get(DcMotor.class, "motorFL");
        motorFR = hardwareMap.get(DcMotor.class, "motorFR");
        motorBL = hardwareMap.get(DcMotor.class, "motorBL");
        motorBR = hardwareMap.get(DcMotor.class, "motorBR");

        intakeLeft = hardwareMap.get(DcMotor.class, "intakeLeft");
        intakeRight = hardwareMap.get(DcMotor.class, "intakeRight");

        motorLift = hardwareMap.get(DcMotor.class, "motorLift");
        motorLift2 = hardwareMap.get(DcMotor.class, "motorLift2");

        servoFoundationLeft = hardwareMap.get(Servo.class,"servoFoundationLeft");
        servoFoundationRight = hardwareMap.get(Servo.class,"servoFoundationRight");
        servoCapstone = hardwareMap.get(Servo.class, "servoCapstone");
        servoJoint = hardwareMap.get(Servo.class,"servoJoint");
        servoClaw = hardwareMap.get(Servo.class,"servoClaw");

        // Set ZeroPowerBehavior
        motorFL.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorFR.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorBL.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorBR.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        intakeLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        intakeRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        motorLift.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorLift2.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // Set Reversed Motors
        intakeLeft.setDirection(DcMotorSimple.Direction.REVERSE);
        motorFL.setDirection(DcMotorSimple.Direction.REVERSE);
        motorBL.setDirection(DcMotorSimple.Direction.REVERSE);
        motorLift2.setDirection(DcMotorSimple.Direction.REVERSE);

        motorFL.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorFR.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBL.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBR.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        motorLift.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorLift2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);


        // init positions
        servoJoint.setPosition(0);
        servoFoundationLeft.setPosition(0.64);
        servoFoundationRight.setPosition(0.1);
        servoCapstone.setPosition(0.39);
        servoClaw.setPosition(0.5);

        //lift
        initialLiftTicks = motorLift2.getCurrentPosition();

        //imu
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit = BNO055IMU.AngleUnit.DEGREES;
        imu = hardwareMap.get(BNO055IMU.class, "imu");
        imu.initialize(parameters);

        // Makes sure imu is calibrated before continuing
        while (!isStopRequested() && !imu.isGyroCalibrated())
        {
            sleep(50);
            idle();
        }


    }

    void driveMecanum(double driveAngle, double drivePower, double turnPower)
    {
        // Calculate x and y components of drive power, where y is forward (0 degrees) and x is right (-90 degrees)
        double x = drivePower * -Math.sin(Math.toRadians(driveAngle));
        double y = drivePower * Math.cos(Math.toRadians(driveAngle));

        /*
         * Below is an explanation of how the mecanum wheels are driven
         *
         * Each wheel has a roller on the bottom mounted at 45 degrees. Because of this, each wheel
         * can only exert a force diagonally in one dimension. Using the front left wheel as an
         * example, when it is given a positive power, it exerts a force forward and right, which
         * means positive y and x. When the front right wheel is given a positive power, it exerts
         * a force forward and left, which means positive y and negative x. This is reflected in
         * how the motor powers are set. Turning is like standard tank drive.
         */
        // Set motor powers
        if(reverseDrive)
            turnPower = -turnPower;

        //todo check if this was coded with the motors set initially to be reversed or not.
        powerFL = y + x - turnPower;
        powerFR = y - x + turnPower;
        powerBL = y - x - turnPower;
        powerBR = y + x + turnPower;

        // Motor powers might be set above 1, so this scales all of the motor powers to stay
        // proportional and within power range
        double scalar = Math.max(Math.abs(powerFL), Math.max(Math.abs(powerFR),
                Math.max(Math.abs(powerBL), Math.abs(powerBR))));

        // Only apply scalar if greater than 1. Otherwise we could unintentionally increase power
        // This also prevents dividing by 0
        if(scalar < 1)
            scalar = 1;

        // Apply scalar
        powerFL /= (scalar * slowModeDivisor);
        powerFR /= (scalar * slowModeDivisor);
        powerBL /= (scalar * slowModeDivisor);
        powerBR /= (scalar * slowModeDivisor);

        if (!reverseDrive)
        {
            // Drive forwards
            motorFL.setPower(powerFL);
            motorFR.setPower(powerFR);
            motorBL.setPower(powerBL);
            motorBR.setPower(powerBR);
        }
        else
        {
            // Drive backwards
            motorFL.setPower(-powerFL);
            motorFR.setPower(-powerFR);
            motorBL.setPower(-powerBL);
            motorBR.setPower(-powerBR);
        }

    }

    boolean buttonsAreReleased(Gamepad pad)
    {
        return !(pad.a || pad.b || pad.x || pad.y || pad.left_bumper || pad.right_bumper
                || pad.dpad_up || pad.dpad_down || pad.dpad_left || pad.dpad_right
                || pad.left_stick_button || pad.right_stick_button
                || pad.start || pad.back || pad.guide || pad.left_trigger > 0.35
                || pad.right_trigger > 0.35);
    }

    // Used for calculating distances between 2 points
    double calculateDistance(double deltaX, double deltaY)
    {
        return Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
    }

    // If you subtract 359 degrees from 0, you would get -359 instead of 1. This method handles
    // cases when one angle is multiple rotations away from the other
    double subtractAngles(double first, double second)
    {
        double delta = first - second;
        while(delta > 180)
            delta -= 360;
        while(delta <= -180)
            delta += 360;
        return delta;
    }
}
