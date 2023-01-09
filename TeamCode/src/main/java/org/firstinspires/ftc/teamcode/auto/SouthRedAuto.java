package org.firstinspires.ftc.teamcode.auto;

import com.chsrobotics.ftccore.engine.navigation.path.PrecisionMode;
import com.chsrobotics.ftccore.engine.navigation.path.Tolerances;
import com.chsrobotics.ftccore.engine.navigation.path.TrapezoidalMotionProfile;
import com.chsrobotics.ftccore.geometry.Position;
import com.chsrobotics.ftccore.hardware.HardwareManager;
import com.chsrobotics.ftccore.hardware.config.Config;
import com.chsrobotics.ftccore.hardware.config.accessory.Accessory;
import com.chsrobotics.ftccore.hardware.config.accessory.AccessoryType;
import com.chsrobotics.ftccore.pipeline.Pipeline;
import com.chsrobotics.ftccore.vision.CVUtility;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.PIDCoefficients;

import org.firstinspires.ftc.teamcode.auto.util.SignalSleeveDetector;
import org.firstinspires.ftc.teamcode.auto.actions.ArmPositionAction;
import org.firstinspires.ftc.teamcode.auto.actions.DelayAction;
import org.firstinspires.ftc.teamcode.auto.actions.FullStopAction;
import org.firstinspires.ftc.teamcode.auto.actions.SetArmAction;
import org.firstinspires.ftc.teamcode.auto.actions.ToggleClawAction;
import org.firstinspires.ftc.teamcode.auto.actions.WaitAction;
import org.firstinspires.ftc.teamcode.auto.util.WebcamPipeline;

@Autonomous(name = "Left Side")
public class SouthRedAuto extends LinearOpMode
{
    @Override
    public void runOpMode() throws InterruptedException
    {
        Config config = new Config.Builder()
                .setDebugMode(true)
                .setDriveMotors("m0", "m1", "m2", "m3")
                .setMotorDirection(DcMotorSimple.Direction.REVERSE)
                .addAccessory(new Accessory(AccessoryType.MOTOR, "l0"))
                .addAccessory(new Accessory(AccessoryType.SERVO, "c0"))
                .addAccessory(new Accessory(AccessoryType.SERVO, "c1"))
                .addAccessory(new Accessory(AccessoryType.WEBCAM, "webcam"))
                .addAccessory(new Accessory(AccessoryType.ODOMETRY_POD, "odo0"))
                .addAccessory(new Accessory(AccessoryType.ODOMETRY_POD, "odo1"))
                .setOdometryWheelProperties(8192, 70, -80.962, -28.575)
                .setOpMode(this)
                .setIMU("imu")
                .setPIDCoefficients(new PIDCoefficients(4.5, 0.0002, 0), new PIDCoefficients(750, 0.03, 0))
                .setNavigationTolerances(new Tolerances(45, 0.15))
                .setHighPrecisionTolerances(new Tolerances(17, 0.09))
                .build();

        HardwareManager manager = new HardwareManager(config, hardwareMap);

        manager.accessoryOdometryPods[0].setDirection(DcMotorSimple.Direction.REVERSE);
        manager.accessoryOdometryPods[1].setDirection(DcMotorSimple.Direction.REVERSE);

        manager.driveMotors[0].setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        manager.driveMotors[1].setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        manager.driveMotors[2].setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        manager.driveMotors[3].setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);

        CVUtility cv = null;
        try {
            cv = new CVUtility(manager, telemetry);
        } catch (Exception e) {
            telemetry.addLine("CVUtility failed to initialized");
            telemetry.update();
        }

        ArmPositionAction armPositionAction = new ArmPositionAction(manager);
        ToggleClawAction toggleClawAction = new ToggleClawAction(manager);
        toggleClawAction.execute();

        waitForStart();

        int dots = 1;
        if (cv != null && cv.initialized && cv.grabFrame() != null) {
            dots = SignalSleeveDetector.detectOrientation(cv.grabFrame());

            telemetry.addData("Dots: ", dots);
            cv.stopStreaming();
        } else {
            telemetry.addLine("Signal sleeve detection failed");
        }
        double parkingPos;
        parkingPos = dots == 1 ? -550 :
                (dots == 2 ? 0 : 600);

        telemetry.update();

        Pipeline pipeline = new Pipeline.Builder(manager)
                .addContinuousAction(armPositionAction)
                .addAction(new SetArmAction(manager, 3900))
                .addLinearPath(                                                 // Go to high junction
                        new TrapezoidalMotionProfile(250, 1000),
                        new Position(590, 100, 0),
                        new Position(590, 1340, 0)
                )
                .addLinearPath(                                                 // Align w/ high junction
                        PrecisionMode.HIGH,
                        new Position(240, 1340, 0)
                )
                .addAction(new FullStopAction(manager))
                .addAction(new SetArmAction(manager, 3200))
                .addAction(new WaitAction(manager, armPositionAction))
                .addAction(toggleClawAction)                                    // Drop cone 1
                .addAction(new DelayAction(manager, 200))
                .addAction(new SetArmAction(manager, 700))
                .addLinearPath(                                                 // Align with cone stack
                        PrecisionMode.HIGH,
                        new TrapezoidalMotionProfile(500, 1000),
                        new Position(-560, 1340, Math.PI / 2, 1)
                )
                .addAction(new FullStopAction(manager))
                .addAction(new WaitAction(manager, armPositionAction))
                .addAction(toggleClawAction)                                    // Pickup cone 2
                .addAction(new DelayAction(manager, 400))
                .addAction(new SetArmAction(manager, 3900))
                .addAction(new DelayAction(manager, 200))
                .addLinearPath(                                                 // Align with high junction
                        PrecisionMode.HIGH,
                        new TrapezoidalMotionProfile(500, 1000),
                        new Position(240, 1360, 0)
                )
                .addAction(new FullStopAction(manager))
                .addAction(new SetArmAction(manager, 3200))
                .addAction(new WaitAction(manager, armPositionAction))
                .addAction(toggleClawAction)                                    // Drop cone 2
                .addAction(new DelayAction(manager, 200))
                .addAction(new SetArmAction(manager, 500))
                .addLinearPath(                                                 // Align with cone stack
                        PrecisionMode.HIGH,
                        new TrapezoidalMotionProfile(500, 1000),
                        new Position(-560, 1340, Math.PI / 2, 0.5)
                )
                .addAction(new FullStopAction(manager))
                .addAction(new WaitAction(manager, armPositionAction))
                .addAction(toggleClawAction)                                    // Pickup cone 3
                .addAction(new DelayAction(manager, 400))
                .addAction(new SetArmAction(manager, 3900))
                .addAction(new DelayAction(manager, 200))
                .addLinearPath(                                                 // Align with high junction
                        PrecisionMode.HIGH,
                        new TrapezoidalMotionProfile(500, 1000),
                        new Position(240, 1360, 0)
                )
                .addAction(new FullStopAction(manager))
                .addAction(new SetArmAction(manager, 3200))
                .addAction(new WaitAction(manager, armPositionAction))
                .addAction(toggleClawAction)                                    // Drop cone 3
                .addAction(new DelayAction(manager, 200))
                .addAction(new SetArmAction(manager, 0))
                .addLinearPath(                                                 // Park
                        PrecisionMode.HIGH,
                        new TrapezoidalMotionProfile(500, 1000),
                        new Position(parkingPos, 1300, 0)
                )
                .addAction(new FullStopAction(manager))
                .addAction(new WaitAction(manager, armPositionAction))
                .build();

        pipeline.execute();
    }
}
