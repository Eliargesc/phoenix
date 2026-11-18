// Description: Interfaz de la capa IO del shooter (separa la logica del hardware).
// Authors: LamBot 3478

package frc.robot.subsystems.shooter;

public interface ShooterIO {

    // Lecturas del hardware, se llenan cada ciclo
    class ShooterIOInputs {
        public double velocityRpm = 0.0;        // del motor lider, positivo = pieza sube
        public double statorCurrentAmps = 0.0;  // la mayor de los dos motores
        public double tofDistanceMm = 0.0;
        public boolean tofConnected = false;
    }

    // Lee el hardware y llena inputs
    void updateInputs(ShooterIOInputs inputs);

    // Velocidad objetivo del mecanismo. Positivo = la pieza sube
    void setVelocityRpm(double rpm);

    // Salida neutral (0)
    void stop();
}
