// Description: Interfaz de la capa IO del chasis (separa la logica del hardware).
// Authors: LamBot 3478

package frc.robot.subsystems.drive;

public interface DriveIO {

    // Salidas de -1 a 1 por lado, ya con la convencion "adelante = positivo"
    void setOutputs(double left, double right);

    // Pone ambos lados en cero
    void stop();
}
