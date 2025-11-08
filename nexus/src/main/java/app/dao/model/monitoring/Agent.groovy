package app.dao.model.monitoring

import com.google.gson.Gson
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

@Entity
class Agent {

    private static final long serialVersionUID = 3327643027629528402L
    private static Gson gson = new Gson()

    @Id
    @GeneratedValue
    Long id

    @Column(unique = true, nullable = false)
    String agentId

    @Column(nullable = false)
    String displayName

    @Column(nullable = false)
    String status

    @Column
    String machine

    @Column
    Long heartbeat = 0L

    @Column
    Integer customProgramCount = 0

    @Column(columnDefinition = "TEXT")
    String description

}
