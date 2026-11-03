// Description: Fabrica de comandos autonomos.
// Authors: LamBot 3478
// Notes: Tiempo no es distancia: depende de la bateria, el piso y la reduccion.

package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.Constants.AutoConstants;
import frc.robot.subsystems.drive.DriveSubsystem;

public final class Autos {

    private Autos() {}

    // No hace nada: solo asegura el chasis detenido
    public static Command doNothing(DriveSubsystem drive) {
        return drive.runOnce(drive::stop).withName(AutoConstants.kDoNothingName);
    }

    // Avanza recto por tiempo; el finallyDo garantiza el paro aunque se cancele
    public static Command forwardForTime(DriveSubsystem drive) {
        return drive.run(() -> drive.setOutputs(AutoConstants.kForwardPercent, AutoConstants.kForwardPercent))
                .withTimeout(AutoConstants.kForwardSeconds)
                .finallyDo(drive::stop)
                .withName(AutoConstants.kForwardName);
    }
}
