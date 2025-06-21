package app.dao.service;

import app.dao.model.component.RatesComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing RatesComponent entities.
 * This interface extends JpaRepository to provide CRUD operations.
 */
@Repository
public interface RatesComponentRepo extends JpaRepository<RatesComponent, Long> {

    /**
     * Custom query to find a RatesComponent by its name.
     *
     * @param componentName the name of the RatesComponent
     * @return a list of RatesComponent entities with the specified name
     */
    @Query("SELECT u FROM RatesComponent u WHERE u.componentName = ?1")
    List<RatesComponent> findByComponentName(String componentName);


}
