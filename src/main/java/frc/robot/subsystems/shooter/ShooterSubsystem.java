// Description: Subsistema del shooter: junta la logica, la IO y el dashboard.
// Authors: LamBot 3478
// Notes: La logica (estados, atasco, sensor) vive en ShooterLogic; el hardware en ShooterIO.

package frc.robot.subsystems.shooter;

import java.util.function.BooleanSupplier;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.ShooterConstants;
import frc.robot.subsystems.shooter.ShooterLogic.State;

public class ShooterSubsystem extends SubsystemBase {

    // Cada cuanto corre periodic(): 20 ms
    private static final double kPeriodSeconds = 0.02;

    private final ShooterIO io;
    private final ShooterLogic logic;
    private final ShooterIO.ShooterIOInputs inputs = new ShooterIO.ShooterIOInputs();

    // Peticiones actuales: las escriben los comandos, las lee periodic()
    private boolean wantIntake = false;
    private boolean wantShoot = false;
    private boolean wantEject = false;
    // True mientras el comando de teleop esta corriendo en autonomo: el operador no manda.
    private boolean operatorBlocked = false;

    // Constructor normal: usa las constantes reales
    public ShooterSubsystem(ShooterIO io) {
        this(io, ShooterLogic.fromConstants());
    }

    // Constructor para pruebas: recibe sus propios valores
    public ShooterSubsystem(ShooterIO io, ShooterLogic.Config config) {
        this.io = io;
        this.logic = new ShooterLogic(config);
    }

    @Override
    public void periodic() {
        io.updateInputs(inputs);
        logic.update(DriverStation.isEnabled() && !operatorBlocked, wantIntake, wantShoot, wantEject,
                inputs.tofConnected, inputs.tofDistanceMm, inputs.statorCurrentAmps, kPeriodSeconds);

        // Objetivo 0 (ESPERA o deshabilitado): se manda stop() EN CADA CICLO. Phoenix sigue
        // retransmitiendo la ultima orden; si no mandamos stop() tambien estando deshabilitado,
        // al rehabilitar el motor retomaria la velocidad anterior.
        double target = logic.getTargetRpm();
        if (target == 0.0) {
            io.stop();
        } else {
            io.setVelocityRpm(target);
        }

        publish(target);
    }

    private void publish(double target) {
        SmartDashboard.putString("Shooter/Estado", logic.getState().name());
        SmartDashboard.putString("Shooter/Motivo", logic.getReason());
        SmartDashboard.putNumber("Shooter/RPM objetivo", target);
        SmartDashboard.putNumber("Shooter/RPM real", inputs.velocityRpm);
        SmartDashboard.putNumber("Shooter/Corriente (A)", inputs.statorCurrentAmps);
        SmartDashboard.putNumber("Shooter/Distancia TOF (mm)", inputs.tofDistanceMm);
        SmartDashboard.putBoolean("Shooter/Listo (pieza detectada)", logic.hasPiece());
        SmartDashboard.putBoolean("Shooter/Sensor invalido", logic.isSensorInvalid());
        SmartDashboard.putBoolean("Shooter/Atasco", logic.isJamLatched());
        SmartDashboard.putBoolean("Shooter/Orden vieja ignorada", logic.isStaleOrderIgnored());
        // "En velocidad" solo tiene sentido si se pide moverse
        boolean atSpeed = target != 0.0
                && Math.abs(inputs.velocityRpm - target) <= ShooterConstants.kVelocityToleranceRpm;
        SmartDashboard.putBoolean("Shooter/En velocidad", atSpeed);
    }

    // Teleop: copia los tres botones a las peticiones cada ciclo.
    // IMPORTANTE: .ignoringDisable(true) hace que siga corriendo con el robot deshabilitado.
    // Asi la logica ve que botones estaban presionados al habilitar y los trata como "orden
    // vieja" (hay que soltarlos y volver a presionar). La logica ya garantiza salida 0 estando
    // deshabilitado, asi que esto no mueve nada.
    //
    // En autonomo el operador NO debe mover el shooter. Por eso, si es autonomo, el teleop se
    // marca como "bloqueado": la logica lo ve como deshabilitado (salida 0) pero los botones se
    // siguen copiando, asi los que sigan apretados al pasar a teleop cuentan como "orden vieja".
    // Los comandos de autonomo (intakeCommand, etc.) interrumpen a este comando; su finallyDo
    // pone operatorBlocked en false y la logica vuelve a verse habilitada.
    public Command teleopCommand(BooleanSupplier intake, BooleanSupplier shoot, BooleanSupplier eject) {
        return run(() -> {
            operatorBlocked = DriverStation.isAutonomous();
            wantIntake = intake.getAsBoolean();
            wantShoot = shoot.getAsBoolean();
            wantEject = eject.getAsBoolean();
        }).finallyDo(() -> {
            clearRequests();
            operatorBlocked = false;
        }).ignoringDisable(true).withName("ShooterTeleop");
    }

    // Para autonomo: una sola peticion. No define tiempos ni rutinas (eso lo decide el equipo).
    public Command intakeCommand() {
        return run(() -> setOnly(true, false, false)).finallyDo(this::clearRequests)
                .withName("ShooterIntake");
    }

    public Command shootCommand() {
        return run(() -> setOnly(false, true, false)).finallyDo(this::clearRequests)
                .withName("ShooterShoot");
    }

    public Command ejectCommand() {
        return run(() -> setOnly(false, false, true)).finallyDo(this::clearRequests)
                .withName("ShooterEject");
    }

    private void setOnly(boolean intake, boolean shoot, boolean eject) {
        wantIntake = intake;
        wantShoot = shoot;
        wantEject = eject;
    }

    private void clearRequests() {
        setOnly(false, false, false);
    }

    public State getState() {
        return logic.getState();
    }

    public double getTargetRpm() {
        return logic.getTargetRpm();
    }
}
