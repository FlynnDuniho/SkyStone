package org.firstinspires.ftc.team6220_2019;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

// todo Edit autonomous outline based on new structure.
/**
 * General autonomous class.  During initialization, driver 1 selects various options, including
 * alliance, delay time, number of SkyStones, whether we want to soore the foundation, and whether
 * we want to park close or far under the Skybridge.  These options allow our robot to adapt to the
 * preferences of various alliance partners.
 *
 *   Outline of autonomous (written from perspective of red team)
 *         Be able to start at two positions. One with x = -36in, one with x = 24in.
 *
 *         If alliance partner is doing autonomous, translate left two feet.
 *
 *         Else, translate forward by 12in. Pause briefly to determine which stone is skystone.
 *         Rotate robot 180 degrees. --FROM NOW ON, ALL INSTRUCTIONS GIVEN WITH NEW ORIENTATION--
 *         Align robot by translating left/right for skystone. Translate forward around two feet to collect
 *         skystone. Ensure that the position of the arm is ready to collect stone.
 *
 *         Once collected, grabber should close. Translate backwards to one of two positions, depending on
 *         whether we are using inside lane or outside lane (depends on where alliance robot is). Translate right until
 *         center of robot is around 24in from right edge.
 *
 *         Translate forward an appropriate amount (depending on which lane used), then lower foundation servos.
 *         Drive backwards until the back of the robot is against the wall. Extend arm, drop grabber.
 *
 *         Translate left to get out of the corner (possibly engaging collector motors to counteract the
 *         inevitable friction), then translate to the designated parking spot (depends on where
 *         alliance partner is).
 */
// todo Test old auto code to see if it works.
// todo Need to clean up implementation of buffet autonomous.

@Autonomous(name = "AutoCompetition")
public class AutoCompetition extends MasterAutonomous
{
    @Override
    public void runOpMode() throws InterruptedException
    {
        initialize(true, true);

        // Start perpendicular to red wall
        setRobotStartingOrientation(initAngle_side);


        waitForStart();


        // Grabber arm must initialize after match starts in order to satisfy 18" size constraint, after which it flips out collector.
        grabberArmLeft.setPosition(Constants.GRABBER_ARM_SERVO_LEFT_RETRACT);
        grabberArmRight.setPosition(Constants.GRABBER_ARM_SERVO_RIGHT_RETRACT);
        // Raise lift to correct position for collection of SkyStone.   todo Is this running the lift into the ground?  Enable when working.
        runLiftToPosition(Constants.LIFT_MOTOR_COLLECT_HEIGHT);
        pauseWhileUpdating(1.0);    // Wait for collector to drop before webcam attempts to recognize SkyStone.


        // Wait to start the match for 0-10 seconds, depending on setup input.
        pauseWhileUpdating(delayCount);


        // Only run stone collection section of auto if we have specified that we want to collect 1 or more SkyStones.
        if (numSkyStones > 0)
        {
            // Find SkyStone image target and translate appropriate distance toward image target.
            alignWithSkyStone();

            // Drive forward and collect SkyStone.
            navigateUsingEncoders(0, 45, 0.3, true);

            // Lower lift, then grab SkyStone and drive backwards (12 in away from tile line)
            runLiftToPosition(Constants.LIFT_MOTOR_GRAB_HEIGHT);
            toggleGrabber();
            navigateUsingEncoders(0, -21 - parkShift, 0.5, false);  // parkShift accounts for near or far park

            // Turn to face Skybridge
            turnTo(180, 0.7);
        }

        // Option for scoring foundation.
        if (scoreFoundation)
        {
            // Account for different robot positions depending on whether we are scoring SkyStones or not.
            if (numSkyStones != 0)
            {
                // Navigate to center of foundation (+3.5 tiles and 4 in for foundation), and turn again so foundationServos face the foundation
                navigateUsingEncoders(0, -83 + robotShiftSign * robotShift /*- centerAdjustment*/, 0.7, false);

                // Stop collector.
                pauseWhileUpdating(0.25);
                collectorLeft.setPower(0);
                collectorRight.setPower(0);

                turnTo(-90 + turnShift, 0.7);   // Account for turn shift if blue alliance (+180)
            }
            else    // Otherwise, if no stones are to be scored, just drive from wall to front of foundation.
                navigateUsingEncoders(0, -27, 0.7, false);      // WARNING:  Collector might hit wall in this setup scenario.


            // Drive up to foundation (forward 3 in + 4 in extra for good measure) and activate foundationServos.
            navigateUsingEncoders(0, -7 - parkShift, 0.3, false);   // Account for near or far park to go correct distance to foundation.
            toggleFoundationServos();
            pauseWhileUpdating(0.5);

           /* // Pull foundation:  Turn 45 degrees and pull, then 45 degrees cw again and push.
            pivotTurn(!isRedAlliance,robotShiftSign * -135,0.8);   // robotShiftSign accounts for opposite red / blue turns here.
            pivotTurn(isRedAlliance,robotShiftSign * -180,0.8);                      // isRedAlliance accounts for left / right pull order being reversed.
            toggleFoundationServos();*/
            // Pull foundation straight back.
            navigateUsingEncoders(0, 36, 0.3, false);   // Account for near or far park to go correct distance to foundation.

            // Raise lift, move grabber arm, drop SkyStone, and retract lift along with grabber arm.
            runLiftToPosition(Constants.LIFT_MOTOR_PLACE_HEIGHT);
            pauseWhileUpdating(0.5);
            toggleGrabberArm();
            pauseWhileUpdating(0.5);
            toggleGrabber();
            pauseWhileUpdating(0.5);
            toggleGrabberArm();
            pauseWhileUpdating(0.5);
            runLiftToPosition(Constants.LIFT_MOTOR_GRAB_HEIGHT);

            /*// Release foundation and shift toward wall if parking close (must account for blue / red here).
            toggleFoundationServos();
            navigateUsingEncoders(-robotShiftSign * parkShift / 2, 0, 0.5, false);   // todo /2 factor here may be very wrong!*/
            // Release foundation and park.
            toggleFoundationServos();
            toggleGrabberArm();
            navigateUsingEncoders(robotShiftSign * 60, 0, 0.7, false);

            /*// Drive forward 72 - (9 + 18.5) = 44.5 in to park on line.
            navigateUsingEncoders(0, 45, 0.7, false);*/
        }
        else
        {
            // Center of robot 14 inches past center line
            // Move past line and return to both park and score SkyStone.
            navigateUsingEncoders(0, -50, 0.7, false);
            navigateUsingEncoders(0, 14, 0.7, false);
        }



        // Turn off OpenCV.
        skystoneDetector.disable();
        // stop the vision system
        vuf.stop();
    }
}
