package frc.robot.subsystems.arm;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.util.Color8Bit;

import frc.robot.devices.motor.Motor;
import frc.robot.devices.motor.MotorConfig;
import frc.robot.subsystems.SubsystemBase;
import frc.robot.util.Util;

import static frc.robot.subsystems.arm.ArmConstants.*;

import org.littletonrobotics.junction.Logger;

public class Arm extends SubsystemBase<Arm.Command> {
	public enum Command {
		DISABLED,
		IDLE,
		TRAVEL
	}

	private enum Travel {
		MOVING,
		HOLDING
	}

	private static Arm instance;

	private Motor motor;
	private double targetAngle_deg;

	private static final Arm2d arm2d = new Arm2d("Arm", new Color8Bit(255, 0, 0));

	public Arm() {
		super("Arm");

		MotorConfig config = new MotorConfig(MOTOR_ID)
			.withCanbus(CANBUS)
			.withBrake(BRAKE)
			.withFFGains(kS, kV, kA, kG)
			.withPIDGains(kP, kI, kD, GRAVITY_TYPE)
			.withMotionMagic(CRUISE_VELOCITY_rps, ACCELERATION_rps2, JERK_rps3)
			.withSim(MOTOR, GEARING, MOI_kgm2);

		this.motor = new Motor("Arm/motor", config);

		setCommand(Command.DISABLED);
	}

	public static Arm getInstance() {
		if (instance == null) {
			instance = new Arm();
		}

		return instance;
	}

	public void idle() {
		setCommand(Command.IDLE);
	}

	public void disable() {
		setCommand(Command.DISABLED);
	}

	public void rotateTo(double angle) {
		this.targetAngle_deg = MathUtil.clamp(angle, MIN_ANGLE_deg, MAX_ANGLE_deg);
		setCommand(Command.TRAVEL);
	}

	@Override
	protected void handle() {
		switch (getCommand()) {
			case DISABLED:
				motor.stop();
				break;
			case IDLE:
				motor.setMotionMagic(Units.degreesToRotations(targetAngle_deg));
				break;
			case TRAVEL:
				if (firstLoop()) {
					setSubstate(Travel.MOVING);
				}

				motor.setMotionMagic(Units.degreesToRotations(targetAngle_deg));

				switch ((Travel) getSubstate()) {
					case MOVING:
						if (atRotationTarget()) {
							setSubstate(Travel.HOLDING);
							break;
						}
						break;
					case HOLDING:
						if (!atRotationTarget()) {
							setSubstate(Travel.MOVING);
							break;
						}
						break;
					default:
						break;
				}
				break;
			default:
				break;
		}
	}

	@Override
	protected void inputPeriodic() {
		motor.readInputs();
	}

	@Override
	protected void outputPeriodic() {
		arm2d.setAngle(getAngle());
		arm2d.periodic();

		Logger.recordOutput("Arm/Angle_deg", getAngle());
		Logger.recordOutput("Arm/RotationalVelocity", getRotationalVelocity());
		Logger.recordOutput("Arm/Voltage_v", motor.getVoltage());
		Logger.recordOutput("Arm/TargetAngle_deg", targetAngle_deg);
	}

	public boolean atRotationTarget() {
		return Util.inRange(targetAngle_deg - getAngle(), TOLERANCE_deg);
	}

	public double getAngle() {
		return Units.rotationsToDegrees(motor.getPosition());
	}

	public double getRotationalVelocity() {
		return Units.rotationsToDegrees(motor.getVelocity());
	}
}