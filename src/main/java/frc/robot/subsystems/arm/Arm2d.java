package frc.robot.subsystems.arm;

import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.mechanism.LoggedMechanism2d;
import org.littletonrobotics.junction.mechanism.LoggedMechanismLigament2d;
import org.littletonrobotics.junction.mechanism.LoggedMechanismRoot2d;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.util.Color8Bit;

public class Arm2d {
	private LoggedMechanism2d mech;
	private LoggedMechanismLigament2d arm;
	private LoggedMechanismRoot2d root;

	private String name;

	public Arm2d(String name, Color8Bit color) {
		this.name = name;

		this.mech = new LoggedMechanism2d(2, 2);
		this.root = mech.getRoot("root", 0.5, 1);
		this.arm = root.append(new LoggedMechanismLigament2d("arm", 1, 0, 5, color));
	}

	public void setAngle(double angle) {
		arm.setAngle(angle);
	}

	public void periodic() {
		SmartDashboard.putData(name, mech);
		Logger.recordOutput(name, mech);
	}
}