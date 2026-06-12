package interview;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParkingGarageServiceTest {

    private ParkingGarageService parkingGarageService;

    @BeforeEach
    void setUp() {
        parkingGarageService = new ParkingGarageService();
    }

    @Test
    void shouldAddParkingSpotsToAvailableInventory() {
        parkingGarageService.addParkingSpot("C1", SpotType.COMPACT);
        parkingGarageService.addParkingSpot("C2", SpotType.COMPACT);
        parkingGarageService.addParkingSpot("R1", SpotType.REGULAR);

        assertAll(
                () -> assertEquals(2, parkingGarageService.getAvailableSpots(SpotType.COMPACT)),
                () -> assertEquals(1, parkingGarageService.getAvailableSpots(SpotType.REGULAR)),
                () -> assertEquals(0, parkingGarageService.getAvailableSpots(SpotType.LARGE))
        );
    }

    @Test
    void shouldRejectDuplicateParkingSpotNumbers() {
        parkingGarageService.addParkingSpot("C1", SpotType.COMPACT);

        assertThrows(
                IllegalArgumentException.class,
                () -> parkingGarageService.addParkingSpot("C1", SpotType.REGULAR)
        );
    }

    @Test
    void shouldRejectInvalidParkingSpotDetails() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> parkingGarageService.addParkingSpot(null, SpotType.COMPACT)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> parkingGarageService.addParkingSpot("", SpotType.COMPACT)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> parkingGarageService.addParkingSpot("C1", null)
                )
        );
    }

    @Test
    void shouldParkVehicleInAvailableSpot() {
        parkingGarageService.addParkingSpot("C1", SpotType.COMPACT);

        String spotNumber = parkingGarageService.parkVehicle("ABC-123", SpotType.COMPACT);
        ParkingAssignment parkingAssignment = parkingGarageService.getParkedVehicle("ABC-123");

        assertAll(
                () -> assertEquals("C1", spotNumber),
                () -> assertEquals(0, parkingGarageService.getAvailableSpots(SpotType.COMPACT)),
                () -> assertEquals("ABC-123", parkingAssignment.licencePlate()),
                () -> assertEquals("C1", parkingAssignment.spotNumber()),
                () -> assertEquals(SpotType.COMPACT, parkingAssignment.vehicleSize()),
                () -> assertEquals(SpotType.COMPACT, parkingAssignment.spotType())
        );
    }

    @Test
    void shouldRejectDuplicateLicencePlate() {
        parkingGarageService.addParkingSpot("C1", SpotType.COMPACT);
        parkingGarageService.addParkingSpot("C2", SpotType.COMPACT);
        parkingGarageService.parkVehicle("ABC-123", SpotType.COMPACT);

        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> parkingGarageService.parkVehicle("ABC-123", SpotType.COMPACT)
                ),
                () -> assertEquals(1, parkingGarageService.getAvailableSpots(SpotType.COMPACT))
        );
    }

    @Test
    void shouldParkCompactVehicleInRegularSpotWhenCompactSpotUnavailable() {
        parkingGarageService.addParkingSpot("R1", SpotType.REGULAR);

        String spotNumber = parkingGarageService.parkVehicle("SMALL-1", SpotType.COMPACT);
        ParkingAssignment parkingAssignment = parkingGarageService.getParkedVehicle("SMALL-1");

        assertAll(
                () -> assertEquals("R1", spotNumber),
                () -> assertEquals(0, parkingGarageService.getAvailableSpots(SpotType.REGULAR)),
                () -> assertEquals(SpotType.COMPACT, parkingAssignment.vehicleSize()),
                () -> assertEquals(SpotType.REGULAR, parkingAssignment.spotType())
        );
    }

    @Test
    void shouldGetParkedVehicleByLicencePlate() {
        parkingGarageService.addParkingSpot("L1", SpotType.LARGE);
        parkingGarageService.parkVehicle("TRUCK-1", SpotType.LARGE);

        ParkingAssignment parkingAssignment = parkingGarageService.getParkedVehicle("TRUCK-1");

        assertAll(
                () -> assertEquals("TRUCK-1", parkingAssignment.licencePlate()),
                () -> assertEquals("L1", parkingAssignment.spotNumber()),
                () -> assertEquals(SpotType.LARGE, parkingAssignment.vehicleSize()),
                () -> assertEquals(SpotType.LARGE, parkingAssignment.spotType())
        );
    }

    @Test
    void shouldReturnNullWhenGettingUnknownParkedVehicle() {
        assertNull(parkingGarageService.getParkedVehicle("UNKNOWN"));
    }

    @Test
    void shouldGetAvailableSpotCountAfterParkingVehicles() {
        parkingGarageService.addParkingSpot("C1", SpotType.COMPACT);
        parkingGarageService.addParkingSpot("R1", SpotType.REGULAR);
        parkingGarageService.addParkingSpot("R2", SpotType.REGULAR);
        parkingGarageService.addParkingSpot("L1", SpotType.LARGE);

        parkingGarageService.parkVehicle("CAR-1", SpotType.COMPACT);
        parkingGarageService.parkVehicle("CAR-2", SpotType.COMPACT);

        assertAll(
                () -> assertEquals(0, parkingGarageService.getAvailableSpots(SpotType.COMPACT)),
                () -> assertEquals(1, parkingGarageService.getAvailableSpots(SpotType.REGULAR)),
                () -> assertEquals(1, parkingGarageService.getAvailableSpots(SpotType.LARGE))
        );
    }

    @Test
    void shouldParkOnlyAvailableSpotsWhenVehiclesParkConcurrently() throws Exception {
        int spotCount = 5;
        int vehicleCount = 20;
        for (int index = 1; index <= spotCount; index++) {
            parkingGarageService.addParkingSpot("C" + index, SpotType.COMPACT);
        }

        List<ParkingAttempt> parkingAttempts = parkConcurrently(
                vehicleCount,
                index -> "CAR-" + index,
                SpotType.COMPACT
        );
        List<String> parkedSpotNumbers = parkingAttempts.stream()
                .filter(ParkingAttempt::parked)
                .map(ParkingAttempt::spotNumber)
                .toList();
        long rejectedVehicles = parkingAttempts.stream()
                .filter(parkingAttempt -> parkingAttempt.error() instanceof IllegalArgumentException)
                .count();
        long unexpectedFailures = parkingAttempts.stream()
                .filter(parkingAttempt -> parkingAttempt.error() != null)
                .filter(parkingAttempt -> !(parkingAttempt.error() instanceof IllegalArgumentException))
                .count();

        assertAll(
                () -> assertEquals(spotCount, parkedSpotNumbers.size()),
                () -> assertEquals(spotCount, new HashSet<>(parkedSpotNumbers).size()),
                () -> assertEquals(vehicleCount - spotCount, rejectedVehicles),
                () -> assertEquals(0, unexpectedFailures),
                () -> assertEquals(0, parkingGarageService.getAvailableSpots(SpotType.COMPACT))
        );
    }

    @Test
    void shouldRejectDuplicateLicencePlateWhenVehiclesParkConcurrently() throws Exception {
        parkingGarageService.addParkingSpot("C1", SpotType.COMPACT);
        parkingGarageService.addParkingSpot("C2", SpotType.COMPACT);

        List<ParkingAttempt> parkingAttempts = parkConcurrently(
                2,
                ignored -> "DUP-1",
                SpotType.COMPACT
        );
        long parkedVehicles = parkingAttempts.stream()
                .filter(ParkingAttempt::parked)
                .count();
        long duplicateRejections = parkingAttempts.stream()
                .filter(parkingAttempt -> parkingAttempt.error() instanceof IllegalArgumentException)
                .count();

        assertAll(
                () -> assertEquals(1, parkedVehicles),
                () -> assertEquals(1, duplicateRejections),
                () -> assertEquals(1, parkingGarageService.getAvailableSpots(SpotType.COMPACT)),
                () -> assertEquals("DUP-1", parkingGarageService.getParkedVehicle("DUP-1").licencePlate())
        );
    }

    private List<ParkingAttempt> parkConcurrently(
            int vehicleCount,
            IntFunction<String> licensePlateFor,
            SpotType vehicleSize
    ) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(vehicleCount);
        CountDownLatch readyGate = new CountDownLatch(vehicleCount);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<ParkingAttempt>> futures = new ArrayList<>();

        for (int index = 0; index < vehicleCount; index++) {
            int vehicleIndex = index;
            futures.add(executorService.submit(() -> {
                readyGate.countDown();
                startGate.await();

                String licensePlate = licensePlateFor.apply(vehicleIndex);
                try {
                    String spotNumber = parkingGarageService.parkVehicle(licensePlate, vehicleSize);
                    return new ParkingAttempt(true, licensePlate, spotNumber, null);
                } catch (RuntimeException exception) {
                    return new ParkingAttempt(false, licensePlate, null, exception);
                }
            }));
        }

        assertTrue(readyGate.await(2, TimeUnit.SECONDS));
        startGate.countDown();

        try {
            List<ParkingAttempt> parkingAttempts = new ArrayList<>();
            for (Future<ParkingAttempt> future : futures) {
                parkingAttempts.add(future.get(2, TimeUnit.SECONDS));
            }
            return parkingAttempts;
        } finally {
            executorService.shutdownNow();
            assertTrue(executorService.awaitTermination(2, TimeUnit.SECONDS));
        }
    }

    private record ParkingAttempt(boolean parked, String licensePlate, String spotNumber, RuntimeException error) {
    }
}
