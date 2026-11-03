// Description: IO real del chasis con Talon SRX / Victor SPX (Phoenix 5).
// Authors: LamBot 3478
// Notes: La inversion se hace SOLO aqui, por lado. No lee sensores.

package frc.robot.subsystems.drive;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.can.BaseMotorController;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;

import frc.robot.Constants.DriveConstants;
import frc.robot.Constants.DriveConstants.ControllerType;

public class DriveIOCTRE implements DriveIO {

    private final BaseMotorController leftLeader;
    private final BaseMotorController leftFollower;
    private final BaseMotorController rightLeader;
    private final BaseMotorController rightFollower;

    // Crea y configura los cuatro controladores
    public DriveIOCTRE() {
        leftLeader = create(DriveConstants.kLeftLeaderType, DriveConstants.kLeftLeaderId,
                DriveConstants.kLeftInverted);
        leftFollower = create(DriveConstants.kLeftFollowerType, DriveConstants.kLeftFollowerId,
                DriveConstants.kLeftInverted);
        rightLeader = create(DriveConstants.kRightLeaderType, DriveConstants.kRightLeaderId,
                DriveConstants.kRightInverted);
        rightFollower = create(DriveConstants.kRightFollowerType, DriveConstants.kRightFollowerId,
                DriveConstants.kRightInverted);
    }

    // Fabrica un controlador del tipo pedido y le aplica la configuracion comun
    private static BaseMotorController create(ControllerType type, int id, boolean inverted) {
        BaseMotorController controller = switch (type) {
            case TALON_SRX -> new WPI_TalonSRX(id);
            case VICTOR_SPX -> new WPI_VictorSPX(id);
        };
        controller.configFactoryDefault();
        controller.setNeutralMode(DriveConstants.kBrakeMode ? NeutralMode.Brake : NeutralMode.Coast);
        controller.setInverted(inverted);
        return controller;
    }

    // No usamos follow(): mandamos la misma orden a los dos motores de cada lado.
    // Con mezcla de SRX y SPX es mas facil de depurar y no depende de la configuracion CAN.
    @Override
    public void setOutputs(double left, double right) {
        leftLeader.set(ControlMode.PercentOutput, left);
        leftFollower.set(ControlMode.PercentOutput, left);
        rightLeader.set(ControlMode.PercentOutput, right);
        rightFollower.set(ControlMode.PercentOutput, right);
    }

    // Ambos lados a cero
    @Override
    public void stop() {
        setOutputs(0.0, 0.0);
    }
}
