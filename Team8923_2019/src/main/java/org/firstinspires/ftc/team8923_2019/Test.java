package org.firstinspires.ftc.team8923_2019;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;


@Autonomous(name="Test", group = "Swerve")
/**
 * Runable shell for Master Autonomous code
 */
//@Disabled
public class Test extends MasterAutonomous
{

    @Override
    public void runOpMode() throws InterruptedException
    {
        configureAutonomous();
        initAuto();
        telemetry.clear();
        telemetry.update();
        double refereneAngle = imu.getAngularOrientation().secondAngle;


        waitForStart();
        telemetry.clear();


        while (opModeIsActive())
        {
            imuPivot(refereneAngle, 90, .3, .015, 3);
            break;
        }

    }
}
