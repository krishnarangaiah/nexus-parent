package app.dao.model.monitoring

import app.dao.service.monitoring.AgentRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AgentService {
    @Autowired
    private AgentRepo repo

    List<Agent> findAll() {
        return repo.findAll()
    }

    Agent findById(Long id) {
        return repo.findById(id).orElse(null)
    }

    Agent findByAgentId(String agentId) {
        return repo.findByAgentId(agentId)
    }

    Agent save(Agent agent) {
        return repo.save(agent)
    }

    void deleteById(Long id) {
        repo.deleteById(id)
    }
}
