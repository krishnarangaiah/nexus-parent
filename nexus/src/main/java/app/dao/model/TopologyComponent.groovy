package app.dao.model

import com.google.gson.Gson
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

@Entity
class TopologyComponent {

    private static final long serialVersionUID = 3327643027629528402L
    private static Gson gson = new Gson()

    @Id
    @GeneratedValue
    Long id;
    @Column(unique = true, nullable = false)
    String name
    @Column(nullable = false)
    String description


}
