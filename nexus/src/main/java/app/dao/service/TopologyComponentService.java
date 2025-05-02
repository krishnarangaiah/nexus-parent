package app.dao.service;

import app.dao.model.TopologyComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service class for managing TopologyComponent entities.
 * This class provides methods to interact with the TopologyComponent repository.
 */
@Service
public class TopologyComponentService {

    @Autowired
    private TopologyComponentRepo repo;

    void save(TopologyComponent e) {
        repo.save(e);
    }

}
