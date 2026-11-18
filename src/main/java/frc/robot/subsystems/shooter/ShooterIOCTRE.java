// Description: IO real del shooter: dos Kraken X60 (TalonFX) y un sensor de distancia CANrange.
// Authors: LamBot 3478

package frc.robot.subsystems.shooter;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.StatusCode;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.Follower;
import com.ctre.phoenix6.controls.NeutralOut;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.hardware.CANrange;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.MeasurementHealthValue;
import com.ctre.phoenix6.signals.MotorAlignmentValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import edu.wpi.first.units.measure.AngularVelocity;
import edu.wpi.first.units.measure.Current;
import edu.wpi.first.units.measure.Distance;
import edu.wpi.first.wpilibj.DriverStation;
import frc.robot.Constants.ShooterConstants;

public class ShooterIOCTRE implements ShooterIO {

    // Phoenix trabaja en rotaciones por segundo (rps), nosotros en RPM
    private static final double kSecondsPerMinute = 60.0;
    // Metros a milimetros
    private static final double kMmPerMeter = 1000.0;

    // Los constructores con String estan obsoletos en Phoenix 26: se usa un objeto CANBus
    private static final CANBus kBus = new CANBus(ShooterConstants.kCanBus);

    private final TalonFX leader = new TalonFX(ShooterConstants.kLeaderId, kBus);
    private final TalonFX follower = new TalonFX(ShooterConstants.kFollowerId, kBus);
    // TODO [POR CONFIRMAR CON EL EQUIPO]: asumimos que el sensor de distancia (CAN 7) es un CTRE
    // CANrange. Si es de otro fabricante, solo hay que cambiar esta clase (ShooterIOCTRE).
    private final CANrange tof = new CANrange(ShooterConstants.kTofId, kBus);

    // Se crean una sola vez y se reutilizan: asi no se genera basura cada ciclo
    private final VelocityVoltage velocityRequest = new VelocityVoltage(0.0);
    private final NeutralOut neutralRequest = new NeutralOut();

    // Senales de lectura (Phoenix las actualiza con refreshAll)
    private final StatusSignal<AngularVelocity> leaderVelocity = leader.getVelocity();
    private final StatusSignal<Current> leaderCurrent = leader.getStatorCurrent();
    private final StatusSignal<Current> followerCurrent = follower.getStatorCurrent();
    private final StatusSignal<Distance> tofDistance = tof.getDistance();
    // Salud de la medicion del CANrange (Good, Limited o Bad)
    private final StatusSignal<MeasurementHealthValue> tofHealth = tof.getMeasurementHealth();
    // Arreglo creado una sola vez: refreshAll lo recibe cada ciclo sin crear objetos nuevos
    private final BaseStatusSignal[] allSignals =
            {leaderVelocity, leaderCurrent, followerCurrent, tofDistance, tofHealth};

    // TODO: el CANrange usa la configuracion guardada en el sensor (se ajusta con Phoenix Tuner).
    // Desde el codigo NO se le aplica ninguna configuracion (rango, FOV, etc.).
    //
    // ADVERTENCIA: no llamar optimizeBusUtilization() en el lider sin mantener sus senales de
    // salida; si no, el seguidor deja de seguir al lider.
    public ShooterIOCTRE() {
        if (ShooterConstants.kStatorCurrentLimitAmps <= 0.0) {
            DriverStation.reportError(
                    "Shooter: limite de corriente sin calibrar: el shooter no se movera", false);
        }
        // La configuracion se aplica una sola vez, aqui
        TalonFXConfiguration config = new TalonFXConfiguration();
        // Freno o coast segun kBrakeMode (decidido: coast, el motor gira libre al soltar)
        config.MotorOutput.NeutralMode = ShooterConstants.kBrakeMode
                ? NeutralModeValue.Brake
                : NeutralModeValue.Coast;
        config.MotorOutput.Inverted = ShooterConstants.kLeaderInverted
                ? InvertedValue.Clockwise_Positive
                : InvertedValue.CounterClockwise_Positive;
        // Limite de corriente del estator: protege el motor y el mecanismo
        config.CurrentLimits.StatorCurrentLimitEnable = true;
        config.CurrentLimits.StatorCurrentLimit = ShooterConstants.kStatorCurrentLimitAmps;
        // Ganancias del control de velocidad (slot 0)
        // Las ganancias ya vienen en unidades de Phoenix (rps), solo se copian
        config.Slot0.kP = ShooterConstants.kPVoltsPerRps;
        config.Slot0.kI = ShooterConstants.kIVoltsPerRotation;
        config.Slot0.kD = ShooterConstants.kDVoltsPerRpsPerSec;
        config.Slot0.kS = ShooterConstants.kSVolts;
        config.Slot0.kV = ShooterConstants.kVVoltsPerRps;

        // Los dos motores llevan la misma configuracion; la inversion real del
        // seguidor la decide el control Follower (abajo), no esta configuracion
        applyConfig(leader, config, "lider");
        applyConfig(follower, config, "seguidor");

        // El seguidor copia la salida del lider; Opposed si gira contra el lider
        follower.setControl(new Follower(
                ShooterConstants.kLeaderId,
                ShooterConstants.kFollowerOpposesLeader
                        ? MotorAlignmentValue.Opposed
                        : MotorAlignmentValue.Aligned));
    }

    private void applyConfig(TalonFX motor, TalonFXConfiguration config, String name) {
        StatusCode status = motor.getConfigurator().apply(config);
        if (!status.isOK()) {
            DriverStation.reportError(
                    "Shooter: no se pudo configurar el motor " + name + ": " + status.getDescription(), false);
        }
    }

    // Lee todo el hardware una vez por ciclo
    @Override
    public void updateInputs(ShooterIOInputs inputs) {
        BaseStatusSignal.refreshAll(allSignals);

        inputs.velocityRpm = leaderVelocity.getValueAsDouble() * kSecondsPerMinute;
        inputs.statorCurrentAmps = Math.max(
                Math.abs(leaderCurrent.getValueAsDouble()),
                Math.abs(followerCurrent.getValueAsDouble()));
        inputs.tofDistanceMm = tofDistance.getValueAsDouble() * kMmPerMeter;
        // Conectado = el dispositivo responde, la ultima lectura llego bien Y el sensor no
        // marca la medicion como mala (Bad). Con Bad el numero de distancia no es confiable
        // aunque el CAN funcione, y la logica debe tratarlo como sensor invalido.
        inputs.tofConnected = tof.isConnected()
                && tofDistance.getStatus().isOK()
                && tofHealth.getValue() != MeasurementHealthValue.Bad;
    }

    @Override
    public void setVelocityRpm(double rpm) {
        // Sin reduccion: RPM del motor = RPM del mecanismo
        leader.setControl(velocityRequest.withVelocity(rpm / kSecondsPerMinute));
    }

    @Override
    public void stop() {
        // Solo se manda neutral al lider. El seguidor sigue en modo Follower y copia la salida
        // del lider, asi que tambien queda sin salida (Coast: gira libre). No se le manda nada
        // al seguidor porque eso lo sacaria del modo Follower.
        leader.setControl(neutralRequest);
    }
}
