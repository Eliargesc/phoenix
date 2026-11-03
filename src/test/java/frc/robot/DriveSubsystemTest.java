// Description: Pruebas del pipeline de manejo del chasis.
// Authors: LamBot 3478

package frc.robot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.wpilibj.simulation.DriverStationSim;
import edu.wpi.first.wpilibj.simulation.SimHooks;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.robot.subsystems.drive.DriveIOSim;
import frc.robot.subsystems.drive.DriveSubsystem;

public class DriveSubsystemTest {

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
        DriverStationSim.setEnabled(true);
        DriverStationSim.setAutonomous(false);
        DriverStationSim.notifyNewData();
    }

    @AfterEach
    public void tearDown() {
        CommandScheduler.getInstance().cancelAll();
        CommandScheduler.getInstance().unregisterAllSubsystems();
        SimHooks.resumeTiming();
        HAL.shutdown();
    }

    // Llama arcadeDrive varias veces avanzando el reloj para que la rampa se estabilice
    private void settle(double forwardRaw, double turnRaw, boolean slow) {
        for (int i = 0; i < 50; i++) {
            SimHooks.stepTiming(0.02);
            drive.arcadeDrive(forwardRaw, turnRaw, slow);
        }
    }

    @Test
    public void neutralGivesZero() {
        settle(0.0, 0.0, false);
        assertEquals(0.0, io.getLeftOutput(), kEpsilon);
        assertEquals(0.0, io.getRightOutput(), kEpsilon);
    }

    @Test
    public void forwardMovesBothSidesEqually() {
        // Empujar adelante en el Xbox lee negativo
        settle(-1.0, 0.0, false);
        assertTrue(io.getLeftOutput() > 0.0);
        assertEquals(io.getLeftOutput(), io.getRightOutput(), kEpsilon);
    }

    @Test
    public void turnGivesOppositeSigns() {
        // Stick derecho a la derecha lee positivo: el robot gira en sentido horario
        settle(0.0, 1.0, false);
        assertTrue(io.getLeftOutput() > 0.0);
        assertTrue(io.getRightOutput() < 0.0);
    }

    @Test
    public void deadbandIgnoresSmallInput() {
        settle(-0.05, 0.05, false);
        assertEquals(0.0, io.getLeftOutput(), kEpsilon);
        assertEquals(0.0, io.getRightOutput(), kEpsilon);
    }

    @Test
    public void slowModeIsSlowerThanNormal() {
        settle(-1.0, 0.0, false);
        double normal = io.getLeftOutput();
        drive.stop();
        settle(-1.0, 0.0, true);
        double slow = io.getLeftOutput();
        assertTrue(slow > 0.0);
        assertTrue(slow < normal);
        assertTrue(drive.isSlowMode());
    }

    @Test
    public void slewLimitsFirstCycle() {
        SimHooks.stepTiming(0.02);
        drive.arcadeDrive(-1.0, 0.0, false);
        // Con el reloj pausado el primer ciclo no avanza nada: la rampa arranca en 0
        assertTrue(io.getLeftOutput() < 0.1);
    }

    @Test
    public void stopZeroesEverything() {
        settle(-1.0, 0.0, false);
        drive.stop();
        assertEquals(0.0, drive.getLeftDemand(), kEpsilon);
        assertEquals(0.0, drive.getRightDemand(), kEpsilon);
        assertEquals(0.0, io.getLeftOutput(), kEpsilon);
        assertEquals(0.0, io.getRightOutput(), kEpsilon);
    }

    @Test
    public void disabledPeriodicZeroes() {
        settle(-1.0, 0.0, false);
        DriverStationSim.setEnabled(false);
        DriverStationSim.notifyNewData();
        drive.periodic();
        assertEquals(0.0, io.getLeftOutput(), kEpsilon);
        assertEquals(0.0, io.getRightOutput(), kEpsilon);
    }

    @Test
    public void defaultCommandIgnoresSticksInAuto() {
        drive.setDefaultCommand(drive.teleopDriveCommand(() -> -1.0, () -> 0.0, () -> false));
        // Primero en teleop: debe haber salida real
        for (int i = 0; i < 20; i++) {
            SimHooks.stepTiming(0.02);
            CommandScheduler.getInstance().run();
        }
        assertTrue(io.getLeftOutput() > 0.0);
        // Cambia a autonomo con el stick a fondo: debe quedar en cero
        DriverStationSim.setAutonomous(true);
        DriverStationSim.setEnabled(true);
        DriverStationSim.notifyNewData();
        for (int i = 0; i < 5; i++) {
            SimHooks.stepTiming(0.02);
            CommandScheduler.getInstance().run();
        }
        assertEquals(0.0, io.getLeftOutput(), kEpsilon);
        assertEquals(0.0, io.getRightOutput(), kEpsilon);
        assertNotNull(drive.getCurrentCommand());
    }

    @Test
    public void defaultCommandDrivesInTeleop() {
        drive.setDefaultCommand(drive.teleopDriveCommand(() -> -1.0, () -> 0.0, () -> false));
        for (int i = 0; i < 20; i++) {
            SimHooks.stepTiming(0.02);
            CommandScheduler.getInstance().run();
        }
        assertTrue(io.getLeftOutput() > 0.0);
    }
}
