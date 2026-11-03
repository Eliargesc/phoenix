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
import frc.robot.commands.Autos;
import frc.robot.subsystems.Subsistema1;
import frc.robot.subsystems.drive.DriveIOCTRE;
import frc.robot.subsystems.drive.DriveIOSim;
import frc.robot.subsystems.drive.DriveSubsystem;
import frc.robot.util.AllianceSelector;

public class RobotContainer {

    private final DriveSubsystem drive;
    // Listo para el reto: aun sin hardware
    @SuppressWarnings("unused")
    private final Subsistema1 subsistema1 = new Subsistema1();
    private final AllianceSelector alliance = new AllianceSelector();

    private final CommandXboxController driver =
            new CommandXboxController(OperatorConstants.kDriverControllerPort);
    @SuppressWarnings("unused")
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
        SmartDashboard.putBoolean("Config/OK", problems.isEmpty());

        autoChooser.setDefaultOption(AutoConstants.kDoNothingName, Autos.doNothing(drive));
        autoChooser.addOption(AutoConstants.kForwardName, Autos.forwardForTime(drive));
        SmartDashboard.putData("Autonomo/Selector", autoChooser);

        configureBindings();
    }

    // Controles: stick izquierdo avance, derecho giro, boton de modo lento (kSlowModeButton)
    private void configureBindings() {
        drive.setDefaultCommand(drive.teleopDriveCommand(
                driver::getLeftY, driver::getRightX, driver.button(OperatorConstants.kSlowModeButton)));

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
