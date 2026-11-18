// Description: Logica pura del shooter (estados, atasco, sensor). Sin WPILib ni CTRE para probarla en laptop.
// Authors: LamBot 3478
// Notes: Port de logicStep del simulador. Sin reintentos automaticos: tras un atasco
//        el operador debe soltar todo para reiniciar.

package frc.robot.subsystems.shooter;

import frc.robot.Constants.ShooterConstants;

public class ShooterLogic {

    public enum State { ESPERA, RECOGER, DISPARAR, ESCUPIR }

    // Todos los valores que la logica necesita, juntos para poder cambiarlos en pruebas
    public record Config(double intakeRpm, double shootRpm, double ejectRpm,
            double jamCurrentAmps, double jamTimeSeconds,
            double tofMinValidMm, double tofMaxValidMm, double pieceThresholdMm,
            int pieceDebounceCycles) {}

    public static Config fromConstants() {
        return new Config(
            ShooterConstants.kIntakeRpm, ShooterConstants.kShootRpm, ShooterConstants.kEjectRpm,
            ShooterConstants.kJamCurrentAmps, ShooterConstants.kJamTimeSeconds,
            ShooterConstants.kTofMinValidMm, ShooterConstants.kTofMaxValidMm,
            ShooterConstants.kPieceThresholdMm, ShooterConstants.kPieceDebounceCycles);
    }

    private final Config config;

    private State state = State.ESPERA;
    private String reason = "Sin orden";

    // "Orden vieja": boton que ya estaba presionado al deshabilitar. No cuenta hasta soltarlo.
    private boolean staleIntake = false;
    private boolean staleShoot = false;
    private boolean staleEject = false;
    private boolean staleOrderIgnored = false;

    private double jamSeconds = 0.0;
    private boolean jamLatched = false;
    private int jamEventCount = 0;

    private boolean full = false;      // recoger ya vio la pieza; se recuerda hasta soltar el boton
    private int pieceCycles = 0;
    private boolean hasPiece = false;
    private boolean sensorInvalid = false;

    public ShooterLogic(Config config) {
        this.config = config;
    }

    public State update(boolean enabled, boolean wantIntake, boolean wantShoot, boolean wantEject,
            boolean tofConnected, double tofDistanceMm, double statorCurrentAmps, double dtSeconds) {

        // 1. Sensor: si esta desconectado o fuera de la banda "pared a pared" no sirve.
        //    Solo avisamos y seguimos. Una sola lectura de pieza no basta (debounce).
        boolean valid = tofConnected
            && tofDistanceMm >= config.tofMinValidMm() && tofDistanceMm <= config.tofMaxValidMm();
        sensorInvalid = !valid;
        if (valid && tofDistanceMm < config.pieceThresholdMm()) {
            pieceCycles = Math.min(pieceCycles + 1, config.pieceDebounceCycles());
        } else {
            pieceCycles = 0;
        }
        hasPiece = pieceCycles >= config.pieceDebounceCycles();

        // 2. Deshabilitado: salida 0 y los botones aun presionados quedan como ordenes viejas
        if (!enabled) {
            staleIntake = wantIntake;
            staleShoot = wantShoot;
            staleEject = wantEject;
            staleOrderIgnored = false;
            jamSeconds = 0.0;
            full = false;
            state = State.ESPERA;
            reason = "Robot deshabilitado";
            return state;
        }

        // 3. Una orden vieja no cuenta hasta soltar y volver a presionar
        if (!wantIntake) staleIntake = false;
        if (!wantShoot) staleShoot = false;
        if (!wantEject) staleEject = false;
        boolean intake = wantIntake && !staleIntake;
        boolean shoot = wantShoot && !staleShoot;
        boolean eject = wantEject && !staleEject;
        staleOrderIgnored = (wantIntake && staleIntake) || (wantShoot && staleShoot)
            || (wantEject && staleEject);

        // 4. Soltar todo = reset manual del atasco
        if (!intake && !shoot && !eject) {
            jamLatched = false;
        }
        if (!intake) {
            full = false;
        }

        // 5. Prioridad: ESCUPIR > DISPARAR > RECOGER > ESPERA
        State want;
        String why;
        if (eject) {
            want = State.ESCUPIR;
        } else if (shoot) {
            want = State.DISPARAR;
        } else if (intake) {
            want = State.RECOGER;
        } else {
            want = State.ESPERA;
        }
        if (want == State.ESPERA) {
            why = staleOrderIgnored ? "Orden vieja: suelta y vuelve a presionar" : "Sin orden";
        } else {
            why = "Orden del operador";
        }

        // 6. Recoger se detiene cuando el sensor ve la pieza, y se queda detenido
        //    aunque la pieza deje de verse, hasta soltar el boton de recoger
        if (want == State.RECOGER) {
            if (hasPiece) full = true;
            if (full) {
                want = State.ESPERA;
                why = "Lleno: el sensor ya vio la pieza";
            }
        }

        // 7. Atasco: bloquea RECOGER y DISPARAR, nunca ESCUPIR
        if (want == State.RECOGER || want == State.DISPARAR) {
            if (jamLatched) {
                want = State.ESPERA;
                why = "Atasco: suelta todo para reiniciar, o escupe";
            }
        }
        if (want == State.RECOGER || want == State.DISPARAR) {
            if (statorCurrentAmps > config.jamCurrentAmps()) {
                jamSeconds += dtSeconds;
                if (jamSeconds >= config.jamTimeSeconds()) {
                    jamSeconds = 0.0;
                    jamEventCount++;
                    jamLatched = true;
                    want = State.ESPERA;
                    why = "Atasco detectado: motores a 0";
                }
            } else {
                jamSeconds = 0.0;
            }
        } else {
            jamSeconds = 0.0;
        }

        state = want;
        reason = why;
        return state;
    }

    public State getState() {
        return state;
    }

    // RPM que se le piden al motor; 0 en ESPERA
    public double getTargetRpm() {
        switch (state) {
            case RECOGER: return config.intakeRpm();
            case DISPARAR: return config.shootRpm();
            case ESCUPIR: return config.ejectRpm();
            default: return 0.0;
        }
    }

    // Texto en espanol para el dashboard
    public String getReason() {
        return reason;
    }

    public boolean hasPiece() {
        return hasPiece;
    }

    public boolean isSensorInvalid() {
        return sensorInvalid;
    }

    public boolean isJamLatched() {
        return jamLatched;
    }

    public boolean isStaleOrderIgnored() {
        return staleOrderIgnored;
    }

    public int getJamEventCount() {
        return jamEventCount;
    }
}
