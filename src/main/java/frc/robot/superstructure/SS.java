package frc.robot.superstructure;

import frc.robot.ControlScheme;
import frc.robot.subsystems.SubsystemBase;
import frc.robot.subsystems.arm.Arm;
import frc.robot.subsystems.arm.ArmConstants;
import frc.robot.subsystems.drive.Drive;

import java.util.EnumSet;

import org.littletonrobotics.junction.Logger;

public class SS extends SubsystemBase<SS.Command> {

    public enum Flag {
        IDLE,
        DISABLE,
        RAISE,
        LOWER
    }

    public enum Command {
        IDLE,
        DISABLE,
        TRAVEL,
        STOWING,
        SCORING,
        MANUAL
    }

    private static final double MANUAL_VOLTS = 2.0;
    private static final double SCORE_LOW_HEIGHT_m = 1.2;
    private static final double SETTLE_TIME_s = 0.2;

    private static SS instance;

    private final EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);

    private final Arm arm;
    private final Drive drive;

    private double targetAngle_deg;

    public static SS getInstance() {
        if (instance == null) {
            instance = new SS();
        }
        return instance;
    }

    private SS() {
        super("Superstructure");
        arm = Arm.getInstance();
        drive = Drive.getInstance();
        setCommand(Command.IDLE);
    }


    public void enable(Flag flag) {
        flags.add(flag);
    }

    public void disable(Flag flag) {
        flags.remove(flag);
    }

    public void set(Flag flag, boolean active) {
        if (active) {
            flags.add(flag);
        } else {
            flags.remove(flag);
        }
    }

    public void toggle(Flag flag) {
        set(flag, !has(flag));
    }

    public boolean has(Flag flag) {
        return flags.contains(flag);
    }

    @Override
    protected void inputPeriodic() {}

    @Override
    protected void handle() {
        // Temp, realistically you would pull arm angle from another subsystem or array of pre-defined angles
        if (has(Flag.RAISE)) {
            this.targetAngle_deg = ArmConstants.MAX_ANGLE_deg;
            setCommand(Command.TRAVEL);
        } else if (has(Flag.LOWER)) {
            this.targetAngle_deg = ArmConstants.MIN_ANGLE_deg;
            setCommand(Command.TRAVEL);
        } else if (has(Flag.IDLE)) {
            setCommand(Command.IDLE);
        } else if (has(Flag.DISABLE)) {
            setCommand(Command.DISABLE);
        } else {
            setCommand(Command.STOWING);
        }

        switch (getCommand()) {
            case IDLE:
                arm.idle();
                break;
            case DISABLE:
                arm.disable();
                break;
            case TRAVEL:
                handleTravel();
                break;
            case MANUAL:
                break;
            case SCORING:
                break;
            case STOWING:
                break;
            default:
                break;
        }
    }

    public void handleTravel() {
        if (arm.atRotationTarget()) {
            setCommand(Command.IDLE);
            return;
        }

        arm.rotateTo(targetAngle_deg);
    }

    @Override
    protected void outputPeriodic() {
        String[] active = flags.stream().map(Enum::name).toArray(String[]::new);
        Logger.recordOutput("Superstructure/Flags", active);
    }
}
