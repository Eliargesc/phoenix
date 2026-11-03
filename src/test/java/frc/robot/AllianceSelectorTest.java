// Description: Pruebas de la alianza efectiva (DriverStation y fallback).
// Authors: LamBot 3478

package frc.robot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.wpi.first.hal.AllianceStationID;
import edu.wpi.first.hal.HAL;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.simulation.DriverStationSim;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.util.AllianceSelector;

public class AllianceSelectorTest {

    @BeforeEach
    public void setUp() {
        assertTrue(HAL.initialize(500, 0), "no se pudo inicializar el HAL");
    }

    @AfterEach
    public void tearDown() {
        HAL.shutdown();
    }

    @Test
    public void driverStationRedWins() {
        AllianceSelector selector = new AllianceSelector();
        DriverStationSim.setAllianceStationId(AllianceStationID.Red1);
        DriverStationSim.notifyNewData();
        assertEquals(Alliance.Red, selector.getEffectiveAlliance());
        assertTrue(selector.isFromDriverStation());
    }

    @Test
    public void unknownStationUsesFallback() {
        AllianceSelector selector = new AllianceSelector();
        DriverStationSim.setAllianceStationId(AllianceStationID.Unknown);
        DriverStationSim.notifyNewData();
        assertEquals(Alliance.Blue, selector.getEffectiveAlliance());
        assertFalse(selector.isFromDriverStation());
        selector.periodic();
        assertEquals("Prueba (fallback)", SmartDashboard.getString("Alianza/Fuente", ""));
    }
}
