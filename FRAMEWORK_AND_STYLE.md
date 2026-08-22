# FRC Framework and Coding Style

This document is my working understanding of the framework in `26droptop` and the patterns proven in `frc2k26`. It is intentionally specific to this codebase, not a general command-based FRC template.

## Core architecture

- The robot uses a manually scheduled, AdvantageKit-based architecture—not WPILib command scheduling. `Robot.robotPeriodic()` explicitly runs controls, superstructure, and each subsystem in a deliberate order.
- Runtime mode is centralized in `Constants.currentMode`: `REAL`, `SIM`, or `REPLAY`. Real code uses physical IO, simulation uses simulated IO, and replay uses empty/no-op IO where appropriate.
- `Robot` owns startup, AdvantageKit metadata/receivers, singleton construction, periodic ordering, and mode lifecycle. Keep control bindings and autonomous selection out of individual mechanism classes.
- Subsystems are singletons accessed through `getInstance()`. Dependencies are constructed/obtained in the subsystem constructor rather than passed through unrelated layers.
- AdvantageKit is part of the design, not an afterthought: each IO wrapper has `@AutoLog` inputs; inputs are updated once per loop and logged with `Logger.processInputs(...)`; commands, states, timers, targets, and meaningful derived values are recorded as outputs.
- `PerfTracker` brackets each subsystem loop. Preserve the `input -> handle -> output` structure so timing and logs remain understandable.

## Subsystems

### Preferred current pattern: command + substate

New mechanisms should extend `SubsystemBase<Command>`:

1. Declare a public `Command` enum for externally requested behavior (for example `IDLE`, `HOMING`, `GO_TO_HEIGHT`, `MANUAL`).
2. Declare private enums for internal progress when a command needs stages (for example `Homing.SEEKING/SETTLED` and `Travel.MOVING/HOLDING`).
3. Initialize with `super("MechanismName")`, build devices/configuration, then `setCommand(Command.IDLE)`.
4. Implement `inputPeriodic()`, `handle()`, and `outputPeriodic()`.
5. Expose intention-revealing public methods (`home()`, `trackToHeight(...)`, `manual(...)`, `idle()`), instead of exposing state transitions to callers.

`SubsystemBase` guarantees this loop order:

```text
inputPeriodic() -> handle() -> firstLoop=false -> outputPeriodic()
```

`setCommand(...)` only resets `commandTimer`, substate, and first-loop state when the command actually changes. Use `firstLoop()` to initialize the command’s first internal substate. `setSubstate(...)` similarly resets `substateTimer`; use `substateInit()` or `substateElapsed(seconds)` for staged behavior. The base logs `Command`, `CommandTimer`, `Substate`, and `SubstateTimer` under the subsystem name.

Example shape (matching `Elevator`):

```java
case GO_TO_TARGET:
    if (firstLoop()) setSubstate(Travel.MOVING);
    switch ((Travel) getSubstate()) {
        case MOVING:
            device.setMotionMagic(target);
            if (atTarget(TOLERANCE_units)) setSubstate(Travel.HOLDING);
            break;
        case HOLDING:
            device.setMotionMagic(target);
            if (!atTarget(TOLERANCE_units)) setSubstate(Travel.MOVING);
            break;
    }
    break;
```

### Legacy/proven pattern: single state enum

The 2026 robot uses `StateMachineSubsystemBase<T extends Enum<T> & IState>`. Its flow is the same: `inputPeriodic()`, `handleStateMachine()`, and `outputPeriodic()`. States are enum values implementing the marker interface `IState`; transitions go through `queueState(...)`.

- `queueState(...)` resets the shared `timer` and makes `stateInit()` true once only when the state changes.
- State enums use uppercase names such as `IDLE`, `DISABLED`, `MANUAL`, `TRAVELLING`, `HOLDING`, and `HOMING`.
- Most mechanisms pair a target (`targetAngle_deg`, `targetHeight_m`) with `trackTo...(...)` and `isValueReached(...)` helpers; travel states command the actuator and holding states maintain the same target while detecting drift.

For new work, prefer the command/substate base. Use the older base only when extending a subsystem already built around `IState`/`queueState`.

## Superstructure and controls

- The superstructure (`SS`) owns mechanism coordination and prioritization. It is another stateful subsystem, not merely a collection of button callbacks.
- The current preferred public interface is an `EnumSet<Flag>` with `enable`, `disable`, `set`, `toggle`, and `has`. Controls continuously set flags; `SS.handle()` applies a priority chain and selects its command.
- Keep mechanism-level behavior in the mechanism and coordination/priority in `SS`. For example, `SS` chooses `SCORING` and target height, while `Elevator` handles homing, Motion Magic, tolerance, and holding.
- The prior robot uses `Intention` (operator request) and `InternalState` (superstructure progress). That is the same separation of external intent from internal implementation; use it when flags are not the clearest representation.
- `ControlScheme` converts OI input into named drive inputs and superstructure flags/intentions. It does not directly duplicate internal mechanism behavior. It has an initialization method to reset drive mode/overrides at teleop start.

## Adding a new device

Reusable hardware belongs under `frc.robot.devices.<device>` and follows this layout:

```text
Device.java                 public, mechanism-facing wrapper
DeviceConfig.java           chainable configuration data
DeviceIO.java               interface plus @AutoLog DeviceIOInputs
DeviceIOReal.java           real hardware implementation
DeviceIOSim.java            simulation implementation
DeviceIOInputsAutoLogged.java  generated by AdvantageKit
```

The wrapper owns the IO implementation selection based on `Constants.currentMode`, the generated inputs object, and logging. It exposes a small semantic API—e.g. `Motor.setMotionMagic(...)`, `CANrange.isDetected()`, `HallEffect.get()`—rather than leaking vendor objects into subsystems.

Device rules:

- `readInputs()` calls `io.updateInputs(inputs)` then `Logger.processInputs(name, inputs)`. Call it once from the owning subsystem’s `inputPeriodic()`.
- Name each device with its complete logging path, such as `"Elevator/Motor"` and `"Elevator/HallEffect"`.
- IO interfaces provide no-op `default` methods. This supports replay/placeholder IO and keeps the mechanism testable.
- Input field names carry units: `position`, `velocity`, `appliedVolts` in generic device APIs; mechanism-specific fields use suffixes such as `_m`, `_rps`, `_deg`, `_degps`, `_V`, and `_A`.
- Config objects have sensible defaults, a required identity constructor (CAN ID/channel/name), public data fields, and chainable `with...(...)` builders.
- Add real and simulation implementations together. Do not make simulation an afterthought.

`Motor` is the reference device: `MotorConfig` owns CAN, inversion, follower, brake/current limits, mechanism ratio, FF/PID/Motion Magic, and sim parameters; `Motor` records the selected `ControlType` and target setpoint whenever an output is commanded.

## Constants, names, and units

- Each mechanism gets a `MechanismConstants` class with a private constructor and `public static final` fields. Import it statically within the mechanism when it improves readability.
- Constants are grouped by hardware IDs/bus, electrical/configuration values, control gains, simulation values, limits, conversions, then operating values such as tolerance and homing voltage.
- Use uppercase constant names and encode units in names: `MAX_HEIGHT_m`, `HOMING_VOLTS`, `SUPPLY_CURRENT_LIMIT_A`, `MM_CRUISE_VELOCITY`, `HALL_DEBOUNCE_s`.
- Use camelCase for values and fields, with the same unit suffix: `targetHeight_m`, `voltsTarget`, `hallDetected`, `velocity_rps`, `toleranceDeg`.
- Use plural nouns for groups/collections (`flags`, `modules`), boolean predicates for state (`zeroed`, `hasTarget`, `isDetected`, `atTarget`), and action verbs for public behavior (`trackToHeight`, `setVoltage`, `zeroPosition`, `stop`).
- Use `get...` for measurements/accessors and `set...` for direct configuration or commands. `trackTo...` means closed-loop target tracking. `home()` initiates a homing routine.
- The code favors explicit local names (`x_`, `y_`, `w_`, `throttle`, `scoreTarget_m`) and explicit switch cases over clever indirection.

## Logging and safety expectations

- Log raw IO via `Logger.processInputs`; log commanded targets, flags, state/command, sensors, and calculated status in `outputPeriodic()`.
- Clamp externally requested motion targets to physical bounds before commanding hardware.
- Use sensors for homing/zeroing and make the completion state explicit (`zeroed`). Simulation may treat physical conditions as satisfied through `Robot.isSimulation()`.
- Make `IDLE`/`DISABLED` outputs safe and deterministic (`stop()` or zero voltage).
- Continue using brake mode, current limits, mechanism conversion ratios, and follower configuration in device configuration rather than scattering those settings through subsystem logic.

## Default implementation checklist

When I add a mechanism to this framework, I will:

1. Add a focused constants class with units in names.
2. Reuse a device wrapper or add the full wrapper/config/IO/real/sim stack.
3. Build the subsystem as a singleton using `SubsystemBase<Command>`.
4. Read and log device inputs in `inputPeriodic()`, implement commands/substates in `handle()`, and log targets/status/visualizations in `outputPeriodic()`.
5. Give `SS` the high-level request/priority logic and give `ControlScheme` the OI-to-flag mapping.
6. Instantiate it in `Robot`, add it to the explicit periodic order, and preserve REAL/SIM/REPLAY behavior.

This is the framework/style contract I will follow for future changes unless you direct otherwise.
