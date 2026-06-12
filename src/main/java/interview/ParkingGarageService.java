package interview;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ParkingGarageService {
    private final Map<SpotType, Deque<ParkingSpot>> availableSpots = new HashMap<>();
    private final Map<String, ParkingAssignment> parkedVehicles = new HashMap<>();
    private final Set<String> allSpotNumbers = new HashSet<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    public ParkingGarageService() {
        for (SpotType spotType : SpotType.values()) {
            availableSpots.put(spotType, new ArrayDeque<>());
        }
    }

    public void addParkingSpot(String spotNumber, SpotType spotType) {
        writeLock.lock();
        try {
            if (spotType == null || spotNumber == null || spotNumber.isEmpty()) {
                throw new IllegalArgumentException("Spot type/number cannot be null or empty");
            }
            if (allSpotNumbers.contains(spotNumber)) {
                throw new IllegalArgumentException("Spot number already exists");
            }
            ParkingSpot parkingSpot = new ParkingSpot(spotNumber, spotType);
            allSpotNumbers.add(spotNumber);
            availableSpots.get(spotType).add(parkingSpot);
        } finally {
            writeLock.unlock();
        }
    }

    public String parkVehicle(String licensePlate, SpotType vehicleSize) {
        writeLock.lock();
        try {
            if (licensePlate == null || vehicleSize == null) {
                throw new IllegalArgumentException("licensePlate or vehicleSize cannot be null or empty");
            }
            if (parkedVehicles.containsKey(licensePlate)) {
                throw new IllegalArgumentException("Vehicle already exists");
            }
            Deque<ParkingSpot> parkingSpots = findAvailableSpotsFor(vehicleSize);
            ParkingSpot parkingSpot = parkingSpots.poll();
            ParkingAssignment parkingAssignment = new ParkingAssignment(
                    licensePlate,
                    parkingSpot.spotNumber(),
                    vehicleSize,
                    parkingSpot.spotType()
            );
            parkedVehicles.put(licensePlate, parkingAssignment);
            return parkingAssignment.spotNumber();
        } finally {
            writeLock.unlock();
        }
    }

    public int getAvailableSpots(SpotType spotType) {
        readLock.lock();
        try {
            return availableSpots.get(spotType).size();
        } finally {
            readLock.unlock();
        }
    }

    public ParkingAssignment getParkedVehicle(String licensePlate) {
        readLock.lock();
        try {
            return parkedVehicles.get(licensePlate);
        } finally {
            readLock.unlock();
        }
    }

    private Deque<ParkingSpot> findAvailableSpotsFor(SpotType vehicleSize) {
        SpotType[] spotTypes = SpotType.values();
        for (int index = vehicleSize.ordinal(); index < spotTypes.length; index++) {
            Deque<ParkingSpot> parkingSpots = availableSpots.get(spotTypes[index]);
            if (!parkingSpots.isEmpty()) {
                return parkingSpots;
            }
        }

        throw new IllegalArgumentException("Vehicle has no parking spot");
    }
}
