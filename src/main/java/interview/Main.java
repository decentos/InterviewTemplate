package interview;

public class Main {
    public static void main(String[] args) {
        ParkingGarageService parkingGarageService = new ParkingGarageService();
        parkingGarageService.addParkingSpot("C1", SpotType.COMPACT);
        String spotNumber = parkingGarageService.parkVehicle("ABC-123", SpotType.COMPACT);

        System.out.println("Parked at: " + spotNumber);
    }
}
