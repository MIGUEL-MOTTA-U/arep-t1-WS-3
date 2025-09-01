package escuelaing.edu.co.framework.services.implementations;

import escuelaing.edu.co.framework.annotations.Autowired;
import escuelaing.edu.co.framework.annotations.Component;
import escuelaing.edu.co.framework.services.interfaces.AnotherService;
import escuelaing.edu.co.framework.services.interfaces.Service;

@Component
public class ServiceImpl implements Service, Runnable {
    @Autowired
    private AnotherService anotherService;

    @Override
    public void run() {
        anotherService.kill();
    }

}
