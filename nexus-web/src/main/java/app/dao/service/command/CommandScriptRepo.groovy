package app.dao.service.command

import app.dao.model.command.CommandScript
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CommandScriptRepo extends JpaRepository<CommandScript, Long> {

    /** Find script by name */
    CommandScript findByName(String name)

    /** Check if script exists */
    boolean existsByName(String name)

    /** Find all active scripts */
    List<CommandScript> findByActiveTrue()

    /** Find all active scripts by category */
    List<CommandScript> findByCategoryAndActiveTrue(String category)

    /** Find all built-in scripts */
    List<CommandScript> findByBuiltInTrue()

    /** Find all categories */
    List<String> findDistinctCategoryByActiveTrue()
}

