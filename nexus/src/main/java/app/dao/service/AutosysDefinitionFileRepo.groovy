package app.dao.service

import app.dao.model.autosys.AutosysDefinitionFile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
/**
 * Repository interface for AutosysDefinitionFile entity.
 * This interface extends JpaRepository to provide CRUD operations.
 */
@Repository
interface AutosysDefinitionFileRepo extends JpaRepository<AutosysDefinitionFile, Long> {
}

