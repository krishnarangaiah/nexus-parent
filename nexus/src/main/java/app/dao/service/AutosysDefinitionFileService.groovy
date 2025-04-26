package app.dao.service

import app.dao.model.user.AppUser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AutosysDefinitionFileService {

    @Autowired
    private AutosysDefinitionFileRepo repo

    void save(AppUser user) {
        repo.save(user)
    }

}
