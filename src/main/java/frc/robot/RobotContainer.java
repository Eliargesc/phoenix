// Description: Arma subsistemas, controles, selector de autonomo y validaciones.
// Authors: LamBot 3478

package frc.robot;

import java.util.List;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.Constants.AutoConstants;
import frc.robot.Constants.DriveConstants;
import frc.robot.Constants.OperatorConstants;
import frc.robot.Constants.ShooterConstants;
import frc.robot.commands.Autos;
import frc.robot.subsystems.Subsistema1;
import frc.robot.subsystems.drive.DriveIOCTRE;
import frc.robot.subsystems.drive.DriveIOSim;
import frc.robot.subsystems.drive.DriveSubsystem;
import frc.robot.subsystems.shooter.ShooterIO;
import frc.robot.subsystems.shooter.ShooterIOCTRE;
import frc.robot.subsystems.shooter.ShooterIOSim;
import frc.robot.subsystems.shooter.ShooterSubsystem;
import frc.robot.util.AllianceSelector;

public class RobotContainer {

    private final DriveSubsystem drive;
    private final ShooterSubsystem shooter;
    // Listo para el reto: aun sin hardware
    @SuppressWarnings("unused")
    private final Subsistema1 subsistema1 = new Subsistema1();
    private final AllianceSelector alliance = new AllianceSelector();

    private final CommandXboxController driver =
            new CommandXboxController(OperatorConstants.kDriverControllerPort);
    private final CommandXboxController operator =
            new CommandXboxController(OperatorConstants.kOperatorControllerPort);

    private final SendableChooser<Command> autoChooser = new SendableChooser<>();

    // Valida la configuracion, publica el selector y conecta controles
    public RobotContainer() {
        List<String> problems = DriveConstants.validate();
        List<String> blocking = DriveConstants.blockingErrors();
        for (String problem : problems) {
            if (blocking.contains(problem)) {
                DriverStation.reportError(problem, false);
            } else {
                DriverStation.reportWarning(problem, false);
            }
        }
        if (!blocking.isEmpty()) {
            DriverStation.reportError("chasis deshabilitado: corrige los IDs en Constants", false);
        }
        // En simulacion no hay controladores CAN. Con IDs invalidos tampoco es seguro
        // hablarles: usamos la IO falsa y el chasis queda deshabilitado
        boolean useFakeIo = RobotBase.isSimulation() || DriveConstants.hasBlockingErrors();
        drive = new DriveSubsystem(useFakeIo ? new DriveIOSim() : new DriveIOCTRE());

        // Shooter: los errores bloqueantes se reportan como error; lo que falta calibrar,
        // como aviso. Mientras falte algo usamos la IO falsa: el shooter real no se mueve.
        List<String> shooterErrors = ShooterConstants.blockingErrors();
        for (String error : shooterErrors) {
            DriverStation.reportError(error, false);
        }
        List<String> shooterPending = ShooterConstants.uncalibrated();
        if (!shooterPending.isEmpty()) {
            DriverStation.reportWarning(
                    "shooter deshabilitado hasta calibrar: " + String.join(", ", shooterPending)
                    + ". Mientras tanto el robot usa el shooter simulado. Calibra con Phoenix Tuner X "
                    + "(gira los motores desde Control, lee la distancia del CANrange en Plot y valida "
                    + "ahi la inversion, con el robot sobre bloques y un mentor presente) y luego "
                    + "copia los valores en ShooterConstants", false);
        }
        boolean shooterReady = ShooterConstants.isReadyForHardware();
        ShooterIO shooterIo = ShooterConstants.useRealIo(RobotBase.isSimulation(), shooterReady)
                ? new ShooterIOCTRE() : new ShooterIOSim();
        shooter = new ShooterSubsystem(shooterIo);
        SmartDashboard.putBoolean("Shooter/ListoParaHardware", shooterReady);

        // Config/OK mira el chasis y los errores bloqueantes del shooter. Lo pendiente de
        // calibrar del shooter NO lo baja: seria false todo el tiempo hasta calibrar.
        SmartDashboard.putBoolean("Config/OK", problems.isEmpty() && shooterErrors.isEmpty());

        autoChooser.setDefaultOption(AutoConstants.kDoNothingName, Autos.doNothing(drive));
        autoChooser.addOption(AutoConstants.kForwardName, Autos.forwardForTime(drive));
        SmartDashboard.putData("Autonomo/Selector", autoChooser);

        configureBindings();
    }

    // Controles: stick izquierdo avance, derecho giro, boton de modo lento (kSlowModeButton)
    private void configureBindings() {
        drive.setDefaultCommand(drive.teleopDriveCommand(
                driver::getLeftY, driver::getRightX, driver.button(OperatorConstants.kSlowModeButton)));

        // Operador: gatillo derecho = recoger, gatillo izquierdo = disparar, B = escupir
        // Ojo: en modo Test habilitado el operador tambien puede mover el shooter (util en
        // los pits, con supervision). En autonomo no: ahi solo mandan los comandos de Autos.
        shooter.setDefaultCommand(shooter.teleopCommand(
                () -> operator.getRightTriggerAxis() > ShooterConstants.kTriggerThreshold,
                () -> operator.getLeftTriggerAxis() > ShooterConstants.kTriggerThreshold,
                () -> operator.getHID().getBButton()));

        // TODO (reto): bindings del operador, por ejemplo:
        // operator.a().whileTrue(subsistema1.algunComando());
    }

    // Autonomo elegido en el dashboard; si no hay, no hace nada
    public Command getAutonomousCommand() {
        Command selected = autoChooser.getSelected();
        return selected != null ? selected : Autos.doNothing(drive);
    }

    // Se llama cada ciclo desde Robot
    public void periodic() {
        alliance.periodic();
    }
}
