package frc.robot.subsystems.arm;

import edu.wpi.first.math.system.plant.DCMotor;
import frc.robot.devices.motor.MotorConfig.GravityType;

public class ArmConstants {
	public static final int MOTOR_ID = 1;
	public static final String CANBUS = "";
	public static final boolean BRAKE = true;
	public static final DCMotor MOTOR = DCMotor.getFalcon500Foc(1);

	public static final double kP = 5;
	public static final double kI = 2.5;
	public static final double kD = 9;
	public static final GravityType GRAVITY_TYPE = GravityType.ARM;

	public static final double kS = 0.05;
	public static final double kG = 0.05;
	public static final double kV = 0.5;
	public static final double kA = 0.5;

	public static final double CRUISE_VELOCITY_rps = 0.25;
	public static final double ACCELERATION_rps2 = 1;
	public static final double JERK_rps3 = 4;

	public static final double MIN_ANGLE_deg = -90;
	public static final double MAX_ANGLE_deg = 90;
	public static final double TOLERANCE_deg = 3;

	public static final double GEARING = 1;
	public static final double MOI_kgm2 = 0.5;
}