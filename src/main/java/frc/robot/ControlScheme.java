package frc.robot;

import frc.robot.subsystems.drive.Drive;
import frc.robot.subsystems.drive.PathingMode;
import frc.robot.subsystems.drive.PathingOverride;
import frc.robot.subsystems.drive.SwerveInput;
import frc.robot.util.Util;


public class ControlScheme {

    private final Drive drive;

    public ControlScheme(Drive drive) {
        this.drive = drive;
    }

    public void init() {
        drive.queueState(PathingMode.FIELD_RELATIVE);
        drive.setPathingOverride(PathingOverride.NONE);
        System.out.println("Controls initialized");
    }

    public void update() {
        double x_ = OI.deadband(-OI.DR.getLeftY());
        double y_ = OI.deadband(-OI.DR.getLeftX());
        double w_ = 0.5 * -Util.sqInput(OI.deadband(OI.DR.getRightX()));
        double throttle = Util.sqInput(
                1.0 - OI.deadband(Math.max(OI.DR.getLeftTriggerAxis(), OI.DR.getRightTriggerAxis())));

        if (OI.DR.getPOV() == 180) {
            drive.zeroGyro();
        }
        drive.setInput(new SwerveInput(x_, y_, w_, throttle));

    }
}
