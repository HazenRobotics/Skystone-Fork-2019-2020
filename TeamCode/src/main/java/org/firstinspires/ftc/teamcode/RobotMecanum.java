
package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cRangeSensor;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.GyroSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

public class RobotMecanum// extends Robot
{

    protected String skystoneLocation = "";

    double P = 0.0135, I = 0.02025, D = 0;
    int integral,previous_error;
    double previousTime;

    final int TICK_PER_REVOLUTION = 1440;
    final double WHEEL_DIAMETER = 4;
    final int GEAR_RATIO = 2;

    DcMotor lift;
    final double MAX_LIFT_SPEED = 0.8;

    //Define Wheel Motors
    DcMotor frontLeftWheel;
    DcMotor backLeftWheel;
    DcMotor frontRightWheel;
    DcMotor backRightWheel;

    Servo leftHook;
    Servo rightHook;
    final double LEFT_HOOK_HOME = 1.0;
    final double RIGHT_HOOK_HOME = 1.0;
    final double LEFT_HOOK_EXTENDED = 0.5;
    final double RIGHT_HOOK_EXTENDED =0.5;
    double leftHookPosition = LEFT_HOOK_HOME;
    double rightHookPosition =  RIGHT_HOOK_HOME;


    Servo claw;
    final double CLAW_HOME = convertDegreeToPercent(0.0,180.0);
    final double CLAW_EXTENDED = convertDegreeToPercent(170.0,180.0);
    double clawPosition = CLAW_HOME;


    //Sensors
    GyroSensor gyro;
    ModernRoboticsI2cRangeSensor rangeSensorRightFront;
    ModernRoboticsI2cRangeSensor rangeSensorRightBack;
    ModernRoboticsI2cRangeSensor rangeSensorLeftFront;
    ModernRoboticsI2cRangeSensor rangeSensorLeftBack;

    //ColorSensor colorSensorRight;
    //ColorSensor colorSensorLeft;

    //=======================================================
    OpMode opMode;
    HardwareMap hardwareMap;
    //LinearOpMode opMode;
    Telemetry telemetry;

    TensorFlow tensorFlow;

    //==============================================================================================   Robot method
    public RobotMecanum(HardwareMap hMap, OpMode opMode)
    {


        hardwareMap = hMap;
        this.opMode = opMode;
        //this.opMode = (LinearOpMode) opMode;
        telemetry = opMode.telemetry;

        //super(hMap, opMode);

        telemetry.setAutoClear(true);

        lift = hardwareMap.dcMotor.get("lift");

        tensorFlow = new TensorFlow(hardwareMap, opMode);

        frontLeftWheel = hardwareMap.dcMotor.get("front_left_wheel");
        backLeftWheel = hardwareMap.dcMotor.get("back_left_wheel");
        frontRightWheel = hardwareMap.dcMotor.get("front_right_wheel");
        backRightWheel = hardwareMap.dcMotor.get("back_right_wheel");

        leftHook = hardwareMap.servo.get("left_hook");
        rightHook = hardwareMap.servo.get("right_hook");

        leftHook.setDirection(Servo.Direction.REVERSE);

        //Reverse the two flipped wheels
        frontLeftWheel.setDirection(DcMotor.Direction.REVERSE);
        backLeftWheel.setDirection(DcMotor.Direction.REVERSE);

        //Claw
        claw = hardwareMap.servo.get("claw");
        claw.setDirection(Servo.Direction.REVERSE);
        //claw.setPosition(CLAW_HOME);

        rangeSensorRightFront = hardwareMap.get(ModernRoboticsI2cRangeSensor.class, "range_sensor_right_front");
        rangeSensorRightBack = hardwareMap.get(ModernRoboticsI2cRangeSensor.class, "range_sensor_right_back");
        rangeSensorLeftFront = hardwareMap.get(ModernRoboticsI2cRangeSensor.class, "range_sensor_left_front");
        rangeSensorLeftBack = hardwareMap.get(ModernRoboticsI2cRangeSensor.class, "range_sensor_left_back");

        rangeSensorRightFront.setI2cAddress(I2cAddr.create8bit(0X78));
        rangeSensorRightBack.setI2cAddress(I2cAddr.create8bit(0X76));
        rangeSensorLeftFront.setI2cAddress(I2cAddr.create8bit(0X28));
        rangeSensorLeftBack.setI2cAddress(I2cAddr.create8bit(0X26));

        gyro = hardwareMap.gyroSensor.get("gyro");
        gyro.calibrate();
        while(gyro.isCalibrating());
    }
    public void printGyroHeading()
    {
        telemetry.addData("Gyro Heading", gyro.getHeading() );
        telemetry.update();
    }
    //==============================================================================================   convertDistTicks
    //method takes in 2nd parameter for circumfrence of spinning object
    public int convertDistTicks(double distanceToTravel, double circumfrence)
    {
        //1440 revolutions = 1 rotation
        //1 rotation = 4

        double revolutions = distanceToTravel / circumfrence;
        int totalTicks = (int) Math.round(revolutions * TICK_PER_REVOLUTION / GEAR_RATIO);

        return totalTicks;
    }
    public void moveOmni(double drivePower, double strafePower, double rotatePower)
    {
        double drive = Math.signum(drivePower) * Math.pow(drivePower, 4);
        double strafe = Math.signum(-strafePower) * Math.pow(strafePower, 4);
        double rotate = rotatePower;

        double frontLeftPower = drive + strafe + rotate;
        double backLeftPower = drive - strafe + rotate;
        double frontRightPower = drive - strafe - rotate;
        double backRightPower = drive + strafe - rotate;

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
    public void drive (double distance, double power)
    {
        backRightWheel.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        backLeftWheel.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        frontRightWheel.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        frontLeftWheel.setMode(DcMotor.RunMode.RUN_USING_ENCODER);


        backRightWheel.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        backLeftWheel.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        frontRightWheel.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        frontLeftWheel.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        backRightWheel.setTargetPosition(convertDistTicks(distance, WHEEL_DIAMETER * Math.PI));
        backLeftWheel.setTargetPosition(convertDistTicks(distance, WHEEL_DIAMETER * Math.PI));
        frontRightWheel.setTargetPosition(convertDistTicks(distance, WHEEL_DIAMETER * Math.PI));
        frontLeftWheel.setTargetPosition(convertDistTicks(distance, WHEEL_DIAMETER * Math.PI));

        moveOmni( power, 0, 0);


        backRightWheel.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        backLeftWheel.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        frontRightWheel.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        frontLeftWheel.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        while (backLeftWheel.isBusy())
        {
            telemetry.addData("encoder-fwdBL", backLeftWheel.getCurrentPosition() + "  busy=" + backLeftWheel.isBusy());
            telemetry.addData("encoder-fwdBR", backRightWheel.getCurrentPosition() + "  busy=" + backRightWheel.isBusy());
            telemetry.addData("encoder-fwdFL", backLeftWheel.getCurrentPosition() + "  busy=" + frontLeftWheel.isBusy());
            telemetry.addData("encoder-fwd", backRightWheel.getCurrentPosition() + "  busy=" + frontRightWheel.isBusy());
            telemetry.update();
        }

        // set motor power to zero to turn off motors. The motors stop on their own but
        // power is still applied so we turn off the power.
        backRightWheel.setPower(0.0);
        backLeftWheel.setPower(0.0);
        frontRightWheel.setPower(0.0);
        frontLeftWheel.setPower(0.0);


        backRightWheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        backLeftWheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        frontRightWheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        frontLeftWheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }
    public void strafe(double distance, double power)
    {
        frontLeftWheel.setTargetPosition(convertDistTicks(distance, WHEEL_DIAMETER * Math.PI));
        backLeftWheel.setTargetPosition(convertDistTicks(distance, WHEEL_DIAMETER * Math.PI));
        frontRightWheel.setTargetPosition(convertDistTicks(distance, WHEEL_DIAMETER * Math.PI));
        backRightWheel.setTargetPosition(convertDistTicks(distance, WHEEL_DIAMETER * Math.PI));
        moveOmni( 0, power, 0);
    }

    /**
     * + power and isRightSensor moves towards right wall
     * <br>- power and isRightSensor moves away from right wall
     * <br>+ power and !isRightSensor moves away from left wall
     * <br>- power and !isRightSensor moves towards left wall
     *
     * @param distanceFromWall distance to stop from the wall in inches
     * @param power power to run the motors. + moves right, - moves left
     * @param isRightSensor if the robot should use the right sensors
     *
     */
    public void strafeRange(int distanceFromWall, double power, boolean isRightSensor)
    {
        frontRightWheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        backRightWheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        frontLeftWheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        backLeftWheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        gyro.resetZAxisIntegrator();

        previousTime = opMode.getRuntime();

        moveOmni(0, power, 0);

        // while moving toward the right wall OR moving away from the left wall OR moving away from the right wall OR moving toward the left wall
        while((power > 0 && isRightSensor && (rangeSensorRightFront.getDistance(DistanceUnit.INCH) + rangeSensorRightBack.getDistance(DistanceUnit.INCH)) / 2 > distanceFromWall)
                || (power > 0 && !isRightSensor && (rangeSensorLeftFront.getDistance(DistanceUnit.INCH) + rangeSensorLeftBack.getDistance(DistanceUnit.INCH)) / 2 < distanceFromWall)
                || (power < 0 && isRightSensor && (rangeSensorLeftFront.getDistance(DistanceUnit.INCH) + rangeSensorLeftBack.getDistance(DistanceUnit.INCH)) / 2 < distanceFromWall)
                || (power < 0 && !isRightSensor && (rangeSensorLeftFront.getDistance(DistanceUnit.INCH) + rangeSensorLeftBack.getDistance(DistanceUnit.INCH)) / 2 > distanceFromWall)){
            moveOmni(0, power, gyroPID(180, opMode.getRuntime() - previousTime));
            telemetry.addData("distance from wall", rangeSensorRightFront.getDistance(DistanceUnit.INCH));
            telemetry.update();
            previousTime = opMode.getRuntime();
        }
        moveOmni(0,0,0);
    }

    public void incrementalDrive(double distance, double power, boolean isStrafe)
    {
        for(int i = 0; i < distance/2; i++)
        {
            if (isStrafe)
                strafe(2, power);
            else
                drive(2, power);
        }
    }
    public double convertDegreeToPercent(double desiredAngle, double maxAngle)
    {
        return desiredAngle/maxAngle;
    }
    public double gyroPID(int targetAngle, double time)
    {
        int error = targetAngle - getNewGyroHeading(); // Error = Target - Actual
        this.integral += (error * time); // Integral is increased by the error*time
        double derivative = (error - this.previous_error) / time;
        telemetry.addData("PID correction value:", P * error);
        telemetry.update();
        return P * error + I * this.integral + D * derivative;
    }
    public int getNewGyroHeading()
    {
        return (gyro.getHeading() + 180 ) % 360;
    }
    //==============================================================================================   turnGyro
    //not ready or programmed yet
    public void turnGyro(double turningDegrees, double power, boolean turnRight)
    {

        telemetry.addData("turnGyro", "running");

        frontRightWheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        backRightWheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        frontLeftWheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        backLeftWheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        gyro.resetZAxisIntegrator();
        if(turnRight)
        {
            //mostly works --- turned right 270 when I wanted it to move 90 right at 0.7 power
            moveOmni(0, 0, -power);
            while( getNewGyroHeading() > 180 - turningDegrees)
            {
                telemetry.addData("New While:", getNewGyroHeading() > 180 - turningDegrees);
                telemetry.addData("Heading + 180:", getNewGyroHeading());
                telemetry.update();
            }
            moveOmni(0, 0, 0);
        }
        //doesn't work --- had to leave didn't get to finishing this up.
        else
        {
            moveOmni(0, 0, power);
            while( getNewGyroHeading() < 180 + turningDegrees)
            {
                telemetry.addData("While:", getNewGyroHeading() < 180 + turningDegrees);
                telemetry.addData("Heading + 180:", getNewGyroHeading());
                telemetry.update();
            }
            moveOmni(0, 0, 0);
        }
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
    public void shuffle(double power, double distance)
    {
        for(int i = 0; i < 3; i++)
            drive(distance, power);
        for(int i = 0; i < 3; i++)
            drive(-distance, power);
    }
    public void tensorFlowDrive()
    {
        do
        {
            tensorFlow.tensorFlow();
            shuffle(0.2, 1);
        }while(tensorFlow.needsShuffle);

        if (tensorFlow.getSkystonePosition() == TensorFlow.Position.none)
            skystoneLocation = "none";
        else if (tensorFlow.getSkystonePosition() == TensorFlow.Position.left)
            skystoneLocation = "left";
        else if (tensorFlow.getSkystonePosition() == TensorFlow.Position.right)
            skystoneLocation = "right";
        else
            telemetry.addData("Error: ", "No Move");
        telemetry.update();
    }

}

