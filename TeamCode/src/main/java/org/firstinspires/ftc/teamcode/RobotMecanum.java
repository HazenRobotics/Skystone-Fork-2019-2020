
package org.firstinspires.ftc.teamcode;

import android.app.SharedElementCallback;

import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cGyro;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.GyroSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcontroller.internal.FtcRobotControllerActivity;
import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.android.AndroidGyroscope;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;
import org.firstinspires.ftc.robotcore.external.tfod.TFObjectDetector;

import java.util.List;
import java.util.ArrayList;

public class RobotMecanum extends Robot
{
    final int tickPerRevlolution = 1440;
    final double linearWheelDistance = 4;

    DcMotor lift;
    final double MAX_LIFT_SPEED = 0.8;

    //Define Wheel Motors
    DcMotor frontLeftWheel;
    DcMotor frontRightWheel;

    Servo leftHook;
    Servo rightHook;
    final double LEFT_HOOK_HOME = 0.75;
    final double RIGHT_HOOK_HOME = 0.2;
    final double LEFT_HOOK_EXTENDED = 0;
    final double RIGHT_HOOK_EXTENDED = 1;
    double leftHookPosition = LEFT_HOOK_HOME;
    double rightHookPosition =  RIGHT_HOOK_HOME;

    Servo claw;
    final double CLAW_HOME = 0.0;
    final double CLAW_EXTENDED = 0.38;
    double clawPosition = CLAW_HOME;

    //==============================================================================================   Robot method
    public RobotMecanum(HardwareMap hMap, OpMode opMode)
    {
        super(hMap, opMode);

        lift = hardwareMap.dcMotor.get("lift");

        frontLeftWheel = hardwareMap.dcMotor.get("front_left_wheel");
        frontRightWheel = hardwareMap.dcMotor.get("front_right_wheel");

        leftHook = hardwareMap.servo.get("left_hook");
        rightHook = hardwareMap.servo.get("right_hook");

        gyro = hardwareMap.gyroSensor.get("gyro");
        gyro.calibrate();

        //Reverse the two flipped wheels
        frontRightWheel.setDirection(DcMotor.Direction.REVERSE);
        backRightWheel.setDirection(DcMotor.Direction.REVERSE);

        //Claw
        claw = hardwareMap.servo.get("claw");
        claw.setPosition(CLAW_HOME);

    }
    //==============================================================================================   convertDistTicks
    //method takes in 2nd parameter for circumfrence of spinning object
    public int convertDistTicks(double distanceToTravel, double circumfrence)
    {
        //1440 revolutions = 1 rotation
        //1 rotation = 4

        double revolutions = distanceToTravel / circumfrence;
        int totalTicks = (int) Math.round(revolutions * tickPerRevlolution);

        return totalTicks;
    }
    public void moveOmni(double left_stick_y, double left_stick_x, double right_stick_x)
    {
        double drive = Math.signum(-left_stick_y) * Math.pow(left_stick_y, 4);
        double strafe = Math.signum(left_stick_x) * Math.pow(left_stick_x, 4);
        double rotate = right_stick_x;

        double frontLeftPower = drive + strafe + rotate;
        double backLeftPower = drive - strafe + rotate;
        double frontRightPower = drive - strafe - rotate;
        double backRightPower = drive + strafe - rotate;

        //frontLeftPower = drive + strafe + rotate;
        //backLeftPower = drive - strafe + rotate;
        //frontRightPower = drive - strafe - rotate;
        //backRightPower = drive + strafe - rotate;

        //Set the wheel power according to variables
        frontLeftWheel.setPower(frontLeftPower);
        backLeftWheel.setPower(backLeftPower);
        frontRightWheel.setPower(frontRightPower);
        backRightWheel.setPower(backRightPower);

        //Print Telementary Data for the wheels
        telemetry.addData("frontLeftPower", frontLeftPower);
        telemetry.addData("backLeftPower", backLeftPower);
        telemetry.addData("frontRightPower", frontRightPower);
        telemetry.addData("backRightPower", backRightPower);
    }
    public void moveVertical(double distance, double power)
    {
        moveOmni( 1, 1, 1);
    }
    public void moveHorizontal()
    {
        moveOmni( 1, 1, 1);
    }
    public void rotate()
    {
        moveOmni( 1, 1, 1);
    }
    public void claw(boolean clawHome)
    {
        if (clawHome)
        {
            clawPosition = CLAW_HOME;
        }
        else
        {
            clawPosition = CLAW_EXTENDED;
        }

        claw.setPosition(clawPosition);

        telemetry.addData("Claw Position: ", claw.getPosition());
        telemetry.update();
    }
    public void hooks(boolean hooksHome)
    {
        telemetry.addData("hooks", "running");
        //telemetry.update();

        //set hooks positions to positions
        if(hooksHome)
        {
            leftHookPosition = LEFT_HOOK_HOME;
            rightHookPosition = RIGHT_HOOK_HOME;
        }
        else
        {
            leftHookPosition = LEFT_HOOK_EXTENDED;
            rightHookPosition = RIGHT_HOOK_EXTENDED;
        }
        leftHook.setPosition(leftHookPosition);
        rightHook.setPosition(rightHookPosition);

        telemetry.addData("Left Hook Position: ", leftHook.getPosition());
        telemetry.addData("Right Hook Position: ", rightHook.getPosition());
        telemetry.update();
    }
    //==============================================================================================   turnGyro
    public void turnGyro(double turningDegrees, double power, boolean turnRight)
    {
        telemetry.addData("turnGyro", "running");
        telemetry.update();

        backRightWheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        backLeftWheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        gyro.resetZAxisIntegrator();
        if(turnRight)
        {
            backRightWheel.setDirection(DcMotor.Direction.FORWARD);
            backLeftWheel.setDirection(DcMotor.Direction.FORWARD);

            backRightWheel.setPower(power);
            backLeftWheel.setPower(power);

            while(gyro.getHeading() + 180 < 180 - turningDegrees) {}
        }
        else
        {
            backRightWheel.setDirection(DcMotor.Direction.REVERSE);
            backLeftWheel.setDirection(DcMotor.Direction.REVERSE);

            backRightWheel.setPower(power);
            backLeftWheel.setPower(power);

            while(gyro.getHeading() + 180 < 180 + turningDegrees) {}
        }

        backRightWheel.setPower(0);
        backLeftWheel.setPower(0);

        backRightWheel.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        backLeftWheel.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }
}

