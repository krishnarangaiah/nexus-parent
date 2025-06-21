package app.dao.service


import app.dao.model.component.RatesComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service

/**
 * Service class for managing RatesComponent entities.
 * This class provides methods to interact with the RatesComponent repository.
 */
@Service
public class RatesComponentService {

    @Autowired
    private RatesComponentRepo repo;

    void save(RatesComponent e) {
        repo.save(e);
    }

    void delete(RatesComponent e) {
        repo.delete(e);
    }

    void deleteById(Long id) {
        repo.deleteById(id);
    }

    RatesComponent findById(Long id) {
        return repo.findById(id).orElse(null);
    }

    List<RatesComponent> findByComponentName(String componentName) {
        return repo.findByComponentName(componentName);
    }

    List<RatesComponent> findAll() {
        return repo.findAll();
    }

}
