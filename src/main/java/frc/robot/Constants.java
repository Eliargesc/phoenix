// Description: Todas las constantes del robot en un solo lugar.
// Authors: LamBot 3478
// Notes: Etiquetas: [DECIDIDO] lo fijo el mentor, [PROVISIONAL] confirmar en el robot,
//        [SUGERIDO] valor inicial ajustable.

package frc.robot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.wpi.first.wpilibj.XboxController;

public final class Constants {

    private Constants() {}

    public static final class DriveConstants {

        private DriveConstants() {}

        // Tipos de controlador que puede haber en cada posicion del chasis
        public enum ControllerType { TALON_SRX, VICTOR_SPX }

        // Lado izquierdo: lider y seguidor
        public static final ControllerType kLeftLeaderType = ControllerType.TALON_SRX;    // [PROVISIONAL]
        public static final int kLeftLeaderId = 1;                                        // [PROVISIONAL]
        public static final ControllerType kLeftFollowerType = ControllerType.TALON_SRX;  // [PROVISIONAL]
        public static final int kLeftFollowerId = 2;                                      // [PROVISIONAL]

        // Lado derecho: lider y seguidor
        public static final ControllerType kRightLeaderType = ControllerType.TALON_SRX;   // [PROVISIONAL]
        public static final int kRightLeaderId = 3;                                       // [PROVISIONAL]
        public static final ControllerType kRightFollowerType = ControllerType.TALON_SRX; // [PROVISIONAL]
        public static final int kRightFollowerId = 4;                                     // [PROVISIONAL]

        // Cambiar a true despues de verificar los IDs en Phoenix Tuner  [PROVISIONAL]
        public static final boolean kCanIdsConfirmed = false;

        // La inversion se aplica SOLO por lado, en DriveIOCTRE con setInverted. Los dos
        // motores de un lado reciben la misma inversion (en una ToughBox Mini los dos CIM
        // giran igual). Nadie mas invierte: ni DriveSubsystem ni DifferentialDrive.
        public static final boolean kLeftInverted = false;   // [PROVISIONAL] se mide en bloques
        // El lado derecho esta espejeado respecto al izquierdo
        public static final boolean kRightInverted = true;   // [PROVISIONAL]

        // Freno al soltar el stick: el robot se detiene rapido  [DECIDIDO]
        public static final boolean kBrakeMode = true;

        // Zona muerta del stick: ignora el ruido cuando esta suelto  [SUGERIDO]
        public static final double kDeadband = 0.08;
        // Tope de salida en teleop (0 a 1)  [SUGERIDO]
        public static final double kMaxOutput = 1.0;
        // Multiplicador del modo lento  [SUGERIDO]
        // Con kSquareInputs = true el input se eleva al cuadrado despues: a stick completo
        // la salida lenta es aprox 0.6^2 = 36 %, no 60 %.
        public static final double kSlowModeMultiplier = 0.6;
        // Rampa de avance en unidades/s: 2.0 = de 0 a 1 en 0.5 s  [SUGERIDO]
        public static final double kForwardSlewRatePerSec = 2.0;
        // Rampa de giro en unidades/s  [SUGERIDO]
        public static final double kTurnSlewRatePerSec = 3.0;
        // Elevar al cuadrado el input da mas control fino a baja velocidad  [SUGERIDO]
        public static final boolean kSquareInputs = true;

        // Errores que bloquean el chasis: IDs duplicados o fuera de rango 0..62
        public static List<String> blockingErrors() {
            List<String> errors = new ArrayList<>();
            int[] ids = {kLeftLeaderId, kLeftFollowerId, kRightLeaderId, kRightFollowerId};
            Set<Integer> seen = new HashSet<>();
            for (int id : ids) {
                if (id < 0 || id > 62) {
                    errors.add("ID CAN fuera de rango 0..62: " + id);
                }
                if (!seen.add(id)) {
                    errors.add("ID CAN duplicado: " + id);
                }
            }
            return errors;
        }

        // True si hay errores que impiden usar el chasis real
        public static boolean hasBlockingErrors() {
            return !blockingErrors().isEmpty();
        }

        // Todos los problemas: errores bloqueantes mas avisos (IDs sin confirmar)
        public static List<String> validate() {
            List<String> problems = blockingErrors();
            if (!kCanIdsConfirmed) {
                problems.add("Los IDs CAN no estan confirmados (kCanIdsConfirmed = false)");
            }
            return problems;
        }
    }

    public static final class OperatorConstants {

        private OperatorConstants() {}

        public static final int kDriverControllerPort = 0;    // [DECIDIDO] default, ajustable
        public static final int kOperatorControllerPort = 1;  // [DECIDIDO] default, ajustable

        // Boton del modo lento del piloto: bumper izquierdo (LB)  [SUGERIDO]
        public static final int kSlowModeButton = XboxController.Button.kLeftBumper.value;

        // El Xbox lee negativo al empujar el stick hacia adelante  [SUGERIDO]
        public static final boolean kInvertForwardAxis = true;
        // En WPILib el giro positivo es antihorario, y el stick derecho da valor positivo  [SUGERIDO]
        public static final boolean kInvertTurnAxis = true;
    }

    public static final class AutoConstants {

        private AutoConstants() {}

        // Duracion del auto de avanzar  [DECIDIDO]
        public static final double kForwardSeconds = 3.0;
        // Potencia del auto de avanzar (0 a 1)  [DECIDIDO]
        public static final double kForwardPercent = 0.40;
        public static final String kDoNothingName = "No hacer nada";
        public static final String kForwardName = "Avanzar 3 segundos";
    }

    public static final class AllianceConstants {

        private AllianceConstants() {}

        // SOLO pruebas/simulacion: nunca reemplaza al FMS/DriverStation
        public static final String kFallbackChooserKey = "Alianza/Fallback (solo pruebas)";
    }
}
