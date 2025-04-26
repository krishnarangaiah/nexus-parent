package app.dao.service


import app.dao.model.user.AppUser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserService {

    @Autowired
    private UserRepo repo

    void save(AppUser user) {
        repo.save(user)
    }

    AppUser findById(Long id) {
        return repo.findById(id).orElse(null)
    }

    void delete(AppUser user) {
        repo.delete(user)
    }

    List<AppUser> findAll() {
        return repo.findAll()
    }

    List<AppUser> authenticate(String userName) {
        return repo.findByUserName(userName)
    }

}
