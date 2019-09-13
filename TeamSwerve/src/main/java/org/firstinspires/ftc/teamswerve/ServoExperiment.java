package org.firstinspires.ftc.teamswerve;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.LED;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.Func;


/**
 * Program used to control Drive-A-Bots.
 * This can be a good reference for drive controls.
 */
@TeleOp(name="ServoExperiment", group = "Swerve")
@Disabled
public class ServoExperiment extends LinearOpMode
{
    DcMotor motorLeft = null;
    DcMotor motorRight = null;
    CRServo crServo = null;
    Servo servo = null;
    LED led = null;
    ColorSensor colorSensor = null;

    // Drive mode constants
    public static final int TANK_DRIVE = 0;
    public static final int ARCADE_DRIVE = 1;
    public static final int SPLIT_ARCADE_DRIVE = 2;
    public static final int GAME_DRIVE = 3;
    public int driveMode = SPLIT_ARCADE_DRIVE;

    @Override public void runOpMode() throws InterruptedException
    {
        // Initialize hardware and other important things
        initializeRobot();

        // Wait until start button has been pressed
        waitForStart();

        // Main loop
        while(opModeIsActive())
        {
            // Gamepads have a new state, so update things that need updating
            //if(updateGamepads())
            {
                // Set drive mode
                if(gamepad1.back && gamepad1.x)
                    driveMode = TANK_DRIVE;
                else if(gamepad1.back && gamepad1.a)
                    driveMode = ARCADE_DRIVE;
                else if(gamepad1.back && gamepad1.b)
                    driveMode = GAME_DRIVE;
                else if(gamepad1.back && gamepad1.y)
                    driveMode = SPLIT_ARCADE_DRIVE;

                // Run drive mode
                if(driveMode == TANK_DRIVE)
                    tankDrive(); // Changed by Dryw with permission from Heidi
                else if(driveMode == ARCADE_DRIVE)
                    arcadeDrive();
                else if(driveMode == GAME_DRIVE)
                    gameDrive();
                else if(driveMode == SPLIT_ARCADE_DRIVE)
                    splitArcadeDrive();
            }

            if (gamepad1.a)
            {
                led.enable(true);
                // servo.setPosition(0);
               // crServo.setPower(0);
               // crServo.setDirection(DcMotorSimple.Direction.FORWARD);
            }

            if (gamepad1.b)
            {
                led.enable(false);
                // servo.setPosition(1);
               // crServo.setPower(1);
                // crServo.setDirection(DcMotorSimple.Direction.FORWARD);
            }

            if (gamepad1.x)
            {
                // servo.setPosition(-1);
                // crServo.setPower(-1);
                // crServo.setDirection(DcMotorSimple.Direction.FORWARD);
            }

            if (gamepad1.y)
            {
                // servo.setPosition(.5);
               // crServo.setPower(.2);
                // crServo.setDirection(DcMotorSimple.Direction.FORWARD);
            }

            telemetry.update();
            idle();
        }
    }

    /*
     * Controls the robot with two joysticks
     * Left joystick controls left side
     * Right joystick controls right side
     */
    public void tankDrive()
    {
        double leftPower;
        double rightPower;
        double jx;
        double jy;
        jx = gamepad1.left_stick_y;
        jy = gamepad1.right_stick_y;

        if (jx > 0)
        {
            leftPower = -Math.pow(jx, 2);
        }
        else
        {
            leftPower = Math.pow(jx, 2);
        }

        if (jy > 0)
        {
            rightPower = -Math.pow(jy, 2);
        }
        else
        {
            rightPower = Math.pow(jy, 2);
        }


        motorLeft.setPower(leftPower);
        motorRight.setPower(rightPower);
    }

    /*
     * Controls the robot with a single joystick
     * Forward and backward on joystick control forward and backward power
     * Left and right control turning
     */
    public void arcadeDrive()
    {
        double forwardPower = gamepad1.left_stick_y;
        double turningPower = Math.pow(Math.abs(gamepad1.left_stick_x), 2) * Math.signum(gamepad1.left_stick_x);

        double leftPower = forwardPower - turningPower;
        double rightPower = forwardPower + turningPower;

        motorLeft.setPower(leftPower);
        motorRight.setPower(rightPower);
    }

    /*
     * Controls robot like a racing video game
     * Right trigger moves robot forward
     * Left trigger moves robot backward
     * Left stick for turning
     */
    public void gameDrive()
    {
        double forwardPower = gamepad1.left_trigger - gamepad1.right_trigger;
        double turningPower = Math.pow(gamepad1.left_stick_x, 2) * Math.signum(gamepad1.left_stick_x); // This multiplier is because the robot turns too quickly

        double leftPower = forwardPower - turningPower;
        double rightPower = forwardPower + turningPower;

        motorLeft.setPower(leftPower);
        motorRight.setPower(rightPower);
    }

    /*
     * Arcade drive with 2 joysticks
     */
    public void splitArcadeDrive()
    {
        double forwardPower = gamepad1.left_stick_y;
        double turningPower = Math.pow(Math.abs(gamepad1.right_stick_x), 2) * Math.signum(gamepad1.right_stick_x);

        double leftPower = forwardPower - turningPower;
        double rightPower = forwardPower + turningPower;

        motorLeft.setPower(leftPower);
        motorRight.setPower(rightPower);
    }

    public void initializeRobot()
    {
        // Initialize motors to be the hardware motors
        motorLeft = hardwareMap.dcMotor.get("motorLeft");
        motorRight = hardwareMap.dcMotor.get("motorRight");

        // We're not using encoders, so tell the motor controller
        motorLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motorRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        // The motors will run in opposite directions, so flip one
        //THIS IS SET UP FOR TANK MODE WITH OUR CURRENT DRIVABOTS
        //DON'T CHANGE IT!
        motorRight.setDirection(DcMotor.Direction.REVERSE); //DO NOT change without talking to Heidi first!!!

        crServo = hardwareMap.crservo.get("servoCr");
        servo = hardwareMap.servo.get("servo");

        led = hardwareMap.led.get("led");
        colorSensor = hardwareMap.colorSensor.get("colorSensor");

        // Set up telemetry data
        configureDashboard();
    }

    public void configureDashboard()
    {
        telemetry.addLine()
                .addData("Power | Left: ", new Func<String>() {
                    @Override public String value() {
                        return formatNumber(motorLeft.getPower());
                    }
                })
                .addData("Right: ", new Func<String>() {
                    @Override public String value() {
                        return formatNumber(motorRight.getPower());
                    }
                });

        telemetry.addLine()
                .addData("Color: ", new Func<String>() {
                    @Override public String value() {
                        return (colorSensor.red() + ", " + colorSensor.green() + ", " + colorSensor.blue());
                    }
                });


    }

    public String formatNumber(double d)
    {
        return String.format("%.2f", d);
    }
}
