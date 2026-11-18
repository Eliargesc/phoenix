// Description: Pruebas del subsistema del shooter con la IO falsa y el CommandScheduler real.
// Authors: LamBot 3478

package frc.robot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.wpilibj.simulation.DriverStationSim;
import edu.wpi.first.wpilibj.simulation.SimHooks;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.robot.subsystems.shooter.ShooterIOSim;
import frc.robot.subsystems.shooter.ShooterLogic;
import frc.robot.subsystems.shooter.ShooterLogic.State;
import frc.robot.subsystems.shooter.ShooterSubsystem;

public class ShooterSubsystemTest {

    private static final double kWall = 400.0;   // lectura "pared" (sin pieza)

    // Valores de PRUEBA, no calibrados: solo sirven para probar el subsistema
    private static final ShooterLogic.Config kTestConfig =
        new ShooterLogic.Config(1500, 3500, -1500, 30.0, 0.5, 30.0, 450.0, 200.0, 3);

    private ShooterIOSim io;
    private ShooterSubsystem shooter;

    @BeforeEach
    public void setUp() {
        assertTrue(HAL.initialize(500, 0), "no se pudo inicializar el HAL");
        SimHooks.pauseTiming();
        DriverStationSim.resetData();
        DriverStationSim.setAutonomous(false);
        DriverStationSim.setTest(false);
        CommandScheduler.getInstance().cancelAll();
        CommandScheduler.getInstance().unregisterAllSubsystems();
        io = new ShooterIOSim();
        io.setSimTofConnected(true);
        io.setSimTofDistanceMm(kWall);
        shooter = new ShooterSubsystem(io, kTestConfig);
        setEnabled(true);
    }

    @AfterEach
    public void tearDown() {
        CommandScheduler.getInstance().cancelAll();
        CommandScheduler.getInstance().unregisterAllSubsystems();
        SimHooks.resumeTiming();
        HAL.shutdown();
    }

    private void setEnabled(boolean enabled) {
        setEnabled(enabled, false);
    }

    private void setEnabled(boolean enabled, boolean autonomous) {
        DriverStationSim.setAutonomous(autonomous);
        DriverStationSim.setEnabled(enabled);
        DriverStationSim.notifyNewData();
    }

    // Un ciclo de 20 ms del planificador (corre periodic y los comandos)
    private void cycles(int n) {
        for (int i = 0; i < n; i++) {
            SimHooks.stepTiming(0.02);
            CommandScheduler.getInstance().run();
        }
    }

    private Command teleop(AtomicBoolean intake, AtomicBoolean shoot, AtomicBoolean eject) {
        return shooter.teleopCommand(intake::get, shoot::get, eject::get);
    }

    @Test
    public void intakeRequestRunsAtIntakeRpmAndReleaseStops() {
        Command intake = shooter.intakeCommand();
        CommandScheduler.getInstance().schedule(intake);
        cycles(3);
        assertEquals(1500.0, io.getLastCommandedRpm());
        assertEquals(State.RECOGER, shooter.getState());
        intake.cancel();
        cycles(2);
        assertTrue(io.isStopped());
        assertEquals(0.0, shooter.getTargetRpm());
    }

    @Test
    public void disablingWhileMovingStopsAndKeepsCallingStopEveryCycle() {
        Command shoot = shooter.shootCommand();
        CommandScheduler.getInstance().schedule(shoot);
        cycles(3);
        assertEquals(3500.0, io.getLastCommandedRpm());

        setEnabled(false);
        cycles(1);
        assertTrue(io.isStopped());
        int stopsBefore = io.getStopCount();
        cycles(5);
        assertTrue(io.isStopped());
        assertEquals(stopsBefore + 5, io.getStopCount());
    }

    @Test
    public void buttonHeldWhileDisabledDoesNotStartUntilReleasedAndPressedAgain() {
        AtomicBoolean intake = new AtomicBoolean(true);   // presionado desde deshabilitado
        AtomicBoolean shoot = new AtomicBoolean(false);
        AtomicBoolean eject = new AtomicBoolean(false);
        setEnabled(false);
        shooter.setDefaultCommand(teleop(intake, shoot, eject));
        cycles(5);
        // El comando por defecto corre deshabilitado gracias a ignoringDisable(true)
        assertTrue(CommandScheduler.getInstance().isScheduled(shooter.getDefaultCommand()));
        assertTrue(io.isStopped());

        setEnabled(true);
        cycles(5);
        assertEquals(State.ESPERA, shooter.getState());
        assertTrue(io.isStopped());

        intake.set(false);
        cycles(2);
        intake.set(true);
        cycles(3);
        assertEquals(State.RECOGER, shooter.getState());
        assertEquals(1500.0, io.getLastCommandedRpm());
    }

    @Test
    public void ejectWinsWithAllThreeButtons() {
        shooter.setDefaultCommand(teleop(new AtomicBoolean(true), new AtomicBoolean(true),
            new AtomicBoolean(true)));
        cycles(5);
        assertEquals(State.ESCUPIR, shooter.getState());
        assertEquals(-1500.0, io.getLastCommandedRpm());
    }

    @Test
    public void jamStopsMotorsButEjectStillWorks() {
        io.setSimStatorCurrentAmps(40.0);
        Command intake = shooter.intakeCommand();
        CommandScheduler.getInstance().schedule(intake);
        cycles(40);   // 0.8 s a 40 A, mas que el 0.5 s de atasco
        assertEquals(State.ESPERA, shooter.getState());
        assertTrue(io.isStopped());

        intake.cancel();
        Command eject = shooter.ejectCommand();
        CommandScheduler.getInstance().schedule(eject);
        cycles(3);
        assertEquals(State.ESCUPIR, shooter.getState());
        assertEquals(-1500.0, io.getLastCommandedRpm());
    }

    @Test
    public void onlyOneShooterCommandRunsAtATime() {
        Command teleop = teleop(new AtomicBoolean(false), new AtomicBoolean(false),
            new AtomicBoolean(false));
        shooter.setDefaultCommand(teleop);
        cycles(2);
        assertTrue(CommandScheduler.getInstance().isScheduled(teleop));

        Command shoot = shooter.shootCommand();
        CommandScheduler.getInstance().schedule(shoot);
        assertTrue(CommandScheduler.getInstance().isScheduled(shoot));
        assertFalse(CommandScheduler.getInstance().isScheduled(teleop));
        cycles(3);
        assertEquals(3500.0, io.getLastCommandedRpm());

        shoot.cancel();
        cycles(2);
        assertTrue(CommandScheduler.getInstance().isScheduled(teleop));
        assertFalse(CommandScheduler.getInstance().isScheduled(shoot));
        assertTrue(io.isStopped());
    }

    // ---------- El operador no mueve el shooter en autonomo ----------

    @Test
    public void triggerInAutonomousIsIgnored() {
        setEnabled(true, true);
        shooter.setDefaultCommand(teleop(new AtomicBoolean(true), new AtomicBoolean(false),
            new AtomicBoolean(false)));
        cycles(5);
        assertEquals(State.ESPERA, shooter.getState());
        assertTrue(io.isStopped());
    }

    @Test
    public void shootCommandWorksInAutonomousOverTheTeleopDefault() {
        setEnabled(true, true);
        shooter.setDefaultCommand(teleop(new AtomicBoolean(false), new AtomicBoolean(false),
            new AtomicBoolean(false)));
        cycles(3);
        Command shoot = shooter.shootCommand();
        CommandScheduler.getInstance().schedule(shoot);
        cycles(3);
        assertEquals(State.DISPARAR, shooter.getState());
        assertEquals(3500.0, io.getLastCommandedRpm());
        // Al terminar, el default vuelve y sigue bloqueado: queda detenido
        shoot.cancel();
        cycles(3);
        assertEquals(State.ESPERA, shooter.getState());
        assertTrue(io.isStopped());
    }

    @Test
    public void autonomousToTeleopWithoutDisableNeedsReleaseAndPressAgain() {
        AtomicBoolean intake = new AtomicBoolean(true);   // sostenido desde el autonomo
        shooter.setDefaultCommand(teleop(intake, new AtomicBoolean(false), new AtomicBoolean(false)));
        setEnabled(true, true);
        cycles(5);
        assertTrue(io.isStopped());

        setEnabled(true, false);   // pasa a teleop sin deshabilitar
        cycles(5);
        assertEquals(State.ESPERA, shooter.getState());
        assertTrue(io.isStopped());

        intake.set(false);
        cycles(2);
        intake.set(true);
        cycles(3);
        assertEquals(State.RECOGER, shooter.getState());
        assertEquals(1500.0, io.getLastCommandedRpm());
    }

    // ---------- finallyDo: al cancelar, todo queda detenido ----------

    private void assertCancelStops(Command command, State expectedWhileRunning, double rpm) {
        CommandScheduler.getInstance().schedule(command);
        cycles(3);
        assertEquals(expectedWhileRunning, shooter.getState());
        assertEquals(rpm, io.getLastCommandedRpm());
        command.cancel();
        cycles(2);
        assertTrue(io.isStopped());
        assertEquals(State.ESPERA, shooter.getState());
    }

    @Test
    public void cancelingIntakeCommandStops() {
        assertCancelStops(shooter.intakeCommand(), State.RECOGER, 1500.0);
    }

    @Test
    public void cancelingShootCommandStops() {
        assertCancelStops(shooter.shootCommand(), State.DISPARAR, 3500.0);
    }

    @Test
    public void cancelingEjectCommandStops() {
        assertCancelStops(shooter.ejectCommand(), State.ESCUPIR, -1500.0);
    }

    @Test
    public void cancelingTeleopDefaultStops() {
        shooter.setDefaultCommand(teleop(new AtomicBoolean(true), new AtomicBoolean(false),
            new AtomicBoolean(false)));
        cycles(3);
        assertEquals(1500.0, io.getLastCommandedRpm());
        shooter.removeDefaultCommand();
        CommandScheduler.getInstance().cancelAll();
        cycles(2);
        assertTrue(io.isStopped());
    }
}
