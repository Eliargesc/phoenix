// Description: Subsistema del chasis tanque (arcade con rampa, zona muerta y modo lento).
// Authors: LamBot 3478
// Notes: La logica vive aqui; el hardware vive en DriveIO.

package frc.robot.subsystems.drive;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.DriveConstants;
import frc.robot.Constants.OperatorConstants;

public class DriveSubsystem extends SubsystemBase {

    private final DriveIO io;
    private final SlewRateLimiter forwardLimiter = new SlewRateLimiter(DriveConstants.kForwardSlewRatePerSec);
    private final SlewRateLimiter turnLimiter = new SlewRateLimiter(DriveConstants.kTurnSlewRatePerSec);

    private double leftDemand = 0.0;
    private double rightDemand = 0.0;
    private boolean slowMode = false;

    // Recibe la capa IO (real en el robot, falsa en simulacion y tests)
    public DriveSubsystem(DriveIO io) {
        this.io = io;
    }

    // Pipeline de teleop: sticks crudos -> salidas por lado
    public void arcadeDrive(double forwardRaw, double turnRaw, boolean slow) {
        slowMode = slow;

        // 1. Signos: el Xbox lee negativo al empujar adelante
        double forward = OperatorConstants.kInvertForwardAxis ? -forwardRaw : forwardRaw;
        double turn = OperatorConstants.kInvertTurnAxis ? -turnRaw : turnRaw;

        // 2. Zona muerta: el stick suelto nunca lee exactamente cero
        forward = MathUtil.applyDeadband(forward, DriveConstants.kDeadband);
        turn = MathUtil.applyDeadband(turn, DriveConstants.kDeadband);

        // 3. Modo lento para maniobras finas
        if (slow) {
            forward *= DriveConstants.kSlowModeMultiplier;
            turn *= DriveConstants.kSlowModeMultiplier;
        }

        // 4. Rampa: evita arranques bruscos que patinan o dan tirones
        forward = forwardLimiter.calculate(forward);
        turn = turnLimiter.calculate(turn);

        // 5. Mezcla arcade -> izquierda/derecha (calculo estatico, sin instanciar DifferentialDrive)
        DifferentialDrive.WheelSpeeds speeds =
                DifferentialDrive.arcadeDriveIK(forward, turn, DriveConstants.kSquareInputs);

        // 6. Tope de salida y envio
        setOutputs(speeds.left * DriveConstants.kMaxOutput, speeds.right * DriveConstants.kMaxOutput);
    }

    // Salida directa (autos): solo limita a [-1, 1]
    public void setOutputs(double left, double right) {
        leftDemand = MathUtil.clamp(left, -1.0, 1.0);
        rightDemand = MathUtil.clamp(right, -1.0, 1.0);
        io.setOutputs(leftDemand, rightDemand);
    }

    // Paro total: demandas en cero y rampas reiniciadas
    public void stop() {
        leftDemand = 0.0;
        rightDemand = 0.0;
        slowMode = false;
        forwardLimiter.reset(0.0);
        turnLimiter.reset(0.0);
        io.stop();
    }

    // Ultima demanda del lado izquierdo
    public double getLeftDemand() {
        return leftDemand;
    }

    // Ultima demanda del lado derecho
    public double getRightDemand() {
        return rightDemand;
    }

    // Si el modo lento esta activo
    public boolean isSlowMode() {
        return slowMode;
    }

    // Comando de manejo; es el default, asi que solo maneja en teleop habilitado.
    // En autonomo ("No hacer nada") el piloto no debe poder mover el robot.
    public Command teleopDriveCommand(DoubleSupplier forward, DoubleSupplier turn, BooleanSupplier slow) {
        return run(() -> {
            if (DriverStation.isTeleopEnabled()) {
                arcadeDrive(forward.getAsDouble(), turn.getAsDouble(), slow.getAsBoolean());
            } else {
                stop();
            }
        }).finallyDo(this::stop).withName("Manejo Arcade");
    }

    // Seguridad: deshabilitado = motores en cero; ademas publica datos al dashboard
    @Override
    public void periodic() {
        if (DriverStation.isDisabled()) {
            stop();
        }
        SmartDashboard.putNumber("Drive/DemandaIzquierda", leftDemand);
        SmartDashboard.putNumber("Drive/DemandaDerecha", rightDemand);
        SmartDashboard.putBoolean("Drive/ModoLento", slowMode);
    }
}
