package app.dao.service;

import app.dao.model.TopologyComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing TopologyComponent entities.
 * This interface extends JpaRepository to provide CRUD operations.
 */
@Repository
public interface TopologyComponentRepo extends JpaRepository<TopologyComponent, Long> {
}
