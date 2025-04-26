package app.dao.service

import app.dao.model.autosys.AutosysDefinitionFile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AutosysDefinitionFileRepo extends JpaRepository<AutosysDefinitionFile, Long> {
}

