package frc.robot.subsystems.arm;

import frc.robot.devices.motor.MotorConfig.GravityType;

public class ArmConstants {
	public static final int MOTOR_ID = 1;
	public static final String CANBUS = "";
	public static final boolean BRAKE = true;

	public static final double kP = 0;
	public static final double kI = 0;
	public static final double kD = 0;
	public static final GravityType GRAVITY_TYPE = GravityType.ARM;

	public static final double kS = 0;
	public static final double kG = 0;
	public static final double kV = 0;
	public static final double kA = 0;

	public static final double CRUISE_VELOCITY_rps = 0;
	public static final double ACCELERATION_rps2 = 0;
	public static final double JERK_rps3 = 0;

	public static final double MIN_ANGLE_deg = -90;
	public static final double MAX_ANGLE_deg = 90;
	public static final double TOLERANCE_deg = 3;
}