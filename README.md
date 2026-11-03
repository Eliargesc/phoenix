# LamBot 3478 · Tank Challenge Template

Template de robot FRC con chasis tank. Está hecho para que alumnos nuevos cambien valores, prueben y entiendan qué pasó.

> **Estado:** el código compila y tiene pruebas automáticas. **Nada se ha probado en el robot físico.** Hasta que lo pruebes con supervisión, trata todo como no verificado.

## 1. Qué es el reto y qué está resuelto

**Resuelto:** el chasis. Manejo arcade, zona muerta, rampas, modo lento, brake y dos autónomos.

**Por desarrollar:** `Subsistema1`. Está vacío a propósito. Lo diseñan ustedes cuando se publique el reto.

El robot tiene 4 motores CIM, 2 por lado, con ToughBox Mini. No tiene encoders ni giroscopio.

## 2. Requisitos de instalación

- WPILib 2026 (incluye Java 17 y Gradle).
- Dependencias de CTRE ya incluidas en `vendordeps/`: Phoenix 5 y Phoenix 6. Talon SRX y Victor SPX usan Phoenix 5. Phoenix 5 pide tener Phoenix 6 instalado también.
- Para ver datos: SmartDashboard o Shuffleboard (vienen con WPILib).
- Para conducir el robot: Driver Station. **Solo corre en Windows.**
- Phoenix Tuner, para ver los controladores y sus CAN IDs.

## 3. Cambia esto y prueba

Todo vive en `src/main/java/frc/robot/Constants.java`. Cambia un valor, guarda y prueba (simulación primero).

| Qué quieres cambiar | Clase anidada | Constante | Default | Etiqueta |
|---|---|---|---|---|
| Tipo de controlador izquierdo principal | `DriveConstants` | `kLeftLeaderType` | `TALON_SRX` | PROVISIONAL |
| Tipo de controlador izquierdo secundario | `DriveConstants` | `kLeftFollowerType` | `TALON_SRX` | PROVISIONAL |
| Tipo de controlador derecho principal | `DriveConstants` | `kRightLeaderType` | `TALON_SRX` | PROVISIONAL |
| Tipo de controlador derecho secundario | `DriveConstants` | `kRightFollowerType` | `TALON_SRX` | PROVISIONAL |
| CAN ID izquierdo principal | `DriveConstants` | `kLeftLeaderId` | `1` | PROVISIONAL |
| CAN ID izquierdo secundario | `DriveConstants` | `kLeftFollowerId` | `2` | PROVISIONAL |
| CAN ID derecho principal | `DriveConstants` | `kRightLeaderId` | `3` | PROVISIONAL |
| CAN ID derecho secundario | `DriveConstants` | `kRightFollowerId` | `4` | PROVISIONAL |
| Confirmar que los IDs ya se verificaron | `DriveConstants` | `kCanIdsConfirmed` | `false` | PROVISIONAL |
| Invertir el lado izquierdo | `DriveConstants` | `kLeftInverted` | `false` | PROVISIONAL |
| Invertir el lado derecho | `DriveConstants` | `kRightInverted` | `true` | PROVISIONAL |
| Brake (frenado eléctrico) | `DriveConstants` | `kBrakeMode` | `true` | DECIDIDO |
| Zona muerta de los sticks | `DriveConstants` | `kDeadband` | `0.08` | SUGERIDO |
| Tope de salida en teleop (0 a 1) | `DriveConstants` | `kMaxOutput` | `1.0` | SUGERIDO |
| Fuerza del modo lento | `DriveConstants` | `kSlowModeMultiplier` | `0.6` | SUGERIDO |
| Rampa de avance (por segundo) | `DriveConstants` | `kForwardSlewRatePerSec` | `2.0` | SUGERIDO |
| Rampa de giro (por segundo) | `DriveConstants` | `kTurnSlewRatePerSec` | `3.0` | SUGERIDO |
| Sticks más finos a baja velocidad | `DriveConstants` | `kSquareInputs` | `true` | SUGERIDO |
| Puerto USB del piloto | `OperatorConstants` | `kDriverControllerPort` | `0` | DECIDIDO (ajustable) |
| Puerto USB del operador | `OperatorConstants` | `kOperatorControllerPort` | `1` | DECIDIDO (ajustable) |
| Botón del modo lento | `OperatorConstants` | `kSlowModeButton` | `kLeftBumper` (LB) | SUGERIDO |
| Invertir eje de avance | `OperatorConstants` | `kInvertForwardAxis` | `true` | SUGERIDO |
| Invertir eje de giro | `OperatorConstants` | `kInvertTurnAxis` | `true` | SUGERIDO |
| Tiempo del auto de avance | `AutoConstants` | `kForwardSeconds` | `3.0` | DECIDIDO |
| Potencia del auto de avance | `AutoConstants` | `kForwardPercent` | `0.40` | DECIDIDO |

Qué significan las etiquetas:

- **DECIDIDO:** lo fijó el mentor.
- **PROVISIONAL:** es un valor de arranque. Confírmalo en el robot.
- **SUGERIDO:** es un valor inicial razonable. Ajústalo.

Notas rápidas:

- La inversión se hace **por lado**. Los dos motores de un lado reciben la misma.
- Con `kSquareInputs = true`, el modo lento a stick completo da 0.6² ≈ **36 %** de salida, no 60 %.
- Si hay IDs repetidos o fuera de 0..62, el chasis real **se bloquea**: el código usa un chasis falso, reporta un error en el Driver Station y `Config/OK` queda en `false`. Corrige los IDs en `Constants.java`.
- Si `kCanIdsConfirmed` es `false`, el código solo avisa en el Driver Station. Ponlo en `true` solo después de verificar los IDs en Phoenix Tuner.
- Los defaults de IDs, tipos e inversiones son provisionales. No son datos medidos del robot.

## 4. Controles de conducción

| Control | Qué hace |
|---|---|
| Piloto, stick izquierdo, eje Y (`leftY`) | Avanzar y retroceder |
| Piloto, stick derecho, eje X (`rightX`) | Girar |
| Piloto, bumper izquierdo (LB, `kSlowModeButton`) | Modo lento mientras lo mantienes |
| Operador | Libre. Reservado para el mecanismo del reto |

Si algo va o gira al revés, diagnostica así:

- **Al avanzar, el robot va hacia atrás o gira al revés *también en el auto*:** el problema es el motor. Ajusta `kLeftInverted` / `kRightInverted` y pruébalo en bloques, incluyendo "Avanzar 3 segundos".
- **Solo falla con el stick (el auto va bien):** el stick lee al revés. Ahí sí toca `kInvertForwardAxis` / `kInvertTurnAxis`. El auto no pasa por esos ejes.
- **Los dos motores de un lado se pelean (uno empuja contra el otro):** **deshabilita YA** y corrige el cableado de ese motor. Ninguna constante lo arregla.

## 5. Selector de autos

Elige el auto en el dashboard, en `Autonomo/Selector`.

| Opción | Qué hace |
|---|---|
| **No hacer nada** (default) | El robot se queda quieto |
| **Avanzar 3 segundos** | Avanza al 40 % de potencia durante 3 s y se detiene |

**Tiempo no es distancia.** Con 3 s al 40 % el robot recorre más o menos según batería, piso y peso. No es una distancia fija. Si no hay selección válida, el robot hace "No hacer nada".

## 6. Alianza

- Si el Driver Station o el FMS informan la alianza, el código usa esa. Siempre.
- Si no hay dato (por ejemplo, en simulación), puedes elegir una alianza de prueba en `Alianza/Fallback (solo pruebas)`. Nunca cambia la alianza oficial.
- Los autos actuales no usan la alianza. Es igual para rojo y azul. Está lista para cuando el reto la necesite.

### Qué verás en el dashboard

| Clave | Qué muestra |
|---|---|
| `Autonomo/Selector` | Selector de autónomo |
| `Alianza/Efectiva` | Alianza que usa el código (Azul o Rojo) |
| `Alianza/Fuente` | `DriverStation` o `Prueba (fallback)` |
| `Alianza/Fallback (solo pruebas)` | Selector de alianza de prueba |
| `Drive/DemandaIzquierda` | Salida pedida al lado izquierdo (-1 a 1) |
| `Drive/DemandaDerecha` | Salida pedida al lado derecho (-1 a 1) |
| `Drive/ModoLento` | `true` si el modo lento está activo |
| `Config/OK` | `false` si hay IDs repetidos o fuera de rango (chasis bloqueado) o sin confirmar (solo aviso) |

La compatibilidad con **Elastic** está **pendiente de comprobación manual**. Usa SmartDashboard o Shuffleboard mientras tanto.

## 7. Simulación, build y tests

Corre estos comandos en la raíz del proyecto. En Windows usa `gradlew.bat` en vez de `./gradlew`.

| Comando | Para qué |
|---|---|
| `./gradlew build` | Compilar y correr las pruebas |
| `./gradlew test` | Solo las pruebas automáticas |
| `./gradlew simulateJava` | Simulación de escritorio |
| `./gradlew deploy` | Subir el código al roboRIO (con el robot conectado) |

**La simulación y los tests no validan el robot físico.** No prueban cableado, inversiones ni IDs reales.

Checklist en simulación:

- [ ] Abre `simulateJava` y activa el Driver Station simulado.
- [ ] Revisa que el piloto esté en el puerto 0 y el operador en el 1. El operador no mueve nada.
- [ ] Ve `Drive/DemandaIzquierda` y `Drive/DemandaDerecha` mientras mueves el stick.
- [ ] Elige "No hacer nada" en autónomo: las demandas se quedan en 0.
- [ ] Elige "Avanzar 3 segundos": ves 0.40 en ambos lados durante 3 s y luego 0.
- [ ] Revisa `Alianza/Efectiva` y `Alianza/Fuente`.

## 8. Checklist de primera puesta en marcha

Hazla con el robot **en bloques**, con las ruedas sin tocar el piso.

**Antes de encender**

- [ ] Confirma el modelo real de cada controlador: Talon SRX o Victor SPX por CAN. Otros modelos (Talon FX, Victor SP, Victor 888) necesitan otro código.
- [ ] Confirma los 4 CAN IDs en Phoenix Tuner. Ninguno repetido.
- [ ] Confirma qué motores van en el lado izquierdo y cuáles en el derecho.
- [ ] Revisa batería, breaker, conectores y cables.
- [ ] Conecta los controles Xbox en los puertos definidos.
- [ ] Despeja el área. Una persona tiene la tarea de deshabilitar el robot.

**Pruebas**

- [ ] Cambia `kCanIdsConfirmed` a `true` cuando los IDs estén verificados.
- [ ] Prueba cada lado a baja potencia. Corrige `kLeftInverted` / `kRightInverted` si hace falta, probando también el auto.
- [ ] Confirma que los dos motores de un lado empujan juntos, no uno contra otro. Si pelean, deshabilita YA y corrige el cableado de ese motor.
- [ ] Prueba avance y giro con el stick.
- [ ] Prueba brake, zona muerta, rampa y modo lento.
- [ ] Prueba el auto "No hacer nada".
- [ ] Prueba "Avanzar 3 segundos" en un espacio despejado y con supervisión. **Nunca la primera vez en el piso cerca de personas.**
- [ ] Confirma que el robot se detiene al terminar, al interrumpir y al deshabilitar.

El brake no es un freno mecánico. No garantiza una parada inmediata.

## 9. Dónde implementar `Subsistema1`

Archivo: `src/main/java/frc/robot/subsystems/Subsistema1.java`. Tiene una lista de TODOs numerados. En orden:

1. Define el hardware del mecanismo.
2. Crea `Constants.Subsistema1Constants` con los IDs y valores.
3. Crea una capa IO, igual que `subsystems/drive/`.
4. Escribe métodos y comandos. Cada comando termina con `finallyDo(stop)`.
5. Pon los botones del operador en `RobotContainer.java`, en el bloque `// TODO (reto)`.
6. Prueba el mecanismo sin interferir con el chasis.

### Mapa del código

```
src/main/java/frc/robot/
  Constants.java            <- aquí cambias valores
  RobotContainer.java       <- controles, selector de autos, bloqueo por IDs inválidos
  Robot.java
  commands/Autos.java       <- los dos autónomos
  subsystems/Subsistema1.java
  subsystems/drive/         <- chasis (DriveSubsystem + IO real y simulado)
  util/AllianceSelector.java
src/test/java/frc/robot/    <- pruebas automáticas
```

## 10. Limitaciones conocidas

- Sin encoders ni giroscopio. No hay odometría, distancias en metros ni control de rumbo.
- Tiempo no es distancia. El auto de 3 s al 40 % cambia con batería, piso y peso.
- La reducción de la caja no se conoce. No hace falta para este modo.
- Los CAN IDs, tipos de controlador e inversiones son provisionales hasta confirmarlos en el robot.
- La simulación y los tests no sustituyen la prueba física supervisada.
- Compatibilidad con Elastic: pendiente de comprobación manual.
- Ninguna configuración de software sustituye las medidas de seguridad eléctrica y mecánica.
