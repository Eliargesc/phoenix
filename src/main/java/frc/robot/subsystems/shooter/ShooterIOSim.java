// Description: IO falsa del shooter para simulacion y tests: sin CAN, lecturas fijadas por la prueba.
// Authors: LamBot 3478

package frc.robot.subsystems.shooter;

public class ShooterIOSim implements ShooterIO {

    private double lastCommandedRpm = 0.0;
    private int stopCount = 0;
    private boolean stopped = true;

    // Lecturas fijadas por la prueba (si no, se usa el modelo ideal)
    private Double simVelocityRpm = null;
    private double simStatorCurrentAmps = 0.0;
    private double simTofDistanceMm = 0.0;
    private boolean simTofConnected = false;

    // Modelo ideal: la velocidad medida es la ultima pedida, salvo que la prueba la fije
    @Override
    public void updateInputs(ShooterIOInputs inputs) {
        inputs.velocityRpm = (simVelocityRpm != null) ? simVelocityRpm : lastCommandedRpm;
        inputs.statorCurrentAmps = simStatorCurrentAmps;
        inputs.tofDistanceMm = simTofDistanceMm;
        inputs.tofConnected = simTofConnected;
    }

    @Override
    public void setVelocityRpm(double rpm) {
        lastCommandedRpm = rpm;
        stopped = false;
    }

    // Salida neutral: el mecanismo queda libre, asi que el modelo ideal pide 0 RPM
    @Override
    public void stop() {
        lastCommandedRpm = 0.0;
        stopped = true;
        stopCount++;
    }

    // Ultima RPM pedida (0 despues de stop)
    public double getLastCommandedRpm() {
        return lastCommandedRpm;
    }

    // True si lo ultimo que se llamo fue stop() (o nunca se pidio velocidad)
    public boolean isStopped() {
        return stopped;
    }

    // Cuantas veces se llamo a stop()
    public int getStopCount() {
        return stopCount;
    }

    // Fija la velocidad leida; la prueba la manda en lugar del modelo ideal
    public void setSimVelocityRpm(double rpm) {
        simVelocityRpm = rpm;
    }

    // Vuelve al modelo ideal (velocidad medida = ultima pedida)
    public void clearSimVelocity() {
        simVelocityRpm = null;
    }

    public void setSimStatorCurrentAmps(double amps) {
        simStatorCurrentAmps = amps;
    }

    public void setSimTofDistanceMm(double mm) {
        simTofDistanceMm = mm;
    }

    public void setSimTofConnected(boolean connected) {
        simTofConnected = connected;
    }
}
