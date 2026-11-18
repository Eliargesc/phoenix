// Description: Pruebas de la logica pura del shooter (P1..P8 siguen al simulador).
// Authors: LamBot 3478

package frc.robot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import frc.robot.Constants.ShooterConstants;
import frc.robot.subsystems.shooter.ShooterLogic;
import frc.robot.subsystems.shooter.ShooterLogic.State;

public class ShooterLogicTest {

    private static final double kDt = 0.02;
    private static final double kWall = 400.0;   // lectura "pared" (sin pieza)
    private static final double kPiece = 80.0;   // lectura con pieza

    // Valores de PRUEBA, no calibrados: solo sirven para probar la logica
    private static final ShooterLogic.Config kTestConfig =
        new ShooterLogic.Config(1500, 3500, -1500, 30.0, 0.5, 30.0, 450.0, 200.0, 3);

    private ShooterLogic logic;
    private boolean enabled;
    private boolean tofConnected;

    @BeforeEach
    public void setUp() {
        logic = new ShooterLogic(kTestConfig);
        enabled = true;
        tofConnected = true;
    }

    // Un ciclo de 20 ms con las lecturas dadas
    private State step(boolean intake, boolean shoot, boolean eject, double tofMm, double amps) {
        return logic.update(enabled, intake, shoot, eject, tofConnected, tofMm, amps, kDt);
    }

    private void steps(int n, boolean intake, boolean shoot, boolean eject, double tofMm, double amps) {
        for (int i = 0; i < n; i++) {
            step(intake, shoot, eject, tofMm, amps);
        }
    }

    @Test
    public void p1IntakeStopsOnPieceAndStaysStoppedUntilRelease() {
        steps(5, true, false, false, kWall, 5);
        assertEquals(State.RECOGER, logic.getState());
        steps(3, true, false, false, kPiece, 5);
        assertEquals(State.ESPERA, logic.getState());
        assertEquals(0.0, logic.getTargetRpm());
        // La pieza deja de verse pero el boton sigue: se queda detenido
        steps(10, true, false, false, kWall, 5);
        assertEquals(State.ESPERA, logic.getState());
        // Soltar y volver a presionar arranca de nuevo
        step(false, false, false, kWall, 0);
        step(true, false, false, kWall, 5);
        assertEquals(State.RECOGER, logic.getState());
    }

    @Test
    public void p2ShootWorksWithAndWithoutPieceAndReturnsToIdle() {
        step(false, true, false, kWall, 5);
        assertEquals(State.DISPARAR, logic.getState());
        assertEquals(3500, logic.getTargetRpm());
        steps(5, false, true, false, kPiece, 5);
        assertEquals(State.DISPARAR, logic.getState());
        step(false, false, false, kWall, 0);
        assertEquals(State.ESPERA, logic.getState());
        assertEquals(0.0, logic.getTargetRpm());
    }

    @Test
    public void p3EjectWinsWithAllThreeButtonsAndNeverPositive() {
        for (int i = 0; i < 20; i++) {
            step(true, true, true, i % 2 == 0 ? kPiece : kWall, 5);
            assertEquals(State.ESCUPIR, logic.getState());
            assertEquals(-1500.0, logic.getTargetRpm());
        }
    }

    @Test
    public void p4JamStopsDoesNotRetryAndEjectWorksAndReleaseResets() {
        // 0.5 s a 40 A = 25 ciclos. No dependemos de la suma exacta de punto flotante:
        // no atascado en el ciclo 24, atascado a mas tardar en el 26
        for (int i = 0; i < 24; i++) {
            step(true, false, false, kWall, 40);
        }
        assertFalse(logic.isJamLatched());
        assertEquals(State.RECOGER, logic.getState());
        State s = State.RECOGER;
        for (int i = 0; i < 2; i++) {
            s = step(true, false, false, kWall, 40);
        }
        assertEquals(State.ESPERA, s);
        assertTrue(logic.isJamLatched());
        assertEquals(1, logic.getJamEventCount());
        // Boton aun presionado: cero reintentos, incluso con corriente baja
        for (int i = 0; i < 100; i++) {
            step(true, false, false, kWall, 0);
            assertEquals(State.ESPERA, logic.getState());
            assertEquals(0.0, logic.getTargetRpm());
        }
        assertEquals(1, logic.getJamEventCount());
        // Escupir funciona enganchado
        step(true, false, true, kWall, 0);
        assertEquals(State.ESCUPIR, logic.getState());
        assertEquals(-1500.0, logic.getTargetRpm());
        // Soltar todo reinicia
        step(false, false, false, kWall, 0);
        assertFalse(logic.isJamLatched());
        step(true, false, false, kWall, 5);
        assertEquals(State.RECOGER, logic.getState());
    }

    @Test
    public void p5InvalidSensorWarnsIntakeKeepsRunningShootWorks() {
        // Desconectado
        tofConnected = false;
        steps(10, true, false, false, kPiece, 5);
        assertTrue(logic.isSensorInvalid());
        assertEquals(State.RECOGER, logic.getState());
        assertFalse(logic.hasPiece());
        tofConnected = true;
        // Lectura 0 (sin senal)
        steps(10, true, false, false, 0.0, 5);
        assertTrue(logic.isSensorInvalid());
        assertEquals(State.RECOGER, logic.getState());
        // Lectura sobre la banda
        steps(10, true, false, false, 600.0, 5);
        assertTrue(logic.isSensorInvalid());
        assertEquals(State.RECOGER, logic.getState());
        // Disparar sigue funcionando
        step(false, true, false, 0.0, 5);
        assertEquals(State.DISPARAR, logic.getState());
        // Con lectura buena el aviso se apaga
        step(false, false, false, kWall, 0);
        assertFalse(logic.isSensorInvalid());
    }

    @Test
    public void p6DisabledIsIdleAndEnableWithHeldButtonStaysIdle() {
        steps(5, true, false, false, kWall, 5);
        assertEquals(State.RECOGER, logic.getState());
        enabled = false;
        steps(5, true, false, false, kWall, 0);
        assertEquals(State.ESPERA, logic.getState());
        assertEquals(0.0, logic.getTargetRpm());
        // Habilitar con el boton aun presionado: orden vieja, sigue en ESPERA
        enabled = true;
        steps(5, true, false, false, kWall, 0);
        assertEquals(State.ESPERA, logic.getState());
        assertTrue(logic.isStaleOrderIgnored());
        // Soltar y presionar arranca
        step(false, false, false, kWall, 0);
        assertFalse(logic.isStaleOrderIgnored());
        step(true, false, false, kWall, 5);
        assertEquals(State.RECOGER, logic.getState());
    }

    @Test
    public void p7ShortCurrentSpikeIsNotAJam() {
        // 20 ciclos = 0.4 s < 0.5 s
        steps(20, false, true, false, kWall, 40);
        step(false, true, false, kWall, 5);
        steps(20, false, true, false, kWall, 40);
        assertEquals(State.DISPARAR, logic.getState());
        assertFalse(logic.isJamLatched());
        assertEquals(0, logic.getJamEventCount());
    }

    @Test
    public void p8PieceWithoutButtonsStaysIdle() {
        for (int i = 0; i < 50; i++) {
            step(false, false, false, kPiece, 0);
            assertEquals(State.ESPERA, logic.getState());
            assertEquals(0.0, logic.getTargetRpm());
        }
        assertTrue(logic.hasPiece());
    }

    @Test
    public void intakePlusShootIsShoot() {
        step(true, true, false, kWall, 5);
        assertEquals(State.DISPARAR, logic.getState());
    }

    @Test
    public void singlePieceReadingBelowDebounceDoesNotStopIntake() {
        steps(3, true, false, false, kWall, 5);
        step(true, false, false, kPiece, 5);
        step(true, false, false, kPiece, 5);   // solo 2 de 3 ciclos
        assertEquals(State.RECOGER, logic.getState());
        step(true, false, false, kWall, 5);    // se rompe la racha
        step(true, false, false, kPiece, 5);
        assertEquals(State.RECOGER, logic.getState());
    }

    @Test
    public void shooterConstantsHaveNoBlockingErrorsButAreNotReadyForHardware() {
        assertTrue(ShooterConstants.blockingErrors().isEmpty(),
            "errores: " + ShooterConstants.blockingErrors());
        assertFalse(ShooterConstants.uncalibrated().isEmpty());
        assertFalse(ShooterConstants.isReadyForHardware());
    }

    // ---------- Huecos que encontro la auditoria ----------

    @Test
    public void jamTimerResetsWhenLeavingIntakeOrShoot() {
        // 20 ciclos (0.4 s) a 40 A recogiendo: aun no hay atasco
        steps(20, true, false, false, kWall, 40);
        // Un ciclo escupiendo con corriente alta: el temporizador debe reiniciarse
        step(false, false, true, kWall, 40);
        // 20 ciclos mas (0.4 s): si NO se hubiera reiniciado sumarian 0.8 s y habria atasco
        steps(20, true, false, false, kWall, 40);
        assertFalse(logic.isJamLatched());
        assertEquals(State.RECOGER, logic.getState());
        // Lo mismo pasando por ESPERA (soltar todo un ciclo)
        step(false, false, false, kWall, 40);
        steps(20, false, true, false, kWall, 40);
        assertFalse(logic.isJamLatched());
        assertEquals(State.DISPARAR, logic.getState());
    }

    // Deja presionado recoger desde deshabilitado y habilita: recoger queda como orden vieja
    private void holdIntakeThroughDisable() {
        enabled = false;
        steps(3, true, false, false, kWall, 0);
        enabled = true;
    }

    @Test
    public void staleButtonPlusNewButtonNewWorksOldStaysIgnored() {
        holdIntakeThroughDisable();
        step(true, false, true, kWall, 0);
        assertEquals(State.ESCUPIR, logic.getState());
        assertTrue(logic.isStaleOrderIgnored());
        // Al soltar el boton nuevo, el viejo sigue ignorado
        step(true, false, false, kWall, 0);
        assertEquals(State.ESPERA, logic.getState());
        assertTrue(logic.isStaleOrderIgnored());
    }

    @Test
    public void staleIntakeDoesNotBlockShoot() {
        holdIntakeThroughDisable();
        step(true, true, false, kWall, 0);
        assertEquals(State.DISPARAR, logic.getState());
        assertEquals(3500.0, logic.getTargetRpm());
    }

    @Test
    public void currentExactlyAtThresholdIsNotAJam() {
        // Exactamente 30 A: la comparacion es ">" estricta, no cuenta
        steps(100, true, false, false, kWall, 30.0);
        assertEquals(State.RECOGER, logic.getState());
        assertFalse(logic.isJamLatched());
        // Un poco arriba si cuenta
        step(false, false, false, kWall, 0);
        steps(30, true, false, false, kWall, 30.01);
        assertTrue(logic.isJamLatched());
    }

    @Test
    public void tofReadingsAtBandEdgesAreValidJustOutsideAreInvalid() {
        step(false, false, false, 30.0, 0);
        assertFalse(logic.isSensorInvalid());
        step(false, false, false, 450.0, 0);
        assertFalse(logic.isSensorInvalid());
        step(false, false, false, 29.99, 0);
        assertTrue(logic.isSensorInvalid());
        step(false, false, false, 450.01, 0);
        assertTrue(logic.isSensorInvalid());
    }

    @Test
    public void ejectWithSustainedHighCurrentNeverJams() {
        for (int i = 0; i < 200; i++) {
            step(false, false, true, kWall, 80);
            assertEquals(State.ESCUPIR, logic.getState());
            assertFalse(logic.isJamLatched());
        }
        assertEquals(0, logic.getJamEventCount());
    }

    @Test
    public void reasonTextsMatch() {
        step(false, false, false, kWall, 0);
        assertEquals("Sin orden", logic.getReason());
        step(true, false, false, kWall, 5);
        assertEquals("Orden del operador", logic.getReason());

        // Lleno
        step(false, false, false, kWall, 0);
        steps(5, true, false, false, kPiece, 5);
        assertEquals("Lleno: el sensor ya vio la pieza", logic.getReason());

        // Atasco
        step(false, false, false, kWall, 0);
        for (int i = 0; i < 26; i++) {
            step(true, false, false, kWall, 40);
        }
        assertEquals("Atasco: suelta todo para reiniciar, o escupe", logic.getReason());

        // Deshabilitado
        enabled = false;
        step(false, false, false, kWall, 0);
        assertEquals("Robot deshabilitado", logic.getReason());

        // Orden vieja
        step(true, false, false, kWall, 0);
        enabled = true;
        step(true, false, false, kWall, 0);
        assertEquals("Orden vieja: suelta y vuelve a presionar", logic.getReason());
    }

    @Test
    public void jamReasonAtTheMomentOfDetection() {
        String reason = "";
        for (int i = 0; i < 26 && !logic.isJamLatched(); i++) {
            step(true, false, false, kWall, 40);
            reason = logic.getReason();
        }
        assertTrue(logic.isJamLatched());
        assertEquals("Atasco detectado: motores a 0", reason);
    }

    @Test
    public void latchedJamIsReleasedByDisableAndEnableWithButtonStillHeld() {
        for (int i = 0; i < 26; i++) {
            step(true, false, false, kWall, 40);
        }
        assertTrue(logic.isJamLatched());
        enabled = false;
        step(true, false, false, kWall, 0);
        enabled = true;
        // El boton sigue presionado, pero es orden vieja: los botones efectivos estan sueltos
        step(true, false, false, kWall, 0);
        assertFalse(logic.isJamLatched());
        assertEquals(State.ESPERA, logic.getState());
        assertTrue(logic.isStaleOrderIgnored());
    }

    @Test
    public void fullIsKeptThroughShootAndClearedWhenIntakeIsReleased() {
        steps(5, true, false, false, kPiece, 5);
        assertEquals(State.ESPERA, logic.getState());
        // Disparar con recoger aun presionado (la pieza ya no se ve)
        step(true, true, false, kWall, 5);
        assertEquals(State.DISPARAR, logic.getState());
        // Soltar disparar: recoger sigue presionado y "lleno" se conserva
        step(true, false, false, kWall, 5);
        assertEquals(State.ESPERA, logic.getState());
        // Soltar recoger borra "lleno"
        step(false, false, false, kWall, 0);
        step(true, false, false, kWall, 5);
        assertEquals(State.RECOGER, logic.getState());
    }

    @Test
    public void disconnectedSensorNeverReportsPieceEvenIfDistanceSaysPiece() {
        tofConnected = false;
        steps(20, false, false, false, kPiece, 0);
        assertFalse(logic.hasPiece());
        assertTrue(logic.isSensorInvalid());
    }

    // ---------- Validacion de ShooterConstants con valores de prueba ----------

    // Valores de PRUEBA coherentes (no son los del robot): todo calibrado y sin errores
    private static ShooterConstants.Values good() {
        return values(5, 6, 7, 1500, 3500, -1500, 100, 60, 30, 0.5, 30, 450, 200, 3,
            0.1, 0.0, 0.0, 0.2, 0.12, true);
    }

    private static ShooterConstants.Values values(int leader, int follower, int tof,
            double intake, double shoot, double eject, double tolerance,
            double stator, double jamA, double jamS, double tofMin, double tofMax,
            double piece, int debounce, double kP, double kI, double kD, double kS, double kV,
            boolean inversionOk) {
        return new ShooterConstants.Values(leader, follower, tof, intake, shoot, eject, tolerance,
            stator, jamA, jamS, tofMin, tofMax, piece, debounce, kP, kI, kD, kS, kV, inversionOk);
    }

    private static void assertBlocks(ShooterConstants.Values v, String fragment) {
        List<String> errors = ShooterConstants.blockingErrors(v);
        assertTrue(errors.stream().anyMatch(e -> e.contains(fragment)),
            "se esperaba un error con '" + fragment + "', hubo: " + errors);
    }

    @Test
    public void coherentCalibratedValuesHaveNoErrorsAndNothingPending() {
        assertEquals(List.of(), ShooterConstants.blockingErrors(good()));
        assertEquals(List.of(), ShooterConstants.uncalibrated(good()));
    }

    @Test
    public void negativeIntakeOrShootRpmBlocks() {
        assertBlocks(values(5, 6, 7, -1, 3500, -1500, 100, 60, 30, 0.5, 30, 450, 200, 3,
            0.1, 0, 0, 0.2, 0.12, true), "kIntakeRpm");
        assertBlocks(values(5, 6, 7, -3000, -1, -1500, 100, 60, 30, 0.5, 30, 450, 200, 3,
            0.1, 0, 0, 0.2, 0.12, true), "kShootRpm");
    }

    @Test
    public void positiveEjectRpmBlocks() {
        assertBlocks(values(5, 6, 7, 1500, 3500, 1, 100, 60, 30, 0.5, 30, 450, 200, 3,
            0.1, 0, 0, 0.2, 0.12, true), "kEjectRpm");
    }

    @Test
    public void shootSlowerThanIntakeBlocks() {
        assertBlocks(values(5, 6, 7, 3000, 1000, -1500, 100, 60, 30, 0.5, 30, 450, 200, 3,
            0.1, 0, 0, 0.2, 0.12, true), "menor que kIntakeRpm");
    }

    @Test
    public void negativeCurrentJamTimeAndToleranceBlock() {
        assertBlocks(values(5, 6, 7, 1500, 3500, -1500, -1, 60, 30, 0.5, 30, 450, 200, 3,
            0.1, 0, 0, 0.2, 0.12, true), "kVelocityToleranceRpm");
        assertBlocks(values(5, 6, 7, 1500, 3500, -1500, 100, -60, 30, 0.5, 30, 450, 200, 3,
            0.1, 0, 0, 0.2, 0.12, true), "kStatorCurrentLimitAmps");
        assertBlocks(values(5, 6, 7, 1500, 3500, -1500, 100, 60, -30, 0.5, 30, 450, 200, 3,
            0.1, 0, 0, 0.2, 0.12, true), "kJamCurrentAmps");
        assertBlocks(values(5, 6, 7, 1500, 3500, -1500, 100, 60, 30, -0.5, 30, 450, 200, 3,
            0.1, 0, 0, 0.2, 0.12, true), "kJamTimeSeconds");
    }

    @Test
    public void negativeGainsBlockButZeroKiAndKdAreFine() {
        assertBlocks(values(5, 6, 7, 1500, 3500, -1500, 100, 60, 30, 0.5, 30, 450, 200, 3,
            -0.1, 0, 0, 0.2, 0.12, true), "ganancias");
        assertBlocks(values(5, 6, 7, 1500, 3500, -1500, 100, 60, 30, 0.5, 30, 450, 200, 3,
            0.1, -0.1, 0, 0.2, 0.12, true), "ganancias");
        assertBlocks(values(5, 6, 7, 1500, 3500, -1500, 100, 60, 30, 0.5, 30, 450, 200, 3,
            0.1, 0, -0.1, 0.2, 0.12, true), "ganancias");
        assertBlocks(values(5, 6, 7, 1500, 3500, -1500, 100, 60, 30, 0.5, 30, 450, 200, 3,
            0.1, 0, 0, -0.2, 0.12, true), "ganancias");
        assertBlocks(values(5, 6, 7, 1500, 3500, -1500, 100, 60, 30, 0.5, 30, 450, 200, 3,
            0.1, 0, 0, 0.2, -0.12, true), "ganancias");
        assertEquals(List.of(), ShooterConstants.blockingErrors(good()));
    }

    @Test
    public void jamCurrentNotBelowStatorLimitBlocks() {
        assertBlocks(values(5, 6, 7, 1500, 3500, -1500, 100, 60, 60, 0.5, 30, 450, 200, 3,
            0.1, 0, 0, 0.2, 0.12, true), "MENOR que kStatorCurrentLimitAmps");
        assertBlocks(values(5, 6, 7, 1500, 3500, -1500, 100, 60, 70, 0.5, 30, 450, 200, 3,
            0.1, 0, 0, 0.2, 0.12, true), "MENOR que kStatorCurrentLimitAmps");
    }

    @Test
    public void badTofBandBlocks() {
        // minimo negativo
        assertBlocks(values(5, 6, 7, 1500, 3500, -1500, 100, 60, 30, 0.5, -5, 450, 200, 3,
            0.1, 0, 0, 0.2, 0.12, true), "kTofMinValidMm");
        // minimo igual al maximo
        assertBlocks(values(5, 6, 7, 1500, 3500, -1500, 100, 60, 30, 0.5, 450, 450, 200, 3,
            0.1, 0, 0, 0.2, 0.12, true), "Banda TOF");
        // minimo mayor que el maximo
        assertBlocks(values(5, 6, 7, 1500, 3500, -1500, 100, 60, 30, 0.5, 500, 450, 200, 3,
            0.1, 0, 0, 0.2, 0.12, true), "Banda TOF");
    }

    @Test
    public void calibratingOnlyOneTofLimitIsNotABlockingError() {
        // solo el minimo (max sin calibrar)
        assertEquals(List.of(), ShooterConstants.blockingErrors(
            values(5, 6, 7, 1500, 3500, -1500, 100, 60, 30, 0.5, 30, 0, 0, 3,
                0.1, 0, 0, 0.2, 0.12, true)));
        // solo el maximo (min sin calibrar)
        assertEquals(List.of(), ShooterConstants.blockingErrors(
            values(5, 6, 7, 1500, 3500, -1500, 100, 60, 30, 0.5, 0, 450, 0, 3,
                0.1, 0, 0, 0.2, 0.12, true)));
    }

    @Test
    public void calibratingOnlyIntakeRpmIsNotABlockingError() {
        assertEquals(List.of(), ShooterConstants.blockingErrors(
            values(5, 6, 7, 1500, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3,
                0, 0, 0, 0, 0, false)));
    }

    @Test
    public void pieceThresholdOutsideBandBlocks() {
        // igual al minimo (el intervalo es abierto en el minimo)
        assertBlocks(values(5, 6, 7, 1500, 3500, -1500, 100, 60, 30, 0.5, 30, 450, 30, 3,
            0.1, 0, 0, 0.2, 0.12, true), "kPieceThresholdMm");
        // mayor que el maximo
        assertBlocks(values(5, 6, 7, 1500, 3500, -1500, 100, 60, 30, 0.5, 30, 450, 451, 3,
            0.1, 0, 0, 0.2, 0.12, true), "kPieceThresholdMm");
        // igual al maximo (la lectura de pared) tambien bloquea: toda lectura contaria como pieza
        assertBlocks(values(5, 6, 7, 1500, 3500, -1500, 100, 60, 30, 0.5, 30, 450, 450, 3,
            0.1, 0, 0, 0.2, 0.12, true), "kPieceThresholdMm");
        // justo debajo del maximo si es valido
        assertEquals(List.of(), ShooterConstants.blockingErrors(
            values(5, 6, 7, 1500, 3500, -1500, 100, 60, 30, 0.5, 30, 450, 449, 3,
                0.1, 0, 0, 0.2, 0.12, true)));
    }

    @Test
    public void debounceBelowOneBlocks() {
        assertBlocks(values(5, 6, 7, 1500, 3500, -1500, 100, 60, 30, 0.5, 30, 450, 200, 0,
            0.1, 0, 0, 0.2, 0.12, true), "kPieceDebounceCycles");
    }

    @Test
    public void duplicateOrOutOfRangeIdsBlock() {
        assertBlocks(values(5, 5, 7, 1500, 3500, -1500, 100, 60, 30, 0.5, 30, 450, 200, 3,
            0.1, 0, 0, 0.2, 0.12, true), "duplicado");
        assertBlocks(values(5, 6, 63, 1500, 3500, -1500, 100, 60, 30, 0.5, 30, 450, 200, 3,
            0.1, 0, 0, 0.2, 0.12, true), "fuera de rango");
        assertBlocks(values(-1, 6, 7, 1500, 3500, -1500, 100, 60, 30, 0.5, 30, 450, 200, 3,
            0.1, 0, 0, 0.2, 0.12, true), "fuera de rango");
    }

    @Test
    public void uncalibratedAllZerosListsExactlyTheExpectedNames() {
        assertEquals(List.of("kIntakeRpm", "kShootRpm", "kEjectRpm", "kVelocityToleranceRpm",
                "kStatorCurrentLimitAmps", "kJamCurrentAmps", "kJamTimeSeconds", "kTofMinValidMm",
                "kTofMaxValidMm", "kPieceThresholdMm", "kPVoltsPerRps", "kSVolts", "kVVoltsPerRps",
                "kInversionValidated (inversion sin validar en bloques)"),
            ShooterConstants.uncalibrated(values(5, 6, 7, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 3, 0, 0, 0, 0, 0, false)));
    }

    @Test
    public void useRealIoOnlyOutsideSimulationAndWhenReady() {
        assertFalse(ShooterConstants.useRealIo(true, true));
        assertFalse(ShooterConstants.useRealIo(true, false));
        assertFalse(ShooterConstants.useRealIo(false, false));
        assertTrue(ShooterConstants.useRealIo(false, true));
    }

    @Test
    public void staleEjectOnlyActivatesStaleOrderIgnored() {
        enabled = false;
        steps(3, false, false, true, kWall, 0);
        enabled = true;
        step(false, false, true, kWall, 0);
        assertEquals(State.ESPERA, logic.getState());
        assertTrue(logic.isStaleOrderIgnored());
        step(false, false, false, kWall, 0);
        assertFalse(logic.isStaleOrderIgnored());
    }

    @Test
    public void uncalibratedListsZerosTofMinAndUnvalidatedInversion() {
        List<String> pending = ShooterConstants.uncalibrated(values(5, 6, 7, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 3, 0, 0, 0, 0, 0, false));
        assertTrue(pending.contains("kTofMinValidMm"));
        assertTrue(pending.contains("kIntakeRpm"));
        assertTrue(pending.contains("kPVoltsPerRps"));
        assertTrue(pending.stream().anyMatch(p -> p.contains("kInversionValidated")));
        // kI y kD en 0 no se listan
        assertFalse(pending.contains("kIVoltsPerRotation"));
        assertFalse(pending.contains("kDVoltsPerRpsPerSec"));
    }
}
