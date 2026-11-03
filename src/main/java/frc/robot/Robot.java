// Description: Ciclo de vida del robot (Command-based).
// Authors: LamBot 3478

package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;

public class Robot extends TimedRobot {

    private final RobotContainer container;
    private Command autonomousCommand;

    // Construye el contenedor (subsistemas y controles)
    public Robot() {
        container = new RobotContainer();
    }

    // Corre el scheduler siempre, habilitado o no
    @Override
    public void robotPeriodic() {
        CommandScheduler.getInstance().run();
        container.periodic();
    }

    // Agenda el auto elegido
    @Override
    public void autonomousInit() {
        autonomousCommand = container.getAutonomousCommand();
        if (autonomousCommand != null) {
            CommandScheduler.getInstance().schedule(autonomousCommand);
        }
    }

    // El auto no debe seguir corriendo al entrar a teleop
    @Override
    public void teleopInit() {
        if (autonomousCommand != null) {
            autonomousCommand.cancel();
        }
    }

    // Sin logica extra: el scheduler cancela y DriveSubsystem.periodic pone ceros
    @Override
    public void disabledInit() {}
}
