package interview;

import interview.config.PropertiesLoader;
import interview.service.InnerService;
import interview.service.SolutionService;

import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        Properties config = PropertiesLoader.loadProperties("application.properties");
        String test = config.getProperty("test");
        System.out.println(test);

        InnerService innerService = new InnerService();
        SolutionService solutionService = new SolutionService(innerService);
        System.out.println(solutionService.getResult());
    }
}
