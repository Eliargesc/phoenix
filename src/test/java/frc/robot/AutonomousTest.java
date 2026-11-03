// Description: Pruebas de los autos: no hacer nada y avanzar por tiempo.
// Authors: LamBot 3478

package frc.robot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.wpilibj.simulation.DriverStationSim;
import edu.wpi.first.wpilibj.simulation.SimHooks;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.robot.Constants.AutoConstants;
import frc.robot.commands.Autos;
import frc.robot.subsystems.Subsistema1;
import frc.robot.subsystems.drive.DriveIOSim;
import frc.robot.subsystems.drive.DriveSubsystem;

public class AutonomousTest {

    private static final double kEpsilon = 1e-9;

    private DriveIOSim io;
    private DriveSubsystem drive;

    @BeforeEach
    public void setUp() {
        assertTrue(HAL.initialize(500, 0), "no se pudo inicializar el HAL");
        SimHooks.pauseTiming();
        CommandScheduler.getInstance().cancelAll();
        CommandScheduler.getInstance().unregisterAllSubsystems();
        io = new DriveIOSim();
        drive = new DriveSubsystem(io);
        DriverStationSim.setAutonomous(true);
        DriverStationSim.setEnabled(true);
        DriverStationSim.notifyNewData();
    }

    @AfterEach
    public void tearDown() {
        CommandScheduler.getInstance().cancelAll();
        CommandScheduler.getInstance().unregisterAllSubsystems();
        SimHooks.resumeTiming();
        HAL.shutdown();
    }

    private void step(double seconds) {
        SimHooks.stepTiming(seconds);
        CommandScheduler.getInstance().run();
    }

    @Test
    public void autoConstantsMatchDecision() {
        assertEquals(3.0, AutoConstants.kForwardSeconds, kEpsilon);
        assertEquals(0.40, AutoConstants.kForwardPercent, kEpsilon);
    }

    @Test
    public void doNothingStopsAndFinishes() {
        drive.setOutputs(0.5, 0.5);
        Command cmd = Autos.doNothing(drive);
        CommandScheduler.getInstance().schedule(cmd);
        step(0.02);
        assertFalse(cmd.isScheduled());
        assertEquals(0.0, io.getLeftOutput(), kEpsilon);
        assertEquals(0.0, io.getRightOutput(), kEpsilon);
    }

    @Test
    public void forwardRunsThenStops() {
        Command cmd = Autos.forwardForTime(drive);
        CommandScheduler.getInstance().schedule(cmd);
        step(0.02);
        step(2.88);
        assertTrue(cmd.isScheduled());
        assertEquals(AutoConstants.kForwardPercent, io.getLeftOutput(), kEpsilon);
        assertEquals(AutoConstants.kForwardPercent, io.getRightOutput(), kEpsilon);
        step(0.14);
        step(0.02);
        assertFalse(cmd.isScheduled());
        assertEquals(0.0, io.getLeftOutput(), kEpsilon);
        assertEquals(0.0, io.getRightOutput(), kEpsilon);
    }

    @Test
    public void disablingMidwayCancelsAndStops() {
        Command cmd = Autos.forwardForTime(drive);
        CommandScheduler.getInstance().schedule(cmd);
        step(0.02);
        step(1.0);
        assertTrue(io.getLeftOutput() > 0.0);
        DriverStationSim.setEnabled(false);
        DriverStationSim.notifyNewData();
        step(0.02);
        assertFalse(cmd.isScheduled());
        assertEquals(0.0, io.getLeftOutput(), kEpsilon);
        assertEquals(0.0, io.getRightOutput(), kEpsilon);
    }

    @Test
    public void containerDefaultAutoIsDoNothing() {
        assertEquals(AutoConstants.kDoNothingName, new RobotContainer().getAutonomousCommand().getName());
    }

    @Test
    public void subsistema1NeedsNoHardware() {
        assertNotNull(new Subsistema1());
    }

    @Test
    public void cancelMidwayStops() {
        Command cmd = Autos.forwardForTime(drive);
        CommandScheduler.getInstance().schedule(cmd);
        step(0.02);
        step(1.0);
        assertTrue(io.getLeftOutput() > 0.0);
        cmd.cancel();
        assertEquals(0.0, io.getLeftOutput(), kEpsilon);
        assertEquals(0.0, io.getRightOutput(), kEpsilon);
    }

    @Test
    public void autosRequireDrive() {
        assertTrue(Autos.doNothing(drive).getRequirements().contains(drive));
        assertTrue(Autos.forwardForTime(drive).getRequirements().contains(drive));
    }
}
