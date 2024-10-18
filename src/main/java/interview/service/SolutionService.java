package interview.service;

import java.util.logging.Logger;

public class SolutionService {
    private static final Logger log = Logger.getLogger(SolutionService.class.getName());

    private final InnerService innerService;

    public SolutionService(InnerService innerService) {
        this.innerService = innerService;
    }

    public String getResult() {
        String fromInner = innerService.getInnerValue();
        return fromInner + " test";
    }
}
