// Description: Pruebas de sanidad de las constantes.
// Authors: LamBot 3478

package frc.robot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import frc.robot.Constants.DriveConstants;

public class ConstantsTest {

    @Test
    public void canIdsAreUniqueAndInRange() {
        int[] ids = {DriveConstants.kLeftLeaderId, DriveConstants.kLeftFollowerId,
            DriveConstants.kRightLeaderId, DriveConstants.kRightFollowerId};
        Set<Integer> seen = new HashSet<>();
        for (int id : ids) {
            assertTrue(id >= 0 && id <= 62, "ID fuera de rango: " + id);
            seen.add(id);
        }
        assertEquals(ids.length, seen.size(), "hay IDs duplicados");
    }
}
