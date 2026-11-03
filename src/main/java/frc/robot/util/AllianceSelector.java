// Description: Alianza efectiva: la del DriverStation, o un fallback para pruebas.
// Authors: LamBot 3478
// Notes: El fallback nunca cambia la alianza oficial. Los autos actuales NO usan la
//        alianza (son simetricos); queda lista para cuando el reto la necesite.

package frc.robot.util;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Constants.AllianceConstants;

public class AllianceSelector {

    private final SendableChooser<Alliance> chooser = new SendableChooser<>();

    // Crea el selector de prueba y lo publica
    public AllianceSelector() {
        chooser.setDefaultOption("Azul (prueba)", Alliance.Blue);
        chooser.addOption("Rojo (prueba)", Alliance.Red);
        SmartDashboard.putData(AllianceConstants.kFallbackChooserKey, chooser);
    }

    // Prioridad al DriverStation; si no hay alianza, usa el selector (null = azul)
    public Alliance getEffectiveAlliance() {
        var fromDs = DriverStation.getAlliance();
        if (fromDs.isPresent()) {
            return fromDs.get();
        }
        Alliance selected = chooser.getSelected();
        return selected != null ? selected : Alliance.Blue;
    }

    // True si la alianza viene del DriverStation/FMS
    public boolean isFromDriverStation() {
        return DriverStation.getAlliance().isPresent();
    }

    // Publica alianza efectiva y su fuente al dashboard
    public void periodic() {
        SmartDashboard.putString("Alianza/Efectiva",
                getEffectiveAlliance() == Alliance.Red ? "Rojo" : "Azul");
        SmartDashboard.putString("Alianza/Fuente",
                isFromDriverStation() ? "DriverStation" : "Prueba (fallback)");
    }
}
