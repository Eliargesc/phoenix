// Description: IO falsa para simulacion y tests: solo recuerda la ultima salida.
// Authors: LamBot 3478

package frc.robot.subsystems.drive;

public class DriveIOSim implements DriveIO {

    private double leftOutput = 0.0;
    private double rightOutput = 0.0;

    // Guarda la salida pedida
    @Override
    public void setOutputs(double left, double right) {
        leftOutput = left;
        rightOutput = right;
    }

    // Ambos lados a cero
    @Override
    public void stop() {
        setOutputs(0.0, 0.0);
    }

    // Ultima salida del lado izquierdo
    public double getLeftOutput() {
        return leftOutput;
    }

    // Ultima salida del lado derecho
    public double getRightOutput() {
        return rightOutput;
    }
}
