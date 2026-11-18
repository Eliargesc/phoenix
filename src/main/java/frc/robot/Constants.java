// Description: Todas las constantes del robot en un solo lugar.
// Authors: LamBot 3478
// Notes: Etiquetas: [DECIDIDO] lo fijo el mentor, [PROVISIONAL] confirmar en el robot,
//        [SUGERIDO] valor inicial ajustable, [POR CALIBRAR] numero que se mide en el robot
//        (queda en 0 hasta entonces), [POR VALIDAR EN EL ROBOT] hay que probarlo en bloques.

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

    // Calibracion del shooter: mientras falte calibrar algo, el robot usa el shooter SIMULADO
    // (los motores reales no se mueven). Para calibrar, usa Phoenix Tuner X: gira los motores
    // desde la pestana Control, lee la distancia del CANrange en Plot y valida ahi la inversion,
    // con el robot sobre bloques y un mentor presente. Luego copia los valores aqui.
    public static final class ShooterConstants {

        private ShooterConstants() {}

        // Elige la IO: la real solo si NO es simulacion y la configuracion esta lista
        public static boolean useRealIo(boolean isSimulation, boolean readyForHardware) {
            return !isSimulation && readyForHardware;
        }

        // IDs CAN y bus del shooter  [DECIDIDO]
        public static final int kLeaderId = 5;
        public static final int kFollowerId = 6;
        public static final int kTofId = 7;
        public static final String kCanBus = "rio";

        // Sentido positivo: RPM positivas = la pieza SUBE en el mecanismo.
        // TODO [POR VALIDAR EN EL ROBOT]: probar en bloques que RPM positivas suben la pieza.
        public static final boolean kLeaderInverted = false;
        // TODO [POR VALIDAR EN EL ROBOT]: probar en bloques si el seguidor gira contra el lider.
        public static final boolean kFollowerOpposesLeader = false;
        // TODO: cambiar a true tras validar en bloques las dos inversiones de arriba.
        public static final boolean kInversionValidated = false;

        // Coast: al soltar, los motores giran libres y no frenan de golpe  [DECIDIDO]
        public static final boolean kBrakeMode = false;

        // Velocidades objetivo en RPM. Se dejan en 0.0 a proposito: no inventamos numeros.
        public static final double kIntakeRpm = 0.0;  // TODO [POR CALIBRAR] positivo, sube la pieza
        public static final double kShootRpm = 0.0;   // TODO [POR CALIBRAR] positivo, mayor que kIntakeRpm
        public static final double kEjectRpm = 0.0;   // TODO [POR CALIBRAR] debe ser NEGATIVO
        // Que tan cerca de la meta cuenta como "ya llego" (en RPM)
        public static final double kVelocityToleranceRpm = 0.0;  // TODO [POR CALIBRAR]

        // Corriente: limite del motor y umbral de atasco
        public static final double kStatorCurrentLimitAmps = 0.0;  // TODO [POR CALIBRAR]
        // Debe ser MENOR que el limite de corriente: el TalonFX recorta la corriente justo
        // en el limite y nunca lo "excede", asi que un umbral igual o mayor jamas se cumple.
        public static final double kJamCurrentAmps = 0.0;  // TODO [POR CALIBRAR]
        // Debe ser mayor que lo que tarda el arranque normal (el arranque tambien gasta mucha corriente)
        public static final double kJamTimeSeconds = 0.0;  // TODO [POR CALIBRAR]

        // Sensor TOF: banda valida "pared a pared" (fuera de ella la lectura no sirve)
        // El minimo debe ser mayor que 0 para que la lectura 0 ("sin senal") cuente como invalida.
        public static final double kTofMinValidMm = 0.0;  // TODO [POR CALIBRAR]
        public static final double kTofMaxValidMm = 0.0;  // TODO [POR CALIBRAR]
        // Si la distancia es menor que esto, hay una pieza frente al sensor
        public static final double kPieceThresholdMm = 0.0;  // TODO [POR CALIBRAR]
        // Ciclos seguidos de 20 ms viendo pieza antes de creerle al sensor  [SUGERIDO]
        public static final int kPieceDebounceCycles = 3;

        // Un 0 en kP, kS o kV se trata como "sin calibrar" (aparece en el aviso del shooter).
        // Ganancias del control de velocidad. OJO con las unidades: Phoenix trabaja en
        // rotaciones por segundo (rps), el resto del codigo en RPM. La conversion RPM -> rps
        // (dividir entre 60) se hace en ShooterIOCTRE, no aqui: estas ganancias ya van en rps.
        public static final double kPVoltsPerRps = 0.0;         // TODO [POR CALIBRAR]
        public static final double kIVoltsPerRotation = 0.0;    // TODO [POR CALIBRAR] 0 es normal
        public static final double kDVoltsPerRpsPerSec = 0.0;   // TODO [POR CALIBRAR] 0 es normal
        public static final double kSVolts = 0.0;               // TODO [POR CALIBRAR]
        public static final double kVVoltsPerRps = 0.0;         // TODO [POR CALIBRAR]

        // Cuanto hay que apretar el gatillo para que cuente como presionado.
        // Es el valor por defecto de WPILib para rightTrigger()  [SUGERIDO]
        public static final double kTriggerThreshold = 0.5;

        // Todos los valores que se validan, juntos en un record para poder probar las reglas
        // con numeros de prueba (las constantes reales siguen en 0 hasta calibrar).
        public record Values(int leaderId, int followerId, int tofId,
                double intakeRpm, double shootRpm, double ejectRpm, double velocityToleranceRpm,
                double statorLimitAmps, double jamCurrentAmps, double jamTimeSeconds,
                double tofMinMm, double tofMaxMm, double pieceThresholdMm, int pieceDebounceCycles,
                double kP, double kI, double kD, double kS, double kV,
                boolean inversionValidated) {}

        // Los valores reales de este archivo
        public static Values currentValues() {
            return new Values(kLeaderId, kFollowerId, kTofId,
                kIntakeRpm, kShootRpm, kEjectRpm, kVelocityToleranceRpm,
                kStatorCurrentLimitAmps, kJamCurrentAmps, kJamTimeSeconds,
                kTofMinValidMm, kTofMaxValidMm, kPieceThresholdMm, kPieceDebounceCycles,
                kPVoltsPerRps, kIVoltsPerRotation, kDVoltsPerRpsPerSec, kSVolts, kVVoltsPerRps,
                kInversionValidated);
        }

        // Errores que bloquean el shooter: configuracion contradictoria o imposible.
        // Un 0 NO es error (significa "sin calibrar", eso lo reporta uncalibrated).
        public static List<String> blockingErrors() {
            return blockingErrors(currentValues());
        }

        public static List<String> blockingErrors(Values v) {
            List<String> errors = new ArrayList<>();
            int[] ids = {v.leaderId(), v.followerId(), v.tofId()};
            Set<Integer> seen = new HashSet<>();
            for (int id : ids) {
                if (id < 0 || id > 62) {
                    errors.add("ID CAN fuera de rango 0..62: " + id);
                }
                if (!seen.add(id)) {
                    errors.add("ID CAN duplicado: " + id);
                }
            }
            if (v.intakeRpm() < 0.0) {
                errors.add("kIntakeRpm no puede ser negativo (recoger sube la pieza)");
            }
            if (v.shootRpm() < 0.0) {
                errors.add("kShootRpm no puede ser negativo (disparar sube la pieza)");
            }
            if (v.ejectRpm() > 0.0) {
                errors.add("kEjectRpm debe ser negativo (escupir baja la pieza)");
            }
            // Solo se compara si kShootRpm ya se calibro (> 0): con 0 es "sin calibrar", no error
            if (v.shootRpm() > 0.0 && v.shootRpm() < v.intakeRpm()) {
                errors.add("kShootRpm no puede ser menor que kIntakeRpm");
            }
            if (v.velocityToleranceRpm() < 0.0) {
                errors.add("kVelocityToleranceRpm no puede ser negativa");
            }
            if (v.statorLimitAmps() < 0.0) {
                errors.add("kStatorCurrentLimitAmps no puede ser negativo");
            }
            if (v.jamCurrentAmps() < 0.0) {
                errors.add("kJamCurrentAmps no puede ser negativo");
            }
            if (v.jamTimeSeconds() < 0.0) {
                errors.add("kJamTimeSeconds no puede ser negativo");
            }
            if (v.jamCurrentAmps() > 0.0 && v.statorLimitAmps() > 0.0
                    && v.jamCurrentAmps() >= v.statorLimitAmps()) {
                errors.add("kJamCurrentAmps debe ser MENOR que kStatorCurrentLimitAmps");
            }
            if (v.tofMinMm() < 0.0) {
                errors.add("kTofMinValidMm no puede ser negativo");
            }
            if (v.tofMaxMm() < 0.0) {
                errors.add("kTofMaxValidMm no puede ser negativo");
            }
            // Solo se valida la banda si los dos valores ya se calibraron (> 0)
            if (v.tofMinMm() > 0.0 && v.tofMaxMm() > 0.0 && v.tofMinMm() >= v.tofMaxMm()) {
                errors.add("Banda TOF invalida: kTofMinValidMm debe ser menor que kTofMaxValidMm");
            }
            if (v.pieceThresholdMm() < 0.0) {
                errors.add("kPieceThresholdMm no puede ser negativo");
            }
            // Debe ser MENOR que la lectura de pared (el maximo): si fuera igual o mayor, toda
            // lectura valida contaria como pieza.
            if (v.tofMinMm() > 0.0 && v.tofMaxMm() > 0.0 && v.pieceThresholdMm() > 0.0
                    && !(v.pieceThresholdMm() > v.tofMinMm() && v.pieceThresholdMm() < v.tofMaxMm())) {
                errors.add("kPieceThresholdMm debe estar dentro de la banda TOF (mayor que el minimo, "
                    + "MENOR que el maximo)");
            }
            if (v.pieceDebounceCycles() < 1) {
                errors.add("kPieceDebounceCycles debe ser al menos 1");
            }
            if (v.kP() < 0.0 || v.kI() < 0.0 || v.kD() < 0.0 || v.kS() < 0.0 || v.kV() < 0.0) {
                errors.add("Las ganancias (kP, kI, kD, kS, kV) no pueden ser negativas");
            }
            return errors;
        }

        // Nombres de lo que sigue sin calibrar (en 0) y de la inversion sin validar.
        // kI y kD se omiten a proposito: 0 es un valor normal para ellas.
        public static List<String> uncalibrated() {
            return uncalibrated(currentValues());
        }

        public static List<String> uncalibrated(Values v) {
            List<String> pending = new ArrayList<>();
            addIfZero(pending, "kIntakeRpm", v.intakeRpm());
            addIfZero(pending, "kShootRpm", v.shootRpm());
            addIfZero(pending, "kEjectRpm", v.ejectRpm());
            addIfZero(pending, "kVelocityToleranceRpm", v.velocityToleranceRpm());
            addIfZero(pending, "kStatorCurrentLimitAmps", v.statorLimitAmps());
            addIfZero(pending, "kJamCurrentAmps", v.jamCurrentAmps());
            addIfZero(pending, "kJamTimeSeconds", v.jamTimeSeconds());
            addIfZero(pending, "kTofMinValidMm", v.tofMinMm());
            addIfZero(pending, "kTofMaxValidMm", v.tofMaxMm());
            addIfZero(pending, "kPieceThresholdMm", v.pieceThresholdMm());
            addIfZero(pending, "kPVoltsPerRps", v.kP());
            addIfZero(pending, "kSVolts", v.kS());
            addIfZero(pending, "kVVoltsPerRps", v.kV());
            if (!v.inversionValidated()) {
                pending.add("kInversionValidated (inversion sin validar en bloques)");
            }
            return pending;
        }

        // True si hay errores que impiden usar el shooter real
        public static boolean hasBlockingErrors() {
            return !blockingErrors().isEmpty();
        }

        // Todos los problemas: errores bloqueantes mas lo pendiente de calibrar
        public static List<String> validate() {
            List<String> problems = blockingErrors();
            problems.addAll(uncalibrated());
            return problems;
        }

        // True solo si no hay errores y no falta calibrar nada. El subsistema real
        // NO debe mover el mecanismo mientras esto sea false.
        public static boolean isReadyForHardware() {
            return blockingErrors().isEmpty() && uncalibrated().isEmpty();
        }

        private static void addIfZero(List<String> list, String name, double value) {
            if (value == 0.0) {
                list.add(name);
            }
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
